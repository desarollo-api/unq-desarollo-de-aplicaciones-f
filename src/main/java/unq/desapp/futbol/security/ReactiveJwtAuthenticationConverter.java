package unq.desapp.futbol.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.security.Constants.Jwt;

@Component
public class ReactiveJwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(findJwt(exchange))
            .filter(this::validateJwt)
            .map(this::extractJwt)
            .map(token -> new UsernamePasswordAuthenticationToken(token, token));
    }

    private String findJwt(ServerWebExchange exchange) {
        return exchange.getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);
    }

    private boolean validateJwt(String authHeader) {
        return StringUtils.hasText(authHeader) && authHeader.startsWith(Jwt.PREFIX);
    }

    private String extractJwt(String authHeader) {
        return authHeader.substring(Jwt.PREFIX.length());
    }
}
