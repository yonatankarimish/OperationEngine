/**
 * Created by Yonatan on 07/11/2017.
 */
const os = require('os');
const fs = require('fs');
const path = require('path');
const Promise = require('promise');
const SSH2Utils = require('ssh2-utils');
const yargs = require('yargs').argv;
const sftpConfig = require('c:/backbox/config/sftp.config')();

const appRoot = path.join(__dirname, "/..");

const sshUtils = new SSH2Utils();
const uploadTasks = {
    j: uploadJar, jar: uploadJar,
    d: uploadJar, dependencies: uploadDependencies
};

//Executes the proper upload task according to the value passed to the --task flag.
//Usage:
init();
function init(){
    let execPromise = yargs.task ? uploadTasks[yargs.task]() : uploadTasks['client']();
    execPromise.then(() => {
        process.exit(0);
    }).catch((e) => {
        console.error("SSH upload: failed to execute script with task argument = "+yargs.task);
        console.error("Caused by: ", e);
        process.exit(1);
    });
}

//Uploads the WAR file to the remote Backbox server
function uploadJar(){
    let deployCommands = [
        'echo "finished running uploadJar"', //notify the developer's machine CLI that all commands have run successfully.
    ];
    return sftpTransferFile(appRoot+"/target/OperatingSystem.jar", "/tmp/SixSense/OperatingSystem.jar").then(() => {
        return executeSsh(deployCommands);
    });
}

function uploadDependencies(){
    let deployCommands = [
        'echo "finished running uploadDependencies"', //notify the developer's machine CLI that all commands have run successfully.
    ];

    return Promise.all([
        //sftpTransferFile(appRoot+"/src/main/resources/log4j2.xml", "/tmp/SixSense/config/log4j2.xml"), //this should be uncommented once we figure out how to point to external config file (currently loads log4j2 from classpath)
        sftpTransferDir(appRoot+"/target/dependency-jars", "/tmp/SixSense/dependency-jars")
    ]).then(() => {
        return executeSsh(deployCommands);
    });
}

//transfers a directory from the local dev environment to the remote Backbox server, as defined in the sftp.config.js file
function sftpTransferDir(source, destination){
    console.log("starting sftp transfer from " + source);
    let executions = [];

    for(let remote of sftpConfig.remotes){
        executions.push(new Promise((resolve, reject) => sshUtils.putDir(remote, source, destination, (error, server, connection) => {
            sftpCallback(error, server, connection, source, destination, resolve, reject);
        })));
    }

    return Promise.all(executions);
}

//transfers a single file from the local dev enviroment to the remote Backbox server, as defined in the sftp.config.js file
function sftpTransferFile(source, destination){
    console.log("starting sftp transfer from " + source);
    let executions = [];

    for(let remote of sftpConfig.remotes){
        executions.push(new Promise((resolve, reject) => sshUtils.putFile(remote, source, destination, (error, server, connection) => {
            sftpCallback(error, server, connection, source, destination, resolve, reject);
        })));
    }

    return Promise.all(executions);
}

function sftpCallback(error, server, connection, source, destination, resolve, reject){
    connection.on('error', (err) => {
        reject(err);
    });
    connection.on('close', (hadError) => {
        if(hadError){
            console.log("failed to execute sftp transfer to "+ server.host + ": " + destination);
            reject("connection closed with error");
        }else{
            console.log("finished sftp transfer to " + server.host + ": " + destination);
            resolve("connection closed");
        }
    });

    if (error) {
        console.log(error);
        reject(error);
    }
    connection.end();
}

function executeSsh(commandArr){
    let joinedCmds = commandArr.join(' && ');
    let executions = [];

    for(let remote of sftpConfig.remotes){
        executions.push(new Promise((resolve, reject) => {
            sshUtils.run(remote, joinedCmds, (error, stdout, stderr, server, connection) => {
                stdout.on('data', function (data) {
                    console.log('stdout: ', bufferStringify(data));
                });
                stderr.on('data', function (err) {
                    console.log('stderr: ', bufferStringify(err));
                });
                stdout.on('close', function () {
                    resolve("connection closed");
                    connection.end();
                });
            });
        }));
    }

    return Promise.all(executions);
}

function bufferStringify(buffer) {
    return buffer.toString().replace(/[\r\n]+/g," ").trim()
}