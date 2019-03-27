package com.SixSense.util;

import com.SixSense.data.commands.Block;
import com.SixSense.data.commands.ICommand;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {
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
}
