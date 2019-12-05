const Promise = require('promise');
const SSH2Utils = require('ssh2-utils');
const sshUtils = new SSH2Utils();

//transfers a directory from the local dev environment to the remote Backbox server, as defined in the sftp.config.js file
function sftpTransferDir(source, destination, remotes){
    console.log("starting sftp transfer from " + source);
    let executions = [];

    for(let remote of remotes){
        executions.push(new Promise((resolve, reject) => sshUtils.putDir(remote, source, destination, (error, server, connection) => {
            sftpCallback(error, server, connection, source, destination, resolve, reject);
        })));
    }

    return Promise.all(executions);
}

//transfers a single file from the local dev enviroment to the remote Backbox server, as defined in the sftp.config.js file
function sftpTransferFile(source, destination, remotes){
    console.log("starting sftp transfer from " + source);
    let executions = [];

    for(let remote of remotes){
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

function executeSsh(commandArr, remotes){
    let joinedCmds = commandArr.join(' && ');
    let executions = [];

    for(let remote of remotes){
        executions.push(new Promise((resolve, reject) => {
            sshUtils.run(remote, joinedCmds, (error, stdout, stderr, server, connection) => {
                stdout.on('data', function (data) {
                    console.log('[' + server.host + ' | stdout]# ', bufferStringify(data));
                });
                stderr.on('data', function (err) {
                    console.log('[' + server.host + ' | stderr]# ', bufferStringify(err));
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

module.exports = {
    sftpTransferFile: sftpTransferFile,
    sftpTransferDir: sftpTransferDir,
    executeSsh: executeSsh,
};