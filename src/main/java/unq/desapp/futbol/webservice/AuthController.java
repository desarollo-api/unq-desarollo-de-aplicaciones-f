package unq.desapp.futbol.webservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.FootballService;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.constants.AuthenticationManager;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.security.JwtTokenProvider;

@RestController
@Tag(name = "Authentication")
@RequestMapping("/auth")
public class AuthController {
    private static final String BEARER = "Bearer";
    private final FootballService footballService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
        FootballService footballService,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.footballService = footballService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
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
    @Operation(summary = "Register a new user", description = "Creates a new user account.")
    @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Invalid user data or email already taken", content = @Content)
    public Mono<ResponseEntity<User>> register(@RequestBody User newUser) {
        return Mono.fromCallable(() -> {
                    try {
                        return footballService.addUser(newUser);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                    }
                })
                .map(createdUser -> new ResponseEntity<>(createdUser, HttpStatus.CREATED));
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationTime();

        return new AuthResponse(token, BEARER, expiresIn);
    }
}
