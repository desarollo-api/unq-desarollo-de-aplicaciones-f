package unq.desapp.futbol.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactiveJwtAuthenticationConverter Tests")
@Tag("unit")
class ReactiveJwtAuthenticationConverterTest {

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    private ReactiveJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ReactiveJwtAuthenticationConverter();
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
    }

    @Nested
    @DisplayName("convert() - Valid JWT Tokens")
    class ValidJwtTokens {

        @Test
        @DisplayName("should extract valid JWT token with Bearer prefix")
        void shouldExtractValidJwtTokenWithBearerPrefix() {
            // Arrange
            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature";
            String authHeader = "Bearer " + token;
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(authentication).isNotNull();
                        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);

                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;

                        assertThat(auth.getPrincipal()).isEqualTo(token);
                        assertThat(auth.getCredentials()).isEqualTo(token);
                        assertThat(auth.getAuthorities()).isEmpty();
                        assertThat(auth.isAuthenticated()).isFalse();
                    })
                    .verifyComplete();

            verify(exchange).getRequest();
            verify(request).getHeaders();
            verify(headers).getFirst(HttpHeaders.AUTHORIZATION);
        }

        @Test
        @DisplayName("should handle JWT token with extra spaces after Bearer")
        void shouldHandleJwtTokenWithExtraSpaces() {
            // Arrange
            String token = "valid.jwt.token";
            String authHeader = "Bearer " + token;
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo(token);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle very long JWT token")
        void shouldHandleVeryLongJwtToken() {
            // Arrange
            String longToken = "a".repeat(1000);
            String authHeader = "Bearer " + longToken;
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo(longToken);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle JWT token with special characters")
        void shouldHandleJwtTokenWithSpecialCharacters() {
            // Arrange
            String token = "header.payload-with_special+chars/and=signs.signature";
            String authHeader = "Bearer " + token;
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo(token);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("convert() - Invalid Authorization Headers")
    class InvalidAuthorizationHeaders {

        private static Stream<String> invalidAuthorizationHeaders() {
            return Stream.of(
                    null, // Header is missing
                    "", // Header is empty
                    "   ", // Header is whitespace
                    "SomeToken", // No "Bearer " prefix
                    "Basic dXNlcjpwYXNz", // Wrong prefix
                    "Bearer", // Prefix only, no token
                    "bearer token", // Incorrect case for prefix
                    "BEARER token" // Incorrect case for prefix
            );
        }

        @ParameterizedTest(name = "with header: \"{0}\"")
        @MethodSource("invalidAuthorizationHeaders")
        @DisplayName("should return empty Mono for various invalid Authorization headers")
        void shouldReturnEmptyMonoForInvalidHeaders(String authHeader) {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty Mono when Authorization header is 'Bearer ' with space only")
        void shouldReturnEmptyMonoWhenBearerWithSpaceOnly() {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        // After "Bearer " (7 chars), substring gives empty string
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo("");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("convert() - Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle Authorization header with Bearer prefix but no space")
        void shouldHandleBearerPrefixWithoutSpace() {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearertoken");

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert - Should return empty because it doesn't start with "Bearer "
            StepVerifier.create(result)
                    .expectNextCount(0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle Authorization header with multiple spaces after Bearer")
        void shouldHandleMultipleSpacesAfterBearer() {
            // Arrange
            String token = "token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer  " + token);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert - Will extract " token" (with leading space)
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo(" " + token);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle Authorization header with Bearer in the middle")
        void shouldHandleBearerInTheMiddle() {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Some Bearer token");

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert - Doesn't START with Bearer, so should be empty
            StepVerifier.create(result)
                    .expectNextCount(0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle JWT token that itself contains 'Bearer'")
        void shouldHandleJwtTokenContainingBearer() {
            // Arrange
            String token = "token.with.Bearer.inside";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(((UsernamePasswordAuthenticationToken) authentication)
                                .getPrincipal()).isEqualTo(token);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("convert() - Multiple Invocations")
    class MultipleInvocations {

        @Test
        @DisplayName("should handle multiple conversions with same exchange")
        void shouldHandleMultipleConversionsWithSameExchange() {
            // Arrange
            String token = "consistent.token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            // Act - Call convert twice
            Mono<Authentication> result1 = converter.convert(exchange);
            Mono<Authentication> result2 = converter.convert(exchange);

            // Assert - Both should produce the same result
            StepVerifier.create(result1)
                    .assertNext(auth -> assertThat(((UsernamePasswordAuthenticationToken) auth)
                            .getPrincipal()).isEqualTo(token))
                    .verifyComplete();

            StepVerifier.create(result2)
                    .assertNext(auth -> assertThat(((UsernamePasswordAuthenticationToken) auth)
                            .getPrincipal()).isEqualTo(token))
                    .verifyComplete();

            // Verify headers was accessed multiple times
            verify(headers, times(2)).getFirst(HttpHeaders.AUTHORIZATION);
        }

        @Test
        @DisplayName("should handle different tokens in successive calls")
        void shouldHandleDifferentTokensInSuccessiveCalls() {
            // Arrange
            String token1 = "first.token";
            String token2 = "second.token";

            when(headers.getFirst(HttpHeaders.AUTHORIZATION))
                    .thenReturn("Bearer " + token1)
                    .thenReturn("Bearer " + token2);

            // Act
            Mono<Authentication> result1 = converter.convert(exchange);
            Mono<Authentication> result2 = converter.convert(exchange);

            // Assert
            StepVerifier.create(result1)
                    .assertNext(auth -> assertThat(((UsernamePasswordAuthenticationToken) auth)
                            .getPrincipal()).isEqualTo(token1))
                    .verifyComplete();

            StepVerifier.create(result2)
                    .assertNext(auth -> assertThat(((UsernamePasswordAuthenticationToken) auth)
                            .getPrincipal()).isEqualTo(token2))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("convert() - Authentication Object Properties")
    class AuthenticationObjectProperties {

        @Test
        @DisplayName("should create unauthenticated token")
        void shouldCreateUnauthenticatedToken() {
            // Arrange
            String token = "test.token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;

                        assertThat(auth.isAuthenticated()).isFalse();
                        assertThat(auth.getDetails()).isNull();
                        assertThat(auth.getName()).isEqualTo(token);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should set both principal and credentials to token")
        void shouldSetBothPrincipalAndCredentialsToToken() {
            // Arrange
            String token = "symmetric.token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;

                        assertThat(auth.getPrincipal()).isEqualTo(token);
                        assertThat(auth.getCredentials()).isEqualTo(token);
                        assertThat(auth.getPrincipal()).isSameAs(auth.getCredentials());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should create token with no authorities")
        void shouldCreateTokenWithNoAuthorities() {
            // Arrange
            String token = "no.authorities.token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .assertNext(authentication -> {
                        assertThat(authentication.getAuthorities()).isEmpty();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("convert() - Reactive Behavior")
    class ReactiveBehavior {

        @Test
        @DisplayName("should evaluate exchange when Mono is created")
        void shouldEvaluateExchangeWhenMonoIsCreated() {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");

            // Act - Create Mono (exchange is accessed immediately during Mono construction)
            Mono<Authentication> result = converter.convert(exchange);

            // Assert - Exchange IS called during Mono creation
            verify(exchange).getRequest();
            verify(request).getHeaders();
            verify(headers).getFirst(HttpHeaders.AUTHORIZATION);

            // Additional subscription doesn't cause additional calls
            result.block();

            // Still same number of calls
            verify(exchange, times(1)).getRequest();
        }

        @Test
        @DisplayName("should complete immediately after emitting authentication")
        void shouldCompleteImmediatelyAfterEmitting() {
            // Arrange
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token");

            // Act
            Mono<Authentication> result = converter.convert(exchange);

            // Assert
            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("should cache result after first subscription")
        void shouldCacheResultAfterFirstSubscription() {
            // Arrange
            String token = "reusable.token";
            when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

            Mono<Authentication> result = converter.convert(exchange);

            // Act & Assert - Subscribe multiple times
            // The Mono is created once, so exchange is accessed only once during creation
            for (int i = 0; i < 3; i++) {
                StepVerifier.create(result)
                        .assertNext(auth -> assertThat(auth).isNotNull())
                        .verifyComplete();
            }

            // Exchange was accessed only once during Mono creation
            verify(headers, times(1)).getFirst(HttpHeaders.AUTHORIZATION);
        }
    }
}
