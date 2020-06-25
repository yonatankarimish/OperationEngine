package com.sixsense.model.commands;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.model.logic.ExecutionCondition;
import com.sixsense.model.logic.ExpectedOutcome;
import com.sixsense.model.logic.LogicalExpression;
import com.sixsense.io.Session;
import com.sixsense.utillity.CommandUtils;
import com.sixsense.utillity.LogicalExpressionResolver;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Block extends AbstractCommand implements ICommand, IDeepCloneable<Block>, IEquatable<Block> {
    //When adding new variables or members, take care to update the assignDefaults() and toString() methods to avoid breaking cloning and serializing behaviour
    private List<ICommand> childBlocks;

    private LogicalExpression<ExecutionCondition> repeatCondition;
    private Iterator<ICommand> commandIterator;
    private ICommand currentCommand;

    /*Try not to pollute with additional constructors
     * The empty constructor is for using the 'with' design pattern
     * The parameterized constructor is for conditions and results only */
    public Block() {
        super();
        this.childBlocks = new ArrayList<>();
        this.repeatCondition = new LogicalExpression<>();
    }

    public Block(LogicalExpression<ExecutionCondition> executionCondition, LogicalExpression<ExpectedOutcome> expectedOutcome, List<ICommand> childBlocks, LogicalExpression<ExecutionCondition> repeatCondition) {
        super(executionCondition, expectedOutcome);
        this.childBlocks = childBlocks;
        this.repeatCondition = repeatCondition;
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
            }else if(this.repeatCondition.getResolvableExpressions().isEmpty()){
                return true;
            }else if(LogicalExpressionResolver.resolveLogicalExpression(context.getCurrentSessionVariables(), repeatCondition).isResolved()) {//as long as resolved, the halting condition is not met
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

    public LogicalExpression<ExecutionCondition> getRepeatCondition() {
        return repeatCondition;
    }

    public void setRepeatCondition(LogicalExpression<ExecutionCondition> repeatCondition) {
        this.repeatCondition = repeatCondition;
    }

    public Block withRepeatCondition(LogicalExpression<ExecutionCondition> repeatCondition) {
        this.repeatCondition = repeatCondition;
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
        if(this == block) {
            this.childBlocks.clear();
        }

        return (Block)block
                .addChildBlocks(clonedChildBlocks)
                .withRepeatCondition(block.repeatCondition.deepClone())
                .withSuperCloneState(this);
    }

    @Override
    public boolean weakEquals(Block other) {
        return super.weakEquals(other) && this.equals(other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || this.getClass() != other.getClass()) {
            return false;
        } else {
            Block asBlock = (Block) other;
            return super.equals(asBlock) && this.equals(asBlock);
        }
    }

    public boolean equals(Block other) {
        return this.childBlocks.equals(other.childBlocks) &&
            this.repeatCondition.equals(other.repeatCondition);
    }

    @Override
    public boolean strongEquals(Block other) {
        return super.strongEquals(other) && this.equals(other);
    }

    @Override
    public int hashCode() {
        Stream<Object> childStream = Arrays.stream(new Object[]{childBlocks, repeatCondition});
        Stream<Object> superStream = Arrays.stream(superMembers());

        Object[] mergedMembers = Stream.concat(superStream, childStream).toArray();
        return Arrays.hashCode(mergedMembers);
    }

    @Override
    public String toString() {
        return "Block{" +
                "childBlocks=" + childBlocks +
                ", repeatCondition=" + repeatCondition +
                ", " + super.superToString() +
                '}';
    }
}
