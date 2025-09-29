package unq.desapp.futbol.webservice;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.constants.AuthenticationManager;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.security.JwtTokenProvider;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String BEARER = "Bearer";

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
        @Qualifier(AuthenticationManager.USER_PASSWORD)
        ReactiveAuthenticationManager authenticationManager,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword());

        return authenticationManager
            .authenticate(authentication)
            .map(this::buildResponse)
            .map(ResponseEntity::ok);
    }

    private AuthResponse buildResponse(Authentication authentication) {
        String token = jwtTokenProvider.generateToken(authentication);
        long expiresIn = jwtTokenProvider.getExpirationTime();

        return new AuthResponse(token, BEARER, expiresIn);
    }
}
