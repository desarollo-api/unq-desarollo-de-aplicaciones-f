package unq.desapp.futbol.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.constants.AuthenticationManager;

@Component
@Qualifier(AuthenticationManager.JWT)
@Primary
@RequiredArgsConstructor
public class ReactiveJwtAuthenticationManager implements ReactiveAuthenticationManager {
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!jwtTokenProvider.validateToken(token)) {
            return Mono.empty();
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);

        return userDetailsService.findByUsername(username)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ));
    }
}
