package com.SixSense.util;

import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.Command;
import com.SixSense.data.commands.ICommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandUtils {
    //Adds a new Command or Block to the current block's child blocks
    public static Block addCommand(Block parent, ICommand childCommand){
        parent.getChildBlocks().add(childCommand);
        return parent;
    }

    //Merge an additional Block of commands to the current block of commands;
    // the new blocks child commands will be added to the current block, ignoring any settings on the chained block.
    public static Block chainCommands(ICommand original, ICommand additional){
        if(original instanceof Block){
            Block originalAsBlock = ((Block) original);
            if(additional instanceof Block){
                originalAsBlock.getChildBlocks().addAll(((Block) additional).getChildBlocks());
                return originalAsBlock;
            }else{
                return addCommand(originalAsBlock, additional);
            }
        }else{
            if(additional instanceof Block){
                Block additionalAsBlock = ((Block) additional);
                additionalAsBlock.getChildBlocks().add(0, original);
                return additionalAsBlock;
            }else{
                List<ICommand> chainedBlocks = new ArrayList<>();
                chainedBlocks.add(original);
                chainedBlocks.add(additional);
                return new Block().withtChildBlocks(chainedBlocks);
            }
        }
    }

    public static String evaluateAgainstDynamicFields(Command command){
        return evaluateAgainstDynamicFields(command, null);
    }

    public static String evaluateAgainstDynamicFields(Command command, Map<String, String> additionalFields){
        Map<String, String> dynamicFields = command.getDynamicFields();
        String evaluatedExpression = command.getCommandText();

        if(evaluatedExpression == null || evaluatedExpression.isEmpty()){
            return evaluatedExpression;
        }
        if(dynamicFields != null) {
            for (String dynamicField : dynamicFields.keySet()) {
                evaluatedExpression = evaluatedExpression.replaceAll(dynamicField, dynamicFields.get(dynamicField));
            }
        }
        if(additionalFields != null) {
            for (String dynamicField : additionalFields.keySet()) {
                evaluatedExpression = evaluatedExpression.replaceAll(dynamicField, additionalFields.get(dynamicField));
            }
        }

        return evaluatedExpression;
    }
}
