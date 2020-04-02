/**
 * Created by Yonatan on 07/11/2017.
 */
//dev dependencies
const path = require('path');
const Promise = require('promise');
const yargs = require('yargs').argv;

//configuration files and directories
const appRoot = path.join(__dirname, "\\..");
const resources = appRoot + "\\src\\main\\resources";
const devEnvDestination = appRoot + "\\dev_env";
const ansibleDestination = "/ansible";
const remoteDestination = "/sixsense";
const remoteConfig = require(devEnvDestination + '\\remote.config.js');

const remoteUtils = require(appRoot + '\\scripts\\remote-utils.js');
const uploadTasks = {
    a: wrapWithServiceStopStart(uploadAll), all: wrapWithServiceStopStart(uploadAll),
    j: wrapWithServiceStopStart(uploadJar), jar: wrapWithServiceStopStart(uploadJar),
    d: wrapWithServiceStopStart(uploadDependencies), dependencies: wrapWithServiceStopStart(uploadDependencies),
    t: uploadTests, tests: uploadTests,
    c: uploadAnsible, control: uploadAnsible
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

//Uploads all required files to the remote SixSense server
function uploadAll(){
    return uploadJar().then(uploadTests).then(uploadDependencies);
}

//Uploads the main JAR file to the remote SixSense server
function uploadJar(){
    let deployCommands = [
        'echo "finished running uploadJar"' //notify the developer's machine CLI that all commands have run successfully.
    ];
    return remoteUtils.sftpTransferFile(appRoot+"\\target\\OperationEngine.jar", remoteDestination + "/OperationEngine.jar", remoteConfig.engine.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
    });
}

//Uploads the tests JAR file to the remote SixSense server
function uploadTests(){
    let deployCommands = [
        'echo "finished running uploadTests"' //notify the developer's machine CLI that all commands have run successfully.
    ];
    return remoteUtils.sftpTransferFile(appRoot+"\\target\\OperationEngine-tests.jar", remoteDestination + "/OperationEngine-tests.jar", remoteConfig.engine.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
    });
}

//Uploads maven dependencies to the remote SixSense server
function uploadDependencies(){
    let deployCommands = [
        'echo "finished running uploadDependencies"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return Promise.all([
        //uploadUtils.sftpTransferFile(appRoot+"\src\main\resources\log4j2.xml", remoteDestination + "/config/log4j2.xml", remoteConfig.engine.remotes), //this should be uncommented once we figure out how to point to external config file (currently loads log4j2 from classpath)
        remoteUtils.sftpTransferDir(appRoot+"\\target\\dependency-jars", remoteDestination + "/dependency-jars", remoteConfig.engine.remotes)
    ]).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
    });
}

//Uploads ansible_control directory to the ansible control server
function uploadAnsible(){
    let deployCommands = [
        'echo "finished running uploadAnsible"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return Promise.all([
        remoteUtils.sftpTransferDir(devEnvDestination + "\\ansible_control", ansibleDestination, remoteConfig.ansible.remotes)
    ]).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
    });
}

function wrapWithServiceStopStart(steps){
    return () => stopService().then(steps).then(startService);
}

//Stops the engine service on the remote SixSense server
function stopService(){
    let deployCommands = [
        'service engine stop',
        'echo "engine service has stopped"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
}

//Starts the engine service on the remote SixSense server
function startService(){
    let deployCommands = [
        'service engine start',
        'echo "engine service has started"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return remoteUtils.executeSsh(deployCommands, remoteConfig.engine.remotes);
}
