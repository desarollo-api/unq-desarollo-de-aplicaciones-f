package unq.desapp.futbol.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.RegisterRequest;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.security.JwtTokenProvider;
import unq.desapp.futbol.service.UserService;
import unq.desapp.futbol.config.metrics.BusinessMetric;

@RestController
@Tag(name = "Authentication")
@RequestMapping("/auth")
public class AuthController {
    private static final String BEARER = "Bearer";
    private final UserService footballService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            UserService footballService,
            JwtTokenProvider jwtTokenProvider) {
        this.footballService = footballService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    @BusinessMetric(name = "user_login", help = "Counts user login attempts")
    @Operation(summary = "User Login", description = "Authenticates a user with email and password, returning a JWT.")
    @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        return Mono.justOrEmpty(footballService.loginUser(request.getEmail(), request.getPassword()))
                .map(this::buildResponse)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")));
    }

    @PostMapping("/register")
    @BusinessMetric(name = "user_register", help = "Counts user registration attempts")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT.")
    @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid user data or email already taken", content = @Content)
    public Mono<ResponseEntity<AuthResponse>> register(@RequestBody RegisterRequest request) {
        User newUser = new User(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                Role.USER // Assign default role
        );
        return Mono.fromCallable(() -> {
            try {
                return footballService.addUser(newUser);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        })
                .map(this::buildResponse)
                .map(authResponse -> new ResponseEntity<>(authResponse, HttpStatus.CREATED));
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationTime();

        return new AuthResponse(token, BEARER, expiresIn);
    }
}
