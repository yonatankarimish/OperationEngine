package com.sixsense.utillity;

import com.sixsense.model.commands.*;
import com.sixsense.model.devices.Device;
import com.sixsense.model.devices.RawExecutionConfig;
import com.sixsense.model.pipes.AbstractOutputPipe;
import com.sixsense.io.Session;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandUtils {
    //Merge an additional Block of commands to the current block of commands;
    // the new blocks child commands will be added to the current block, ignoring any settings on the chained block.
    public static ICommand chainCommands(ICommand original, ICommand additional){
        if(original instanceof ParallelWorkflow || additional instanceof ParallelWorkflow){
            throw new UnsupportedOperationException("Workflows can be merged, but not chained");
        }

        if(original instanceof Operation){
            Operation originalAsOperation = (Operation)original;
            ICommand originalExecutionBlock = originalAsOperation.getExecutionBlock();
            if(additional instanceof Operation){
                Operation additionalAsOperation = (Operation)additional;
                ICommand additionalExecutionBlock = additionalAsOperation.getExecutionBlock();
                return originalAsOperation.withExecutionBlock(chainCommands(originalExecutionBlock, additionalExecutionBlock));
            }else{
                return originalAsOperation.withExecutionBlock(chainCommands(originalExecutionBlock, additional));
            }
        }else if(original instanceof Block){
            Block originalAsBlock = (Block) original;
            if(additional instanceof Operation){
                Operation additionalAsOperation = (Operation)additional;
                ICommand additionalExecutionBlock = additionalAsOperation.getExecutionBlock();
                return additionalAsOperation.withExecutionBlock(chainCommands(original, additionalExecutionBlock));
            }else if(additional instanceof Block){
                return originalAsBlock.addChildBlocks(((Block) additional).getChildBlocks());
            }else{
                Command additionalAsCommand = (Command)additional;
                return originalAsBlock.addChildBlock(additionalAsCommand);
            }
        }else{
            if(additional instanceof Operation){
                Operation additionalAsOperation = (Operation)additional;
                ICommand additionalExecutionBlock = additionalAsOperation.getExecutionBlock();
                return additionalAsOperation.withExecutionBlock(chainCommands(original, additionalExecutionBlock));
            }if(additional instanceof Block){
                Block additionalAsBlock = (Block) additional;
                additionalAsBlock.prependChildBlock(original);
                return additionalAsBlock;
            }else{
                return new Block()
                        .addChildBlock(original)
                        .addChildBlock(additional);
            }
        }
    }

    public static ParallelWorkflow composeWorkflow(RawExecutionConfig rawConfig){
        ParallelWorkflow parallelNode = new ParallelWorkflow();
        for(Device device : rawConfig.getDevices()){
            parallelNode.addParallelOperation(
                (Operation)rawConfig.getOperation().deepClone()
                    .addDynamicFields(device.getDynamicFields())
                    .addDynamicField(DynamicFieldGlossary.device_internal_id, device.getShortUUID())
                    .addDynamicField(DynamicFieldGlossary.device_host, device.getCredentials().getHost())
                    .addDynamicField(DynamicFieldGlossary.device_username, device.getCredentials().getUsername())
                    .addDynamicField(DynamicFieldGlossary.device_password, device.getCredentials().getPassword())
                    .addDynamicField(DynamicFieldGlossary.device_port, String.valueOf(device.getCredentials().getPort()))
            );
        }

        return parallelNode;
    }

    public static AbstractWorkflow mergeWorkflows(AbstractWorkflow original, AbstractWorkflow additional){
        if(original instanceof ParallelWorkflow){
            ParallelWorkflow originalAsWorkflow = (ParallelWorkflow)original;
            if(additional instanceof ParallelWorkflow){
                return originalAsWorkflow.addParallelOperations(((ParallelWorkflow)additional).getParallelOperations());
            }else{
                return originalAsWorkflow.addParallelOperation((Operation)additional);
            }
        }else{
            if(additional instanceof ParallelWorkflow){
                throw new UnsupportedOperationException("Parallel workflows cannot be merged into an operation");
            }else{
                Operation originalAsOperation = (Operation)original;
                Operation additionalAsOperation = (Operation)additional;
                return new ParallelWorkflow()
                        .addParallelOperation(originalAsOperation)
                        .addParallelOperation(additionalAsOperation);
            }
        }
    }

    public static String evaluateAgainstDynamicFields(String commandText){
        return evaluateAgainstDynamicFields(commandText, null);
    }

    public static String evaluateAgainstDynamicFields(String commandText, Map<String, String> dynamicFields){
        if(commandText == null || commandText.isEmpty()){
            return commandText;
        }
        if(dynamicFields != null) {
            /*Consider the case of evaluating two dynamic fields: var.scope.field = 'foo' and var.scope.field_with_long_name = 'bar':
            * If the command contains, say, 'echo $var.scope.field_with_long_name', and we evaluate var.scope.field first
            * Then the command will return as 'foo_with_long_name'
            * Therefore we order the dynamic fields by inverse lexicographical order to avoid this scenario*/
            List<String> orderedKeys = dynamicFields.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (String dynamicField : orderedKeys) {
                commandText = commandText.replace(MessageLiterals.VariableMark + dynamicField, dynamicFields.get(dynamicField));
            }
        }

        return commandText;
    }

    public static List<String> pipeCommandOutput(Session session, List<String> output){
        for(AbstractOutputPipe outputPipe : session.getCurrentCommand().getOutputPipes()){
            output = outputPipe.pipe(session, output);
        }
        return output;
    }

    public static String pipeCommandRetention(Session session, String retentionValue){
        for(AbstractOutputPipe retentionPipe : session.getCurrentCommand().getRetentionPipes()){
            retentionValue = retentionPipe.pipe(session, retentionValue);
        }
        return retentionValue;
    }
}
