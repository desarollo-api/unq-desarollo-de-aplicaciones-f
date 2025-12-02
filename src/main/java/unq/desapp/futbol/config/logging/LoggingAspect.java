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
        // Get logger for the intercepted class (not for LoggingAspect)
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        // Extract method information
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Inject MDC context
        MDC.put("class_name", className);
        MDC.put("method_name", methodName);

        // Generate or retrieve correlation ID
        String correlationId = MDC.get("correlation_id");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlation_id", correlationId);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Execute the intercepted method
            Object result = joinPoint.proceed();

            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;
            MDC.put("execution_time_ms", String.valueOf(executionTime));

            return result;
        } catch (Throwable throwable) {
            // Log error with exception details
            logger.error(throwable.getMessage(), throwable);

            // Rethrow exception to maintain Spring's exception handling flow
            throw throwable;
        } finally {
            // Always clear MDC to prevent thread contamination
            MDC.clear();
        }
    }
}
