package unq.desapp.futbol.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEFAULT_TOKEN = "";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
        throws ServletException, IOException {
        String jwt = buildJwt(request);

        if (StringUtils.hasText(jwt)) {
            try {
                if (jwtTokenProvider.validateToken(jwt)) {
                    UsernamePasswordAuthenticationToken authentication =
                        buildAuthentication(request, jwt);

                    SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException exception) {
                logger.error("Authentication failed: invalid token", exception);
            } catch (UsernameNotFoundException exception) {
                logger.error("Authentication failed: user not found", exception);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String buildJwt(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }

        return DEFAULT_TOKEN;
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(HttpServletRequest request, String jwt) {
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        Collection<? extends GrantedAuthority> userAuthorities = user.getAuthorities();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, userAuthorities);
        WebAuthenticationDetails authenticationDetails =
            new WebAuthenticationDetailsSource().buildDetails(request);

        authentication.setDetails(authenticationDetails);

        return authentication;
    }
}
