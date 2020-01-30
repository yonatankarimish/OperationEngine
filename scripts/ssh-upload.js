/**
 * Created by Yonatan on 07/11/2017.
 */
const path = require('path');
const Promise = require('promise');
const yargs = require('yargs').argv;

const appRoot = path.join(__dirname, "\\..");
const devEnvDestination = appRoot + "\\dev_env";
const remoteDestination = "/sixsense";
const remoteConfig = require(devEnvDestination + '\\remote.config.js');

const remoteUtils = require(appRoot + '\\scripts\\remote-utils.js');
const uploadTasks = {
    a: wrapWithServiceStopStart(uploadAll), all: wrapWithServiceStopStart(uploadAll),
    j: wrapWithServiceStopStart(uploadJar), jar: wrapWithServiceStopStart(uploadJar),
    t: wrapWithServiceStopStart(uploadTests), tests: wrapWithServiceStopStart(uploadTests),
    d: wrapWithServiceStopStart(uploadDependencies), dependencies: wrapWithServiceStopStart(uploadDependencies)
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
    return remoteUtils.sftpTransferFile(appRoot+"\\target\\OperationEngine.jar", remoteDestination + "/OperationEngine.jar", remoteConfig.dev.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.dev.remotes);
    });
}

//Uploads the tests JAR file to the remote SixSense server
function uploadTests(){
    let deployCommands = [
        'echo "finished running uploadTests"' //notify the developer's machine CLI that all commands have run successfully.
    ];
    return remoteUtils.sftpTransferFile(appRoot+"\\target\\OperationEngine-tests.jar", remoteDestination + "/OperationEngine-tests.jar", remoteConfig.dev.remotes).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.dev.remotes);
    });
}

//Uploads maven dependencies to the remote SixSense server
function uploadDependencies(){
    let deployCommands = [
        'echo "finished running uploadDependencies"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return Promise.all([
        //uploadUtils.sftpTransferFile(appRoot+"\src\main\resources\log4j2.xml", remoteDestination + "/config/log4j2.xml", remoteConfig.dev.remotes), //this should be uncommented once we figure out how to point to external config file (currently loads log4j2 from classpath)
        remoteUtils.sftpTransferDir(appRoot+"\\target\\dependency-jars", remoteDestination + "/dependency-jars", remoteConfig.dev.remotes)
    ]).then(() => {
        return remoteUtils.executeSsh(deployCommands, remoteConfig.dev.remotes);
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

    return remoteUtils.executeSsh(deployCommands, remoteConfig.dev.remotes);
}

//Starts the engine service on the remote SixSense server
function startService(){
    let deployCommands = [
        'service engine start',
        'echo "engine service has started"' //notify the developer's machine CLI that all commands have run successfully.
    ];

    return remoteUtils.executeSsh(deployCommands, remoteConfig.dev.remotes);
}
