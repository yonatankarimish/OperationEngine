package com.SixSense.data.commands;

import com.SixSense.data.Outcomes.ExpectedOutcome;
import com.SixSense.util.BlockUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Block implements ICommand {
    private int repeatCount;
    private int internalCounter;
    private List<ICommand> childBlocks;
    private List<ExpectedOutcome> expectedOutcomes;

    private Iterator<ICommand> commandIterator;
    private ICommand currentCommand;

    public Block() {
        this.internalCounter = 0;
        this.childBlocks = new ArrayList<>();
        this.expectedOutcomes = new ArrayList<>();
    }

    public Block(int repeatCount, List<ICommand> childBlocks, List<ExpectedOutcome> expectedOutcomes) {
        if(repeatCount < 0){
            throw new IllegalArgumentException("Cannot construct Block with a negative repeat count");
        } else {
            this.internalCounter = 0;
            this.repeatCount = repeatCount;
            this.childBlocks = childBlocks;
            this.expectedOutcomes = expectedOutcomes;
        }
    }

    public Block addCommands(ICommand childCommand){
        return BlockUtils.addCommand(this, childCommand);
    }

    public Block chainCommands(ICommand additional){
        return BlockUtils.chainCommands(this, additional);
    }

    public Command getNextCommand(){
        //If no iterator has been created, obtain an iterator from the childBlocks list
        Iterator<ICommand> commandIterator = this.getCommandIterator();

        //If the last available command has been returned, return null
        if(hasExhaustedCommands(commandIterator)){
            return null;
        }

        //If not returned by this step, there is a next command available
        //If the last command was a plain command, or never requested, obtain the next command.
        if(this.currentCommand == null || this.currentCommand instanceof Command){
            this.currentCommand = commandIterator.next();
        }

        //If the command we just got is a plain command, return it to the invoker.
        if(this.currentCommand instanceof Command){
            return (Command)this.currentCommand;
        }

        //If the command we just got is a block of commands
        if(this.currentCommand instanceof Block){
            Command childCommand = ((Block) this.currentCommand).getNextCommand();
            if(childCommand != null){
                return childCommand;
            } else {
                //the block has exhausted all it's commands and has no more commands to return
                //We iterate to the next command, and invoke ourselves recursively to return
                this.currentCommand = commandIterator.next();
                return getNextCommand();
            }
        }

        //If neither a command or a block (that is, null), return null.
        return null;
    }

    public boolean hasExhaustedCommands(){
        return hasExhaustedCommands(this.getCommandIterator());
    }

    private boolean hasExhaustedCommands(Iterator<ICommand> commandIterator){
        if(!commandIterator.hasNext()){
            boolean childExhaustedCommands = true;
            if(this.currentCommand instanceof Block){
                childExhaustedCommands = hasExhaustedCommands(((Block) this.currentCommand).getCommandIterator());
            }

            if(!childExhaustedCommands){
                //If the current command is a block command, which has not exhausted it's own child commands, return false
                return false;
            }else if(internalCounter < repeatCount) {
                //If this block is a repeating block, which has not yet finished repeating, reset the current loop and return false
                this.resetNextCommandLoop();
                return false;
            }
            return true;
        }
        return false;
    }

    private void resetNextCommandLoop(){
        this.internalCounter++;
        if(this.commandIterator != null) {
            this.commandIterator = this.childBlocks.iterator();
        }

        for(ICommand command : this.getChildBlocks()){
            if(command instanceof Block){
                ((Block) command).resetNextCommandLoop();
                ((Block) command).internalCounter = 0;
            }
        }
    }

    private Iterator<ICommand> getCommandIterator(){
        if(this.commandIterator == null){
            this.commandIterator = childBlocks.iterator();
        }
        return this.commandIterator;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        if(repeatCount < 0) {
            throw new IllegalArgumentException("Cannot construct Block with a negative repeat count");
        } else {
            this.repeatCount = repeatCount;
        }
    }

    public Block withRepeatCount(int repeatCount) {
        if(repeatCount < 0) {
            throw new IllegalArgumentException("Cannot construct Block with a negative repeat count");
        } else {
            this.repeatCount = repeatCount;
            return this;
        }
    }

    public List<ICommand> getChildBlocks() {
        return childBlocks;
    }

    public void setChildBlocks(List<ICommand> childBlocks) {
        this.childBlocks = childBlocks;
    }

    public Block withtChildBlocks(List<ICommand> childBlocks) {
        this.childBlocks = childBlocks;
        return this;
    }

    @Override
    public List<ExpectedOutcome> getExpectedOutcomes() {
        return expectedOutcomes;
    }

    @Override
    public void setExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
    }

    @Override
    public Block withExpectedOutcomes(List<ExpectedOutcome> expectedOutcomes) {
        this.expectedOutcomes = expectedOutcomes;
        return this;
    }

    @Override
    public String toString() {
        return "Block{" +
                "repeatCount=" + repeatCount +
                ", internalCounter=" + internalCounter +
                ", childBlocks=" + childBlocks +
                ", expectedOutcomes=" + expectedOutcomes +
                ", commandIterator=" + commandIterator +
                ", currentCommand=" + currentCommand +
                '}';
    }
}
