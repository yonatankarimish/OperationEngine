package com.SixSense.aspects;

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

    @Around("execution(@org.springframework.web.bind.annotation.*Mapping public * com.SixSense.api.http.controllers.AbstractHttpController+.*(..))")
    public Object httpAspect(ProceedingJoinPoint pjp) throws Throwable{
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            logger.error("Failed to wrap advice around http controller. Caused by:", t);
            throw t;
        }
    }
}
