package com.SixSense.util;

import com.SixSense.data.commands.*;
import com.SixSense.data.pipes.AbstractOutputPipe;
import com.SixSense.io.Session;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandUtils {
    //Adds a new Command or Block to the current block's child blocks
    public static Block addCommand(Block parent, Command childCommand){
        parent.addChildBlock(childCommand);
        return parent;
    }

    public static Block addBlock(Block parent, Block childCommand){
        parent.addChildBlock(childCommand);
        return parent;
    }

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
                return addCommand(originalAsBlock, additionalAsCommand);
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

    public static String evaluateAgainstDynamicFields(Command command){
        return evaluateAgainstDynamicFields(command, null);
    }

    public static String evaluateAgainstDynamicFields(Command command, Map<String, String> additionalFields){
        Map<String, String> dynamicFields = command.getDynamicFields();
        String commandText = command.getCommandText();
        commandText = evaluateAgainstDynamicFields(commandText, command.getDynamicFields());
        commandText = evaluateAgainstDynamicFields(commandText, additionalFields);
        return commandText;
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
}
