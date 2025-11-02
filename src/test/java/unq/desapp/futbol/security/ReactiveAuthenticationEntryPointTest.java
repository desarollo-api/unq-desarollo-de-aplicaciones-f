package unq.desapp.futbol.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactiveAuthenticationEntryPoint Tests")
class ReactiveAuthenticationEntryPointTest {

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private HttpHeaders headers;

    private DataBufferFactory bufferFactory;
    private ReactiveAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        bufferFactory = new DefaultDataBufferFactory();
        entryPoint = new ReactiveAuthenticationEntryPoint();

        // Configure mocks
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(response.writeWith(any())).thenReturn(Mono.empty());
    }

    @Nested
    @DisplayName("commence() - Standard Authentication Exceptions")
    class StandardAuthenticationExceptions {

        @Test
        @DisplayName("should handle BadCredentialsException with proper error response")
        void shouldHandleBadCredentialsException() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Invalid credentials");

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(headers).setContentType(MediaType.APPLICATION_JSON);

            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            Mono<DataBuffer> capturedMono = captor.getValue();
            DataBuffer buffer = capturedMono.block();

            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);
            assertThat(json).contains("\"error\": \"Unauthorized\"");
            assertThat(json).contains("\"message\": \"Invalid credentials\"");
        }

        @Test
        @DisplayName("should handle InsufficientAuthenticationException")
        void shouldHandleInsufficientAuthenticationException() {
            // Arrange
            AuthenticationException exception = new InsufficientAuthenticationException(
                    "Full authentication is required");

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(headers).setContentType(MediaType.APPLICATION_JSON);

            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).contains("\"error\": \"Unauthorized\"");
            assertThat(json).contains("\"message\": \"Full authentication is required\"");
        }

        @Test
        @DisplayName("should handle generic AuthenticationException")
        void shouldHandleGenericAuthenticationException() {
            // Arrange
            AuthenticationException exception = new AuthenticationException("Generic auth error") {
            };

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(headers).setContentType(MediaType.APPLICATION_JSON);
        }
    }

    @Nested
    @DisplayName("commence() - Error Message Content")
    class ErrorMessageContent {

        @Test
        @DisplayName("should format JSON response correctly")
        void shouldFormatJsonResponseCorrectly() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test message");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            // Verify JSON structure
            assertThat(json).startsWith("{\"error\": \"Unauthorized\", \"message\": \"");
            assertThat(json).endsWith("\"}");
            assertThat(json).contains("Test message");
        }

        @Test
        @DisplayName("should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() {
            // Arrange
            AuthenticationException exception = new AuthenticationException(null) {
            };

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).contains("\"error\": \"Unauthorized\"");
            assertThat(json).contains("\"message\": \"null\"");
        }

        @Test
        @DisplayName("should handle exception with empty message")
        void shouldHandleExceptionWithEmptyMessage() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).isEqualTo("{\"error\": \"Unauthorized\", \"message\": \"\"}");
        }

        @Test
        @DisplayName("should handle exception with special characters in message")
        void shouldHandleExceptionWithSpecialCharacters() {
            // Arrange
            String messageWithSpecialChars = "Error: \"authentication\" failed & <token> invalid";
            AuthenticationException exception = new BadCredentialsException(messageWithSpecialChars);

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            // Note: Current implementation doesn't escape special JSON characters
            // This documents the actual behavior
            assertThat(json).contains(messageWithSpecialChars);
        }

        @Test
        @DisplayName("should handle exception with very long message")
        void shouldHandleExceptionWithVeryLongMessage() {
            // Arrange
            String longMessage = "Error: " + "x".repeat(1000);
            AuthenticationException exception = new BadCredentialsException(longMessage);

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).contains(longMessage);
            assertThat(json.length()).isGreaterThan(1000);
        }

        @Test
        @DisplayName("should handle exception with newlines in message")
        void shouldHandleExceptionWithNewlines() {
            // Arrange
            String messageWithNewlines = "Line 1\nLine 2\nLine 3";
            AuthenticationException exception = new BadCredentialsException(messageWithNewlines);

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).contains(messageWithNewlines);
        }

        @Test
        @DisplayName("should handle exception with unicode characters")
        void shouldHandleExceptionWithUnicodeCharacters() {
            // Arrange
            String messageWithUnicode = "Error: 用户认证失败 🔒";
            AuthenticationException exception = new BadCredentialsException(messageWithUnicode);

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            String json = new String(getBytes(buffer), StandardCharsets.UTF_8);

            assertThat(json).contains(messageWithUnicode);
        }
    }

    @Nested
    @DisplayName("commence() - HTTP Response Configuration")
    class HttpResponseConfiguration {

        @Test
        @DisplayName("should set status code to UNAUTHORIZED (401)")
        void shouldSetStatusCodeToUnauthorized() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(response).setStatusCode(eq(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("should set Content-Type to application/json")
        void shouldSetContentTypeToApplicationJson() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            verify(headers).setContentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("should write response body")
        void shouldWriteResponseBody() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            verify(response).writeWith(any(Mono.class));
        }

        @Test
        @DisplayName("should use UTF-8 encoding for response body")
        void shouldUseUtf8Encoding() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test: café ñ");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            ArgumentCaptor<Mono<DataBuffer>> captor = ArgumentCaptor.forClass(Mono.class);
            verify(response).writeWith(captor.capture());

            DataBuffer buffer = captor.getValue().block();
            byte[] bytes = getBytes(buffer);
            String json = new String(bytes, StandardCharsets.UTF_8);

            assertThat(json).contains("café ñ");
        }

        @Test
        @DisplayName("should call exchange.getResponse() to get response object")
        void shouldCallExchangeGetResponse() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            verify(exchange, atLeastOnce()).getResponse();
        }

        @Test
        @DisplayName("should call response.bufferFactory() to create buffer")
        void shouldCallResponseBufferFactory() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert
            verify(response).bufferFactory();
        }
    }

    @Nested
    @DisplayName("commence() - Reactive Behavior")
    class ReactiveBehavior {

        @Test
        @DisplayName("should return Mono that completes successfully")
        void shouldReturnMonoThatCompletesSuccessfully() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should execute operations eagerly when commence is called")
        void shouldExecuteOperationsEagerlyWhenCommenceIsCalled() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act - Create Mono (operations are executed immediately)
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert - Operations ARE executed during Mono creation
            assertThat(result).isNotNull();
            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(headers).setContentType(MediaType.APPLICATION_JSON);
            verify(response).bufferFactory();

            // Subscription completes the writeWith operation
            result.block();
            verify(response).writeWith(any(Mono.class));
        }

        @Test
        @DisplayName("should allow multiple subscriptions to the returned Mono without repeating setup")
        void shouldAllowMultipleSubscriptionsWithoutRepeatingSetup() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Act & Assert - Subscribe multiple times to the SAME Mono
            StepVerifier.create(result).verifyComplete();
            StepVerifier.create(result).verifyComplete();
            StepVerifier.create(result).verifyComplete();

            // Setup operations happen once because the response was prepared eagerly
            verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(headers, times(1)).setContentType(MediaType.APPLICATION_JSON);
            verify(response, times(1)).writeWith(any(Mono.class));
        }

        @Test
        @DisplayName("should propagate errors from writeWith")
        void shouldPropagateErrorsFromWriteWith() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");
            RuntimeException writeError = new RuntimeException("Write failed");
            when(response.writeWith(any())).thenReturn(Mono.error(writeError));

            // Act
            Mono<Void> result = entryPoint.commence(exchange, exception);

            // Assert
            StepVerifier.create(result)
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("commence() - Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("should execute all steps in correct order")
        void shouldExecuteAllStepsInCorrectOrder() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");
            when(response.writeWith(any())).thenAnswer(invocation -> {
                // Verify that status and headers were set before writeWith is called
                verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
                verify(headers).setContentType(MediaType.APPLICATION_JSON);
                return Mono.empty();
            });

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert - verify order
            var inOrder = inOrder(response, headers);
            inOrder.verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            inOrder.verify(response).getHeaders();
            inOrder.verify(headers).setContentType(MediaType.APPLICATION_JSON);
            inOrder.verify(response).bufferFactory();
            inOrder.verify(response).writeWith(any());
        }

        @Test
        @DisplayName("should handle concurrent commence calls")
        void shouldHandleConcurrentCommenceCalls() {
            // Arrange
            AuthenticationException exception1 = new BadCredentialsException("Error 1");
            AuthenticationException exception2 = new BadCredentialsException("Error 2");

            // Act
            Mono<Void> result1 = entryPoint.commence(exchange, exception1);
            Mono<Void> result2 = entryPoint.commence(exchange, exception2);

            // Assert - Both should complete successfully
            StepVerifier.create(Mono.when(result1, result2))
                    .verifyComplete();

            verify(response, times(2)).setStatusCode(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should verify all mocked interactions occurred")
        void shouldVerifyAllMockedInteractionsOccurred() {
            // Arrange
            AuthenticationException exception = new BadCredentialsException("Test");

            // Act
            entryPoint.commence(exchange, exception).block();

            // Assert - Comprehensive verification
            verify(exchange, atLeastOnce()).getResponse();
            verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            verify(response).getHeaders();
            verify(headers).setContentType(MediaType.APPLICATION_JSON);
            verify(response).bufferFactory();
            verify(response).writeWith(any(Mono.class));
            verifyNoMoreInteractions(response, headers);
        }

        @Test
        @DisplayName("should work with different exception types in sequence")
        void shouldWorkWithDifferentExceptionTypesInSequence() {
            // Arrange & Act & Assert
            AuthenticationException[] exceptions = {
                    new BadCredentialsException("Bad creds"),
                    new InsufficientAuthenticationException("Not authenticated"),
                    new AuthenticationException("Generic") {
                    }
            };

            for (AuthenticationException exception : exceptions) {
                reset(response, headers);
                when(exchange.getResponse()).thenReturn(response);
                when(response.getHeaders()).thenReturn(headers);
                when(response.bufferFactory()).thenReturn(bufferFactory);
                when(response.writeWith(any())).thenReturn(Mono.empty());

                StepVerifier.create(entryPoint.commence(exchange, exception))
                        .verifyComplete();

                verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    // Helper method to extract bytes from DataBuffer
    private byte[] getBytes(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        return bytes;
    }
}
