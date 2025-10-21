package unq.desapp.futbol.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.model.RegisterRequest;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.security.JwtTokenProvider;
import unq.desapp.futbol.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService footballService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController controller;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "password", "Test", "User", Role.USER);
    }

    @Test
    void login_success_returnsToken_andUsesBearerType() {
        // Arrange
        AuthRequest request = new AuthRequest(testUser.getEmail(), "password");
        String expectedToken = "header.payload.signature";
        long expectedExpiresIn = 3600L;

        when(footballService.loginUser(request.getEmail(), request.getPassword())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser)).thenReturn(expectedToken);
        when(jwtTokenProvider.getExpirationTime()).thenReturn(expectedExpiresIn);

        // Act
        Mono<ResponseEntity<AuthResponse>> responseMono = controller.login(request);

        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    AuthResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getToken()).isEqualTo(expectedToken);
                    assertThat(body.getTokenType()).isEqualTo("Bearer");
                    assertThat(body.getExpiresIn()).isEqualTo(expectedExpiresIn);
                })
                .verifyComplete();
    }

    @Test
    void login_failure_propagatesException() {
        // Arrange
        AuthRequest request = new AuthRequest(testUser.getEmail(), "wrong-password");
        when(footballService.loginUser(request.getEmail(), request.getPassword())).thenReturn(Optional.empty());

        // Act & Assert
        StepVerifier.create(controller.login(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    @DisplayName("Register with valid data creates user with USER role")
    void register_success_returnsCreatedUser() {
        // Arrange
        RegisterRequest request = new RegisterRequest(testUser.getEmail(), testUser.getPassword(), testUser.getFirstName(), testUser.getLastName());
        String expectedToken = "new.user.token";
        long expectedExpiresIn = 3600L;

        // We expect the service to be called with a User object that has the USER role
        when(footballService.addUser(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn(expectedToken);
        when(jwtTokenProvider.getExpirationTime()).thenReturn(expectedExpiresIn);

        // Act
        Mono<ResponseEntity<AuthResponse>> responseMono = controller.register(request);

        // Assert
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    AuthResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getToken()).isEqualTo(expectedToken);
                })
                .verifyComplete();
        verify(footballService).addUser(any(User.class));
    }

    @Test
    void register_failure_whenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest(testUser.getEmail(), testUser.getPassword(), testUser.getFirstName(), testUser.getLastName());
        when(footballService.addUser(any(User.class))).thenThrow(new IllegalArgumentException("Email is already taken"));

        // Act & Assert
        StepVerifier.create(controller.register(request))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(error.getMessage()).contains("Email is already taken");
                })
                .verify();
    }
}
