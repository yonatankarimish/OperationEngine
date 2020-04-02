/**
 * Created by Yonatan on 11/06/2019.
 */
//dev dependencies
const readline = require('readline');
const path = require('path');
const Promise = require('promise');
const ncp = require('ncp').ncp;
const readYaml = require('read-yaml');
const writeYaml = require('write-yaml');
const fs = require('graceful-fs');

//common directories
const appRoot = path.join(__dirname, "\\..");
const resources = appRoot + "\\src\\main\\resources";
const configTemplateFolder = resources + "\\dev_env";
const remoteConfigFolder = appRoot + "\\dev_env";
const ansibleTemplateFolder = resources + "\\ansible_control";
const remoteAnsibleFolder = remoteConfigFolder + "\\ansible_control";

//configuration files
const remoteConfigPath = remoteConfigFolder + "\\remote.config.js";

const remoteUtils = require(appRoot + '\\scripts\\remote-utils.js');

init();
function init(){
    initDevEnv() // generate the dev_env directory
        .then(() => configureRemoteVM("engine")) //developer enters credentials for his engine vm
        .then((engineVmConfig) => addToHostsFile("local", engineVmConfig)) //configure sixsense-hosts.yaml with engine vm config
        .then(() => configureRemoteVM("ansible")) //developer enters credentials for his ansible control vm
        .then(() => configureRemoteVM("rabbit", ["vhost"])) //developer enters credentials for his rabbitmq broker vm
        .then((rabbitVmConfig) => addToHostsFile("rabbit", rabbitVmConfig)) //configure sixsense-hosts.yaml with rabbitmq broker vm config
        .then(uploadConfigToAnsibleHost)
        .catch(error => {
            console.error(error);
            process.exit(1);
        })
}

function initDevEnv(){
    ncp.limit = 4;

    return new Promise(
        (resolve, reject) => {
            ncp(configTemplateFolder, remoteConfigFolder, error => {
                if (error) {
                    reject(error);
                } else {
                    resolve();
                }
            });
        }
    ).then(() => new Promise(
        (resolve, reject) => {
            ncp(ansibleTemplateFolder, remoteAnsibleFolder, error => {
                if (error) {
                    reject(error);
                } else {
                    resolve();
                }
            })
        }
    ));
}


async function configureRemoteVM(remoteVMKey, additionalKeys){
    let prettyKey = remoteVMKey.replace("_", " ");
    const remoteConfig = require(remoteConfigPath);
    const prompt = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    let vmHost = await askQuestion(prompt, "Please enter the ip address of your " + prettyKey + " vm : ");
    let vmUsername = await askQuestion(prompt, "Please enter a username for the "+ prettyKey + " vm : ");
    let vmPassword = await askQuestion(prompt, "Please enter the password for the username you provided: ");

    let additionalFields = {};
    if(additionalKeys) {
        for (let key of additionalKeys) {
            additionalFields[key] = await askQuestion(prompt, "Please enter a " + key + " for the " + prettyKey + " vm : ");
        }
    }

    prompt.close();

    let vmConfig = remoteConfig[remoteVMKey].remotes[0];

    vmConfig.host = vmHost;
    vmConfig.username = vmUsername;
    vmConfig.password = vmPassword;

    for(let key of Object.keys(additionalFields)){
        vmConfig[key] = additionalFields[key];
    }

    let updatedContents = "module.exports = " + JSON.stringify(remoteConfig, null, 4)  + ";"; //indent with four space characters per indent
    return new Promise((resolve, reject) => {
        fs.writeFile(remoteConfigPath, updatedContents, error => {
            if(error){
                reject(error);
            }else{
                resolve(vmConfig);
            }
        });
    });
}

function askQuestion(prompt, questionText){
    return new Promise((resolve, reject) => {
        prompt.question(questionText, resolve);
    });
}

function addToHostsFile(key, vmConfig){
    let sixsenseHosts = remoteAnsibleFolder + "\\dir_skeleton\\config\\sixsense-hosts.yaml";
    let hostsData = readYaml.sync(sixsenseHosts);

    hostsData.sixsense.hosts[key] = vmConfig;
    writeYaml.sync(sixsenseHosts, hostsData);
}

function uploadConfigToAnsibleHost(){
    const remoteConfig = require(remoteConfigPath);
    let deployCommands = [
        'echo "Remote config uploaded to Ansible host"',
    ];

    return remoteUtils.sftpTransferFile(remoteConfigPath, "/tmp/remote.config.js", remoteConfig.ansible.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.ansible.remotes);
    });
}