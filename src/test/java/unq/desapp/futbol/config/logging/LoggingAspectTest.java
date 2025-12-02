package unq.desapp.futbol.config.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

@Tag("unit")
class LoggingAspectTest {

    private LoggingAspect loggingAspect;
    private ProceedingJoinPoint joinPoint;
    private Signature signature;
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        // Create an instance of LoggingAspect with an injected mock logger
        loggingAspect = getTestLoggingAspect();

        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(Signature.class);

        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldInterceptServiceLayerMethods() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        assertEquals("result", result);
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    void shouldEnrichMDCWithClassAndMethodName() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");

        AtomicReference<String> capturedClassName = new AtomicReference<>();
        AtomicReference<String> capturedMethodName = new AtomicReference<>();
        AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            // Capture MDC values during execution
            capturedClassName.set(MDC.get("class_name"));
            capturedMethodName.set(MDC.get("method_name"));
            capturedCorrelationId.set(MDC.get("correlation_id"));
            return "result";
        });

        // Act
        loggingAspect.logMethodExecution(joinPoint);

        // Assert - Verify MDC was populated during execution
        assertEquals("TestService", capturedClassName.get());
        assertEquals("testMethod", capturedMethodName.get());
        assertNotNull(capturedCorrelationId.get());

        // Assert - MDC should be cleared after execution
        assertNull(MDC.get("class_name"));
        assertNull(MDC.get("method_name"));
        assertNull(MDC.get("correlation_id"));
    }

    @Test
    void shouldLogErrorAndRethrowException() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenThrow(expectedException);

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            loggingAspect.logMethodExecution(joinPoint);
        });

        assertEquals(expectedException, thrownException);
    }

    @Test
    void shouldClearMDCInFinallyBlock() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // Pre-populate MDC
        MDC.put("pre_existing_key", "pre_existing_value");

        // Act
        loggingAspect.logMethodExecution(joinPoint);

        // Assert - All MDC should be cleared
        assertNull(MDC.get("class_name"));
        assertNull(MDC.get("method_name"));
        assertNull(MDC.get("correlation_id"));
        assertNull(MDC.get("execution_time_ms"));
        assertNull(MDC.get("pre_existing_key"));
    }

    @Test
    void shouldClearMDCEvenWhenExceptionIsThrown() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        // Pre-populate MDC
        MDC.put("pre_existing_key", "pre_existing_value");

        // Act
        assertThrows(RuntimeException.class, () -> {
            loggingAspect.logMethodExecution(joinPoint);
        });

        // Assert - All MDC should be cleared even after exception
        assertNull(MDC.get("class_name"));
        assertNull(MDC.get("method_name"));
        assertNull(MDC.get("correlation_id"));
        assertNull(MDC.get("execution_time_ms"));
        assertNull(MDC.get("pre_existing_key"));
    }

    @Test
    void shouldGenerateCorrelationIdIfNotPresent() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");

        AtomicReference<String> capturedCorrelationId = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedCorrelationId.set(MDC.get("correlation_id"));
            return "result";
        });

        // Act
        loggingAspect.logMethodExecution(joinPoint);

        // Assert
        assertNotNull(capturedCorrelationId.get());
        assertFalse(capturedCorrelationId.get().isEmpty());
    }

    @Test
    void shouldReuseExistingCorrelationId() throws Throwable {
        // Arrange
        TestService testService = new TestService();
        String existingCorrelationId = "existing-correlation-id-123";
        MDC.put("correlation_id", existingCorrelationId);

        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");

        AtomicReference<String> capturedCorrelationId = new AtomicReference<>();
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            capturedCorrelationId.set(MDC.get("correlation_id"));
            return "result";
        });

        // Act
        loggingAspect.logMethodExecution(joinPoint);

        // Assert
        assertEquals(existingCorrelationId, capturedCorrelationId.get());
    }

    /**
     * Helper method to create a LoggingAspect instance with a mock logger.
     */
    private LoggingAspect getTestLoggingAspect() {
        mockLogger = mock(Logger.class);
        return new LoggingAspect() {
            @Override
            protected Logger getLogger(Class<?> clazz) {
                return mockLogger;
            }
        };
    }

    /**
     * Mock service class for testing
     */
    private static class TestService {
        @SuppressWarnings("unused")
        public String testMethod() {
            return "result";
        }
    }
}
