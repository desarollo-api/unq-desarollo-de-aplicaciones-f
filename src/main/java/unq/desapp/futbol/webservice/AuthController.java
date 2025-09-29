package unq.desapp.futbol.webservice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.AuthRequest;
import unq.desapp.futbol.model.AuthResponse;
import unq.desapp.futbol.security.JwtTokenProvider;
import unq.desapp.futbol.security.ReactiveUserPasswordAuthenticationManager;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String BEARER = "Bearer";

    private final ReactiveUserPasswordAuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

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
