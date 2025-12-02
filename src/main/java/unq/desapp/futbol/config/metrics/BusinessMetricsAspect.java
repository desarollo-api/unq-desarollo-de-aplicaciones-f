package unq.desapp.futbol.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BusinessMetricsAspect {

    private final MeterRegistry meterRegistry;

    public BusinessMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @AfterReturning(pointcut = "@annotation(businessMetric)", returning = "result")
    public void countSuccess(JoinPoint joinPoint, BusinessMetric businessMetric, Object result) {
        Counter.builder("app_business_operation_success_total")
                .description(businessMetric.help())
                .tag("operation", businessMetric.name())
                .tag("class", joinPoint.getSignature().getDeclaringTypeName())
                .register(meterRegistry)
                .increment();
    }

    @AfterThrowing(pointcut = "@annotation(businessMetric)", throwing = "exception")
    public void countFailure(JoinPoint joinPoint, BusinessMetric businessMetric, Exception exception) {
        Counter.builder("app_business_operation_failed_total")
                .description(businessMetric.help())
                .tag("operation", businessMetric.name())
                .tag("class", joinPoint.getSignature().getDeclaringTypeName())
                .tag("exception", exception.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }
}
