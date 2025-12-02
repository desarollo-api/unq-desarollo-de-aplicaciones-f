package unq.desapp.futbol.config.logging;

import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

/**
 * Aspect for cross-cutting logging concerns.
 * Automatically intercepts methods in @Service and @RestController classes
 * to provide standardized logging with MDC enrichment.
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all methods in classes annotated with @Service
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceLayer() {
        // Pointcut definition
    }

    /**
     * Pointcut for all methods in classes annotated with @RestController
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void webLayer() {
        // Pointcut definition
    }

    /**
     * Around advice that intercepts service and controller methods.
     * Enriches MDC with contextual information, measures execution time,
     * and handles exceptions.
     *
     * @param joinPoint the join point representing the intercepted method
     * @return the result of the method execution
     * @throws Throwable if the intercepted method throws an exception
     */
    @Around("serviceLayer() || webLayer()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = getLogger(joinPoint.getTarget().getClass());
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Generate or retrieve correlation ID
        String correlationId = MDC.get("correlation_id");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            if (result instanceof Mono) {
                String finalCorrelationId = correlationId;
                return ((Mono<?>) result)
                        .doOnEach(signal -> log(logger, className, methodName, startTime, finalCorrelationId, signal))
                        .contextWrite(ctx -> ctx.put("correlation_id", finalCorrelationId));
            }

            if (result instanceof Flux) {
                String finalCorrelationId = correlationId;
                return ((Flux<?>) result)
                        .doOnEach(signal -> log(logger, className, methodName, startTime, finalCorrelationId, signal))
                        .contextWrite(ctx -> ctx.put("correlation_id", finalCorrelationId));
            }

            logSuccess(logger, className, methodName, correlationId, startTime);
            return result;
        } catch (Throwable throwable) {
            logError(logger, className, methodName, correlationId, startTime, throwable);
            throw throwable;
        }
    }

    private void log(Logger logger, String className, String methodName, long startTime,
            String finalCorrelationId, Signal<?> signal) {
        if (signal.isOnComplete()) {
            logSuccess(logger, className, methodName, finalCorrelationId, startTime);
        } else if (signal.isOnError()) {
            logError(logger, className, methodName, finalCorrelationId, startTime,
                    signal.getThrowable());
        }
    }

    private void logSuccess(Logger logger, String className, String methodName, String correlationId, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        try (MDC.MDCCloseable c1 = MDC.putCloseable("class_name", className);
                MDC.MDCCloseable c2 = MDC.putCloseable("method_name", methodName);
                MDC.MDCCloseable c3 = MDC.putCloseable("correlation_id", correlationId);
                MDC.MDCCloseable c4 = MDC.putCloseable("execution_time_ms", String.valueOf(executionTime))) {

            logger.info("Method executed successfully in {} ms", executionTime);
        }
    }

    private void logError(Logger logger, String className, String methodName, String correlationId, long startTime,
            Throwable throwable) {
        long executionTime = System.currentTimeMillis() - startTime;
        try (MDC.MDCCloseable c1 = MDC.putCloseable("class_name", className);
                MDC.MDCCloseable c2 = MDC.putCloseable("method_name", methodName);
                MDC.MDCCloseable c3 = MDC.putCloseable("correlation_id", correlationId);
                MDC.MDCCloseable c4 = MDC.putCloseable("execution_time_ms", String.valueOf(executionTime))) {

            logger.error("Error executing method: {}", throwable.getMessage(), throwable);
        }
    }

    /**
     * Protected method to get logger.
     *
     * @param clazz the class to get the logger for
     * @return the logger
     */
    protected Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
