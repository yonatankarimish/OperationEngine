package com.SixSense.data.commands;

import com.SixSense.data.logic.ExecutionCondition;
import com.SixSense.data.logic.ExpectedOutcome;
import com.SixSense.data.logic.LogicalCondition;
import com.SixSense.io.Session;
import com.SixSense.util.CommandUtils;
import com.SixSense.util.ExpectedOutcomeResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Block extends AbstractCommand implements ICommand {
    private List<ICommand> childBlocks;

    private List<ExecutionCondition> repeatConditions;
    private LogicalCondition repeatAggregation;
    private Iterator<ICommand> commandIterator;
    private ICommand currentCommand;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions and results only */
    public Block() {
        super();
        this.childBlocks = new ArrayList<>();
        this.repeatConditions = new ArrayList<>();
        this.repeatAggregation = LogicalCondition.OR;
    }

    public Block(List<ExecutionCondition> executionConditions, LogicalCondition conditionAggregation, List<ExpectedOutcome> expectedOutcomes, LogicalCondition outcomeAggregation, String aggregatedOutcomeMessage, List<ICommand> childBlocks, List<ExecutionCondition> repeatConditions, LogicalCondition repeatAggregation) {
        super(executionConditions, conditionAggregation, expectedOutcomes, outcomeAggregation, aggregatedOutcomeMessage);
        this.childBlocks = childBlocks;
        this.repeatConditions = repeatConditions;
        this.repeatAggregation = repeatAggregation;
    }

    public Block addCommand(Command childCommand){
        return CommandUtils.addCommand(this, childCommand);
    }

    public Block addBlock(Block childCommand){
        return CommandUtils.addBlock(this, childCommand);
    }

    public ICommand chainCommands(ICommand additional){
        return CommandUtils.chainCommands(this, additional);
    }

    public ICommand getNextCommand(Session context){
        //If no iterator has been created, obtain an iterator from the childBlocks list
        Iterator<ICommand> commandIterator = this.getCommandIterator();

        //If the last available command has been returned, return null
        if(hasExhaustedCommands(context)){
            return null;
        }

        //If not returned by this step, there is a next command available
        //If the last command was a plain command, or already executed, obtain the next command.
        if(this.currentCommand == null || this.currentCommand.isAlreadyExecuted()){
            this.currentCommand = commandIterator.next();
        }

        //finally, return the next command which has not been executed
        return this.currentCommand;

    }

    public boolean hasExhaustedCommands(Session context){
        if(!this.getCommandIterator().hasNext()){
            if(this.currentCommand == null){
                return true;
            }else if(!this.currentCommand.isAlreadyExecuted()){
                return false;
            }else if(ExpectedOutcomeResolver.checkExecutionConditions(context.getCurrentSessionVariables(), repeatConditions, repeatAggregation).isResolved()) {//as long as resolved, the halting condition is not met
                //If the passed block is a repeating block, which has not yet finished repeating, reset the current loop and return false
                this.resetNextCommandLoop();
                return false;
            }
            return true;
        }
        return false;
    }

    private void resetNextCommandLoop(){
        this.commandIterator = childBlocks.iterator();
        this.currentCommand = null;

        for(ICommand command : this.getChildBlocks()){
            command.setAlreadyExecuted(false);
            if(command instanceof Block){
                ((Block) command).resetNextCommandLoop();
            }
        }
    }

    public List<ExecutionCondition> getRepeatConditions() {
        return Collections.unmodifiableList(repeatConditions);
    }

    public Block addRepeatCondition(ExecutionCondition repeatCondition){
        this.repeatConditions.add(repeatCondition);
        return this;
    }

    public Block addRepeatConditions(List<ExecutionCondition> repeatConditions){
        this.repeatConditions.addAll(repeatConditions);
        return this;
    }

    public LogicalCondition getRepeatAggregation() {
        return repeatAggregation;
    }

    public void setRepeatAggregation(LogicalCondition repeatAggregation) {
        this.repeatAggregation = repeatAggregation;
    }

    public Block withRepeatAggregation(LogicalCondition repeatAggregation) {
        this.repeatAggregation = repeatAggregation;
        return this;
    }

    private Iterator<ICommand> getCommandIterator(){
        if(this.commandIterator == null){
            this.commandIterator = childBlocks.iterator();
        }
        return this.commandIterator;
    }

    public List<ICommand> getChildBlocks() {
        return Collections.unmodifiableList(childBlocks);
    }

    public Block prependChildBlock(ICommand childBlock) {
        this.childBlocks.add(0, childBlock);
        return this;
    }

    public Block addChildBlock(ICommand childBlock) {
        this.childBlocks.add(childBlock);
        return this;
    }

    public Block addChildBlocks(List<ICommand> childBlocks) {
        this.childBlocks.addAll(childBlocks);
        return this;
    }

    @Override
    public String toString() {
        return "Block{" +
                "childBlocks=" + childBlocks +
                ", repeatConditions=" + repeatConditions +
                ", repeatAggregation=" + repeatAggregation +
                ", commandIterator=" + commandIterator +
                ", currentCommand=" + currentCommand +
                ", alreadyExecuted=" + alreadyExecuted +
                ", executionConditions=" + executionConditions +
                ", conditionAggregation=" + conditionAggregation +
                ", expectedOutcomes=" + expectedOutcomes +
                ", outcomeAggregation=" + outcomeAggregation +
                ", aggregatedOutcomeMessage='" + aggregatedOutcomeMessage + '\'' +
                ", dynamicFields=" + dynamicFields +
                ", saveTo=" + saveTo +
                '}';
    }
}
