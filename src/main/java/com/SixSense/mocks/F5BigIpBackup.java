package com.SixSense.mocks;

import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;
import com.SixSense.data.commands.Operation;
import com.SixSense.data.outcomes.*;
import com.SixSense.data.retention.ResultRetention;
import com.SixSense.data.retention.VariableRetention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class F5BigIpBackup {
    public static Operation f5BigIpBackup(String host, String username, String password){
        ICommand operationBlock = sshConnect()
                .chainCommands(rebuildSixSenseDirectory())
                .chainCommands(exitCommand());

        Map<String, String> dynamicFields = new HashMap<>();
        dynamicFields.put("\\$device.host", host);
        dynamicFields.put("\\$device.username", username);
        dynamicFields.put("\\$device.password", password);
        dynamicFields.put("\\$device.port", "22");
        operationBlock.addDynamicFields(dynamicFields);

        return new Operation()
                .withVPV("F5 BigIP Version 11 and above")
                .withOperationName("Configuration Backup")
                .withExecutionBlock(operationBlock);
    }

    private static ICommand sshConnect(){
        Command ssh = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("ssh $device.username@$device.host -p $device.port")
                .withSecondsToTimeout(60);

        ExpectedOutcome template = new ExpectedOutcome()
                .withBinaryRelation(BinaryRelation.CONTAINS)
                .withOutcome(ResultStatus.SUCCESS);

        ExpectedOutcome password = new ExpectedOutcome(template).withExpectedValue("assword:");
        ExpectedOutcome hashbang = new ExpectedOutcome(template).withExpectedValue("#");
        ExpectedOutcome bracket = new ExpectedOutcome(template).withExpectedValue(">");
        ExpectedOutcome connectYesNo = new ExpectedOutcome(template).withExpectedValue("connecting (yes/no)");
        ExpectedOutcome connecting = new ExpectedOutcome(template).withExpectedValue("connecting");

        ExpectedOutcome identificationChange = new ExpectedOutcome(template)
                .withExpectedValue("REMOTE HOST IDENTIFICATION HAS CHANGE")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("SSH Key Mismatch");

        ExpectedOutcome timeout = new ExpectedOutcome(template)
                .withExpectedValue("Connection timed out")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("Connection timed out");

        ExpectedOutcome refusal = new ExpectedOutcome(template)
                .withExpectedValue("Connection refused")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("Connection refused");

        ExpectedOutcome noRouteToHost = new ExpectedOutcome(template)
                .withExpectedValue("No route to host")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("No route to host");

        List<ExpectedOutcome> sshExpectedOutcomes = new ArrayList<>();
        sshExpectedOutcomes.add(password);
        sshExpectedOutcomes.add(hashbang);
        sshExpectedOutcomes.add(bracket);
        sshExpectedOutcomes.add(connectYesNo);
        sshExpectedOutcomes.add(connecting);
        sshExpectedOutcomes.add(identificationChange);
        sshExpectedOutcomes.add(timeout);
        sshExpectedOutcomes.add(refusal);
        sshExpectedOutcomes.add(noRouteToHost);
        ssh.setExpectedOutcomes(sshExpectedOutcomes);

        ssh.setSaveTo(new VariableRetention()
            .withResultRetention(ResultRetention.Variable)
            .withName("\\$ssh.connect.response")
        );

        Command rsaFirstTime = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("yes")
                .withSecondsToTimeout(60);

        ExecutionCondition rsaFirstTimeCondition = new ExecutionCondition()
                .withVariable("\\$ssh.connect.response")
                .withBinaryRelation(BinaryRelation.CONTAINS)
                .withExpectedValue("connecting (yes/no)");

        List<ExecutionCondition> rsaFirstTimeConditions = new ArrayList<>();
        rsaFirstTimeConditions.add(rsaFirstTimeCondition);
        rsaFirstTime.setExecutionConditions(rsaFirstTimeConditions);

        ExpectedOutcome denied = new ExpectedOutcome(template).withExpectedValue("denied");

        ExpectedOutcome nameOrServiceUnknown = new ExpectedOutcome(template)
                .withExpectedValue("Name or service not known")
                .withOutcome(ResultStatus.FAILURE);

        ExpectedOutcome invalidArgument = new ExpectedOutcome(template)
                .withExpectedValue("Invalid argument")
                .withOutcome(ResultStatus.FAILURE);

        List<ExpectedOutcome> rsaFirstTimeExpectedOutcomes = new ArrayList<>();
        rsaFirstTimeExpectedOutcomes.add(password);
        rsaFirstTimeExpectedOutcomes.add(hashbang);
        rsaFirstTimeExpectedOutcomes.add(bracket);
        rsaFirstTimeExpectedOutcomes.add(denied);
        rsaFirstTimeExpectedOutcomes.add(nameOrServiceUnknown);
        rsaFirstTimeExpectedOutcomes.add(identificationChange);
        rsaFirstTimeExpectedOutcomes.add(timeout);
        rsaFirstTimeExpectedOutcomes.add(refusal);
        rsaFirstTimeExpectedOutcomes.add(noRouteToHost);
        rsaFirstTimeExpectedOutcomes.add(invalidArgument);
        rsaFirstTime.setExpectedOutcomes(rsaFirstTimeExpectedOutcomes);

        rsaFirstTime.setSaveTo(new VariableRetention()
                .withResultRetention(ResultRetention.Variable)
                .withName("\\$ssh.connect.response")
        );

        Command typePassword = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("$device.password")
                .withSecondsToTimeout(60);

        ExecutionCondition typePasswordCondition = new ExecutionCondition()
                .withVariable("\\$device.password")
                .withBinaryRelation(BinaryRelation.NOT_EQUALS)
                .withExpectedValue("");

        List<ExecutionCondition> typePasswordConditions = new ArrayList<>();
        typePasswordConditions.add(typePasswordCondition);
        typePassword.setExecutionConditions(typePasswordConditions);

        ExpectedOutcome emptySuccess = new ExpectedOutcome()
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.EQUALS)
                .withOutcome(ResultStatus.SUCCESS);

        ExpectedOutcome connectionRefused = new ExpectedOutcome(template)
                .withExpectedValue("Connection")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("Connection refused");

        ExpectedOutcome connectionDenied = new ExpectedOutcome(template)
                .withExpectedValue("enied")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("Wrong username or password");

        ExpectedOutcome wrongCredentials = new ExpectedOutcome(template)
                .withExpectedValue("word:")
                .withOutcome(ResultStatus.FAILURE)
                .withMessage("Wrong credentials");

        List<ExpectedOutcome> typePasswordExpectedOutcomes = new ArrayList<>();
        typePasswordExpectedOutcomes.add(emptySuccess);
        typePasswordExpectedOutcomes.add(connectionRefused);
        typePasswordExpectedOutcomes.add(connectionDenied);
        typePasswordExpectedOutcomes.add(wrongCredentials);
        typePassword.setExpectedOutcomes(typePasswordExpectedOutcomes);

        Command tmsh = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("tmsh modify cli preference pager disabled")
                .withSecondsToTimeout(30);

        List<ExpectedOutcome> tmshExpectedOutcomes = new ArrayList<>();
        tmshExpectedOutcomes.add(emptySuccess);
        tmsh.setExpectedOutcomes(tmshExpectedOutcomes);

        return ssh.chainCommands(rsaFirstTime)
                .chainCommands(typePassword)
                .chainCommands(tmsh);
    }

    private static ICommand rebuildSixSenseDirectory(){
        Command deleteOldDir = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("rm -rf /var/SixSense")
                .withSecondsToTimeout(600);

        ExpectedOutcome emptySuccess = new ExpectedOutcome()
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.EQUALS)
                .withOutcome(ResultStatus.SUCCESS);

        List<ExpectedOutcome> deleteOldDirExpectedOutcomes = new ArrayList<>();
        deleteOldDirExpectedOutcomes.add(emptySuccess);
        deleteOldDir.setExpectedOutcomes(deleteOldDirExpectedOutcomes);

        Command makeNewDir = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("mkdir -p /var/SixSense")
                .withSecondsToTimeout(90);

        List<ExpectedOutcome> makeNewDirExpectedOutcomes = new ArrayList<>();
        makeNewDirExpectedOutcomes.add(emptySuccess);
        makeNewDir.setExpectedOutcomes(makeNewDirExpectedOutcomes);

        return deleteOldDir.chainCommands(makeNewDir);
    }

    private static ICommand exitCommand(){
        Command exit = new Command()
                .withCommandType(CommandType.REMOTE)
                .withCommandText("exit")
                .withSecondsToTimeout(60);

        ExpectedOutcome emptySuccess = new ExpectedOutcome()
                .withExpectedValue("")
                .withBinaryRelation(BinaryRelation.EQUALS)
                .withOutcome(ResultStatus.SUCCESS);

        List<ExpectedOutcome> exitExpectedOutcomes = new ArrayList<>();
        exitExpectedOutcomes.add(emptySuccess);
        exit.setExpectedOutcomes(exitExpectedOutcomes);

        return exit;
    }
}
