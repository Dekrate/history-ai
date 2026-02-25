package com.historyai.aspect;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for centralized logging across controllers and services.
 * Provides automatic logging of method executions, parameters, and exceptions.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger controllerLogger = LoggerFactory.getLogger("controller");
    private static final Logger serviceLogger = LoggerFactory.getLogger("service");

    /**
     * Pointcut for all controller methods in com.historyai.controller package.
     */
    @Pointcut("execution(* com.historyai.controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * Pointcut for all service methods in com.historyai.service package.
     */
    @Pointcut("execution(* com.historyai.service..*(..))")
    public void serviceMethods() {
    }

    /**
     * Logs controller method execution including parameters and response time.
     *
     * @param joinPoint the proceeding join point
     * @return method result
     * @throws Throwable if method execution fails
     */
    @Around("controllerMethods()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        String traceId = MDC.get("traceId");

        if (controllerLogger.isDebugEnabled()) {
            controllerLogger.debug("[{}] Controller -> {} - Args: {}", traceId, methodName, 
                    sanitizeArgs(args));
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            exception = ex;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            if (exception != null) {
                controllerLogger.warn("[{}] Controller -> {} - Completed with error in {}ms", 
                        traceId, methodName, duration);
            } else {
                controllerLogger.info("[{}] Controller -> {} - Completed in {}ms", 
                        traceId, methodName, duration);
            }
        }
    }

    /**
     * Logs service method execution including parameters and response time.
     *
     * @param joinPoint the proceeding join point
     * @return method result
     * @throws Throwable if method execution fails
     */
    @Around("serviceMethods()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        String traceId = MDC.get("traceId");

        if (serviceLogger.isDebugEnabled()) {
            serviceLogger.debug("[{}] Service -> {} - Args: {}", traceId, methodName, 
                    sanitizeArgs(args));
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            serviceLogger.debug("[{}] Service -> {} - Completed in {}ms", 
                    traceId, methodName, duration);
            
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            
            serviceLogger.error("[{}] Service -> {} - Failed in {}ms with: {}", 
                    traceId, methodName, duration, ex.getMessage());
            
            throw ex;
        }
    }

    /**
     * Logs exceptions thrown from controller and service methods.
     *
     * @param joinPoint the join point
     * @param exception the thrown exception
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", 
                   throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();
        String traceId = MDC.get("traceId");
        boolean isController = joinPoint.getTarget().getClass()
                .getPackageName().contains("controller");

        Logger logger = isController ? controllerLogger : serviceLogger;
        
        logger.error("[{}] Exception in {} - {}: {}", 
                traceId, methodName, 
                exception.getClass().getSimpleName(), 
                exception.getMessage());
    }

    /**
     * Sanitizes arguments to avoid logging sensitive data.
     *
     * @param args the method arguments
     * @return sanitized string representation
     */
    private Object[] sanitizeArgs(Object[] args) {
        return Arrays.stream(args)
                .map(this::sanitizeArg)
                .toArray();
    }

    /**
     * Sanitizes a single argument.
     *
     * @param arg the argument
     * @return sanitized representation
     */
    private Object sanitizeArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        String className = arg.getClass().getSimpleName();
        
        if (arg instanceof char[] || arg instanceof String) {
            return "[REDACTED]";
        }
        
        if (className.contains("Password") || className.contains("Token") 
                || className.contains("Secret") || className.contains("Key")) {
            return "[REDACTED]";
        }
        
        return arg;
    }
}
