package unq.desapp.futbol.config.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Tag("unit")
@SuppressWarnings("unchecked")
class LoggingAspectTest {

    private LoggingAspect loggingAspect;
    private ProceedingJoinPoint joinPoint;
    private Signature signature;
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        loggingAspect = new LoggingAspect() {
            @Override
            protected Logger getLogger(Class<?> clazz) {
                return mockLogger;
            }
        };

        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(Signature.class);

        // Setup common mock behavior
        TestService testService = new TestService();
        when(joinPoint.getTarget()).thenReturn(testService);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");

        // Verify MDC context when logger.info is called
        doAnswer(invocation -> {
            assertEquals("TestService", MDC.get("class_name"), "MDC class_name should be set");
            assertEquals("testMethod", MDC.get("method_name"), "MDC method_name should be set");
            return null;
        }).when(mockLogger).info(anyString(), anyLong());

        // Verify MDC context when logger.error is called
        doAnswer(invocation -> {
            assertEquals("TestService", MDC.get("class_name"), "MDC class_name should be set");
            assertEquals("testMethod", MDC.get("method_name"), "MDC method_name should be set");
            return null;
        }).when(mockLogger).error(anyString(), anyString(), any(Throwable.class));
    }

    @Test
    void shouldLogSynchronousSuccess() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        assertEquals("result", result);
        verify(mockLogger).info(eq("Method executed successfully in {} ms"), anyLong());
    }

    @Test
    void shouldLogSynchronousError() throws Throwable {
        // Arrange
        RuntimeException exception = new RuntimeException("Error");
        when(joinPoint.proceed()).thenThrow(exception);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> loggingAspect.logMethodExecution(joinPoint));

        verify(mockLogger).error(eq("Error executing method: {}"), anyString(), eq(exception));
    }

    @Test
    void shouldLogMonoSuccess() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn(Mono.just("result"));

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        StepVerifier.create((Mono<String>) result)
                .expectNext("result")
                .verifyComplete();

        verify(mockLogger).info(eq("Method executed successfully in {} ms"), anyLong());
    }

    @Test
    void shouldLogMonoError() throws Throwable {
        // Arrange
        RuntimeException exception = new RuntimeException("Error");
        when(joinPoint.proceed()).thenReturn(Mono.error(exception));

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        StepVerifier.create((Mono<String>) result)
                .verifyError(RuntimeException.class);

        verify(mockLogger).error(eq("Error executing method: {}"), anyString(), eq(exception));
    }

    @Test
    void shouldLogFluxSuccess() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        StepVerifier.create((Flux<String>) result)
                .expectNext("result1")
                .expectNext("result2")
                .verifyComplete();

        verify(mockLogger).info(eq("Method executed successfully in {} ms"), anyLong());
    }

    @Test
    void shouldLogFluxError() throws Throwable {
        // Arrange
        RuntimeException exception = new RuntimeException("Error");
        when(joinPoint.proceed()).thenReturn(Flux.error(exception));

        // Act
        Object result = loggingAspect.logMethodExecution(joinPoint);

        // Assert
        StepVerifier.create((Flux<String>) result)
                .verifyError(RuntimeException.class);

        verify(mockLogger).error(eq("Error executing method: {}"), anyString(), eq(exception));
    }

    private static class TestService {
        public String testMethod() {
            return "result";
        }
    }
}
