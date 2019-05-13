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
import java.util.stream.Collectors;

public class Block extends AbstractCommand implements ICommand {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
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
            }else if(this.repeatConditions.isEmpty()){
                return true;
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
        this.reset();
        this.commandIterator = childBlocks.iterator();
        this.currentCommand = null;

        for(ICommand command : this.getChildBlocks()){
            if(command instanceof Block){
                ((Block) command).resetNextCommandLoop();
            }else if(command instanceof Command){
                command.reset();
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

    //Returns a new instance of the same block in its pristine state. That is - as if the new state was never executed
    @Override
    public Block deepClone(){
        return assignDefaults(new Block());
    }

    //Reverts the same block instance to it's pristine state.  That is - as if the same command was never executed
    @Override
    public Block reset(){
        return assignDefaults(this);
    }

    private Block assignDefaults(Block block){
        List<ICommand> clonedChildBlocks = this.childBlocks.stream().map(ICommand::deepClone).collect(Collectors.toList());
        List<ExecutionCondition> repeatClone = this.repeatConditions.stream().map(ExecutionCondition::deepClone).collect(Collectors.toList());
        this.childBlocks.clear();
        this.repeatConditions.clear();

        return (Block)block
                .addChildBlocks(clonedChildBlocks)
                .addRepeatConditions(repeatClone)
                .withRepeatAggregation(this.repeatAggregation)
                .withSuperCloneState(this);
    }

    @Override
    public String toString() {
        return "Block{" +
                "childBlocks=" + childBlocks +
                ", repeatConditions=" + repeatConditions +
                ", repeatAggregation=" + repeatAggregation +
                ", commandIterator=" + commandIterator +
                ", currentCommand=" + currentCommand +
                ", " + super.superToString() +
                '}';
    }
}
