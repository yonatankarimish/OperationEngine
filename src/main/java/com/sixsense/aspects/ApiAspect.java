package com.sixsense.aspects;

import com.sixsense.api.ApiDebuggingAware;
import com.sixsense.model.interfaces.IDeepCloneable;
import com.sixsense.model.aspects.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;

@Aspect
@Service //@Service annotation allows detecting the aspect via component-scan
/* https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#aop-ataspectj explains the usage of aop
 * https://stackoverflow.com/a/48626789/1658288 explains how spring boot detects this aspect without using the @EnableAspectJAutoProxy annotation
 * https://howtodoinjava.com/spring-aop/aspectj-pointcut-expressions/ for basic examples on writing pointcuts */
public class ApiAspect {
    private static final Logger logger = LogManager.getLogger(ApiAspect.class);

    /*execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?method-name-pattern(param-pattern) throws-pattern?)
    * params with question marks are optional
    *
    * These are really expensive aspects, which is why they should only be invoked if you know when to use them (debugging, diagnostics or test running)*/
    @Pointcut("execution(@org.springframework.web.bind.annotation.*Mapping public * com.sixsense.api.ApiDebuggingAware+.*(..))")
    private void requestMappings(){}

    @Pointcut("execution(@org.springframework.amqp.rabbit.annotation.RabbitListener public * com.sixsense.api.ApiDebuggingAware+.*(..))")
    private void rabbitListeners(){}


    @Around("requestMappings() || rabbitListeners()")
    public Object httpDiagnosticAspect(ProceedingJoinPoint pjp) throws Throwable{
        try {
            Object target = pjp.getTarget();
            if(target instanceof ApiDebuggingAware){
                ApiDebuggingAware controller = (ApiDebuggingAware)target;
                if(controller.isUnderDebug()){
                    Object[] argumentDeepCopies = tryToDeepClone(pjp.getArgs());

                    MethodInvocation methodInvocation = new MethodInvocation(pjp.getSignature().getName(), argumentDeepCopies);
                    updateAndWarn(controller, methodInvocation);

                    Object returnValue = pjp.proceed();

                    methodInvocation.getReturnValue().complete(returnValue);
                    updateAndWarn(controller, methodInvocation);
                    return returnValue;
                }else{
                    return pjp.proceed();
                }
            }else{
                throw new Exception("Http aspect target does not implement the AbstractHttpController class");
            }
        } catch (Throwable t) {
            logger.error("Failed to wrap advice around http controller. Caused by:", t);
            throw t;
        }
    }

    private Object[] tryToDeepClone(Object[] originalArguments){
        Object[] argumentDeepCopies = new Object[originalArguments.length];
        for(int i=0; i<originalArguments.length; i++){
            Object argument = originalArguments[i];
            if(argument instanceof IDeepCloneable){
                argumentDeepCopies[i] = ((IDeepCloneable)argument).deepClone();
            }else {
                argumentDeepCopies[i] = argument;
            }
        }

        return argumentDeepCopies;
    }

    private void updateAndWarn(ApiDebuggingAware controller, MethodInvocation methodInvocation){
        boolean updateSuccessful = controller.updateMethodInvocations(methodInvocation);
        if(!updateSuccessful){
            logger.warn("Failed to update method invocation status for controller " + controller.getClass().getSimpleName() + " with " + methodInvocation.toString());
        }
    }
}
