/**
 * Created by Yonatan on 11/06/2019.
 */
const readline = require('readline');
const path = require('path');
const Promise = require('promise');
const ncp = require('ncp').ncp;
const fs = require('graceful-fs');

const appRoot = path.join(__dirname, "\\..");
const resources = appRoot + "\\src\\main\\resources";
const devEnvFolder = resources + "\\dev_env";
const devEnvDestination = appRoot + "\\dev_env";

const remoteConfigPath = appRoot + "\\dev_env\\remote.config.js";
const remoteConfig = require(remoteConfigPath);

const remoteUtils = require(appRoot + '\\scripts\\remote-utils.js');

init();
function init(){
    initDevEnv()
        .then(() => configureRemoteVM("dev"))
        .then(() => configureRemoteVM("ansible"))
        .then(uploadConfigToAnsibleHost)
        .catch(error => {
            console.error(error);
            process.exit(1);
        })
}

function initDevEnv(){
    return new Promise((resolve, reject) => {
        ncp.limit = 4;

        ncp(devEnvFolder, devEnvDestination, error => {
            if (error) {
                reject(error);
            } else {
                resolve();
            }

        });
    });
}

async function configureRemoteVM(remoteVMKey){
    let prettyKey = remoteVMKey.replace("_", " ");
    const prompt = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    let vmHost = await askQuestion(prompt, "Please enter the ip address of your " + prettyKey + " vm : ");
    let vmUsername = await askQuestion(prompt, "Please enter a username for the "+ prettyKey + " vm : ");
    let vmPassword = await askQuestion(prompt, "Please enter the password for the username you provided: ");
    prompt.close();

    let vmConfig = remoteConfig[remoteVMKey].remotes[0];

    vmConfig.host = vmHost;
    vmConfig.username = vmUsername;
    vmConfig.password = vmPassword;

    let updatedContents = "module.exports = " + JSON.stringify(remoteConfig, null, 4)  + ";"; //indent with four space characters per indent
    return new Promise((resolve, reject) => {
        fs.writeFile(remoteConfigPath, updatedContents, error => {
            if(error){
                reject(error);
            }else{
                resolve();
            }
        });
    });
}

function askQuestion(prompt, questionText){
    return new Promise((resolve, reject) => {
        prompt.question(questionText, resolve);
    });
}

function uploadConfigToAnsibleHost(){
    let deployCommands = [
        'echo "Remote config uploaded to Ansible host"',
    ];

    return remoteUtils.sftpTransferFile(remoteConfigPath, "/tmp/remote.config.js", remoteConfig.ansible.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.ansible.remotes);
    });
}