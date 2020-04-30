package com.sixsense.model.logic;

import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.interfaces.IEquatable;
import com.sixsense.utillity.ExpressionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LogicalExpression<E extends IFlowConnector> implements IResolvable, IDeepCloneable<LogicalExpression<E>>, IEquatable<LogicalExpression<E>> {
    private LinkedHashSet<IResolvable> resolvableExpressions; //specifically require LinkedHashSet, to preserve resolvable order (as argument order matters when evaluating the expression)
    private LogicalCondition logicalCondition;
    private ExpressionResult expressionResult;


    //this should be the only constructor for this class
    public LogicalExpression() {
        this.resolvableExpressions = new LinkedHashSet<>();
        this.logicalCondition = LogicalCondition.OR;
        this.expressionResult = new ExpressionResult();
    }

    public Set<IResolvable> getResolvableExpressions() {
        return Collections.unmodifiableSet(resolvableExpressions);
    }

    public LogicalExpression<E> addResolvable(E resolvable){
        this.resolvableExpressions.add(resolvable);
        return this;
    }

    public LogicalExpression<E> addExpression(LogicalExpression<E> expression){
        this.resolvableExpressions.add(expression);
        return this;
    }

    public LogicalExpression<E> addResolvables(LinkedHashSet<E> resolvables){
        for(E resolvable : resolvables){
            this.addResolvable(resolvable);
        }
        return this;
    }

    public  LogicalExpression<E> addExpressions(LinkedHashSet<LogicalExpression<E>> expressions){
        for(LogicalExpression<E> expression : expressions){
            this.addExpression(expression);
        }
        return this;
    }

    public LogicalExpression<E> mergeResolvable(E additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    public LogicalExpression<E> mergeExpression(LogicalExpression<E> additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    //this method is private to prevent unchecked type addition (i.e. add an executionCondition to an expression of Expected outcomes
    //currently only used for deep clone method
    private LogicalExpression<E> addResolvableExpressions(Set<IResolvable> expressions){
        this.resolvableExpressions.addAll(expressions);
        return this;
    }

    public LogicalCondition getLogicalCondition() {
        return logicalCondition;
    }

    public void setLogicalCondition(LogicalCondition logicalCondition) {
        this.logicalCondition = logicalCondition;
    }

    public LogicalExpression<E> withLogicalCondition(LogicalCondition logicalCondition) {
        this.logicalCondition = logicalCondition;
        return this;
    }

    @Override
    public ExpressionResult getExpressionResult() {
        return expressionResult;
    }

    @Override
    public void setExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
    }

    @Override
    public LogicalExpression<E> withExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
        return this;
    }

    public LogicalExpression<E> chainExpression(E additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    public LogicalExpression<E> chainExpression(LogicalExpression<E> additional){
        return ExpressionUtils.mergeExpressions(this, additional);
    }

    @Override
    public LogicalExpression<E> deepClone(){
        Set<IResolvable> clonedExpressions = this.resolvableExpressions.stream().map(IResolvable::deepClone).collect(Collectors.toSet());

        return new LogicalExpression<E>()
                .addResolvableExpressions(clonedExpressions)
                .withLogicalCondition(this.logicalCondition)
                .withExpressionResult(this.expressionResult.deepClone());
    }

    @Override
    public boolean weakEquals(LogicalExpression<E> other) {
        return this.resolvableExpressions.equals(other.resolvableExpressions) &&
            this.logicalCondition == other.logicalCondition;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        } else {
            LogicalExpression<E> otherAsExpression;
            try {
                otherAsExpression = (LogicalExpression<E>) other;
            }catch (ClassCastException e){
                return false;
            }

            return this.weakEquals(otherAsExpression);
        }
    }

    public boolean equals(LogicalExpression<E> other){
        return this.weakEquals(other);
    }

    @Override
    public boolean strongEquals(LogicalExpression<E> other) {
        return this.weakEquals(other) && this.expressionResult.equals(other.expressionResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resolvableExpressions, logicalCondition);
    }

    @Override
    public String toPrettyString(){
        if(resolvableExpressions.isEmpty()){
            return "LogicalExpression<Empty>";
        }else {
            String typeName = resolvableExpressions.iterator().next().getClass().getSimpleName();
            return "LogicalExpression<" + typeName + "> with aggregation " + logicalCondition.name();
        }
    }

    @Override
    public String toString() {
        return "LogicalExpression{" +
                "resolvableExpressions=" + resolvableExpressions +
                ", logicalCondition=" + logicalCondition +
                ", expressionResult=" + expressionResult +
                '}';
    }
}
