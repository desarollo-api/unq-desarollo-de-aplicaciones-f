package unq.desapp.futbol.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.constants.AuthenticationManager;
import unq.desapp.futbol.service.FootballService;

@Component
@Qualifier(AuthenticationManager.JWT)
@Primary
public class ReactiveJwtAuthenticationManager implements ReactiveAuthenticationManager {
    private final JwtTokenProvider jwtTokenProvider;
    private final FootballService footballService;

    public ReactiveJwtAuthenticationManager(JwtTokenProvider jwtTokenProvider, FootballService footballService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.footballService = footballService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!jwtTokenProvider.validateToken(token)) {
            return Mono.empty();
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);

        return Mono.justOrEmpty(footballService.findByEmail(username))
                .map(UserDetails.class::cast)
                .map(userDetails -> new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ));
    }
}
