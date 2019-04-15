package com.SixSense.util;

import com.SixSense.data.commands.*;
import com.SixSense.data.pipes.AbstractOutputPipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            for (String dynamicField : dynamicFields.keySet()) {
                commandText = commandText.replaceAll(MessageLiterals.VariableMark + dynamicField, dynamicFields.get(dynamicField));
            }
        }

        return commandText;
    }

    public static List<String> pipeCommandOutput(Command command, List<String> output){
        List<String> outputCopy = new ArrayList<>(output);
        for(AbstractOutputPipe outputPipe : command.getOutputPipes()){
            outputCopy = outputPipe.pipe(outputCopy);
        }
        return outputCopy;
    }
}
