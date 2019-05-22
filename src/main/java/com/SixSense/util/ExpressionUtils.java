package com.SixSense.util;

import com.SixSense.data.logic.*;

import java.util.ArrayList;
import java.util.List;

public class ExpressionUtils {
    //wraps both resolvables with a single logical expression;
    public static <T extends IFlowConnector> LogicalExpression<T> mergeExpressions(T original, T additional){
        return new LogicalExpression<T>()
                .addResolvable(original)
                .addResolvable(additional);
    }

    public static <T extends IFlowConnector> LogicalExpression<T> mergeExpressions(LogicalExpression<T> original, T additional){
        return new LogicalExpression<T>()
                .addExpression(original)
                .addResolvable(additional);
    }

    public static <T extends IFlowConnector> LogicalExpression<T> mergeExpressions(T original, LogicalExpression<T> additional){
        return new LogicalExpression<T>()
                .addResolvable(original)
                .addExpression(additional);
    }

    public static <T extends IFlowConnector> LogicalExpression<T> mergeExpressions(LogicalExpression<T> original, LogicalExpression<T> additional){
        return new LogicalExpression<T>()
                .addExpression(original)
                .addExpression(additional);
    }

    //returns only the leaf nodes E of the logical expression, as an ordered list
    public <T extends IFlowConnector> List<T> flatten(LogicalExpression<T> expression){
        List<T> flattened = new ArrayList<>();
        for(IResolvable member : expression.getResolvableExpressions()){
            if(member instanceof LogicalExpression){
                LogicalExpression<T> asExpression = (LogicalExpression<T>)member;
                flattened.addAll(flatten(asExpression));
            }else{
                T asGenericType = (T)member;
                flattened.add(asGenericType);
            }
        }
        return flattened;
    }
}
