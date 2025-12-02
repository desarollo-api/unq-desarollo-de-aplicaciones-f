package unq.desapp.futbol.config.metrics;

import static org.mockito.Mockito.lenient;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private BusinessMetric businessMetric;

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        lenient().when(businessMetric.name()).thenReturn("test_operation");
        lenient().when(businessMetric.help()).thenReturn("Test help");
    }

    @Test
    void countSuccess_ShouldIncrementSuccessCounter() {
        SimpleMeterRegistry simpleRegistry = new SimpleMeterRegistry();
        BusinessMetricsAspect aspect = new BusinessMetricsAspect(simpleRegistry);

        aspect.countSuccess(joinPoint, businessMetric, "result");

        assert simpleRegistry.find("app_business_operation_success_total")
                .tag("operation", "test_operation")
                .tag("class", "com.example.TestClass")
                .counter() != null;

        assert simpleRegistry.find("app_business_operation_success_total")
                .counter()
                .count() == 1.0;
    }

    @Test
    void countFailure_ShouldIncrementFailureCounter() {
        SimpleMeterRegistry simpleRegistry = new SimpleMeterRegistry();
        BusinessMetricsAspect aspect = new BusinessMetricsAspect(simpleRegistry);
        Exception exception = new RuntimeException("Error");

        aspect.countFailure(joinPoint, businessMetric, exception);

        assert simpleRegistry.find("app_business_operation_failed_total")
                .tag("operation", "test_operation")
                .tag("class", "com.example.TestClass")
                .tag("exception", "RuntimeException")
                .counter() != null;

        assert simpleRegistry.find("app_business_operation_failed_total")
                .counter()
                .count() == 1.0;
    }
}
