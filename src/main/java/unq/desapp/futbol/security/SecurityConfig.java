package unq.desapp.futbol.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import unq.desapp.futbol.constants.AuthenticationManager;
import unq.desapp.futbol.security.Constants.Auth;
import unq.desapp.futbol.security.Constants.Cors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!test")
    public SecurityWebFilterChain securityWebFilterChain(
        ServerHttpSecurity http,
        @Qualifier(AuthenticationManager.JWT) ReactiveAuthenticationManager authenticationManager,
        ReactiveJwtAuthenticationConverter authenticationConverter,
        ReactiveAuthenticationEntryPoint authenticationEntryPoint) {
        AuthenticationWebFilter authenticationWebFilter =
            buildAuthenticationWebFilter(
                authenticationManager,
                authenticationConverter,
                authenticationEntryPoint);
        CorsConfigurationSource corsConfigurationSource =
            buildCorsConfigurationSource();

        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeExchange(authorizeExchange -> authorizeExchange
                .pathMatchers(Auth.PATTERN)
                .permitAll()
                .anyExchange()
                .authenticated())
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint))
            .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    private AuthenticationWebFilter buildAuthenticationWebFilter(
        ReactiveAuthenticationManager authenticationManager,
        ReactiveJwtAuthenticationConverter authenticationConverter,
        ReactiveAuthenticationEntryPoint authenticationEntryPoint) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);

        filter.setServerAuthenticationConverter(authenticationConverter);
        filter.setAuthenticationFailureHandler(
            new ServerAuthenticationEntryPointFailureHandler(authenticationEntryPoint));

        return filter;
    }

    @Bean
    public CorsConfigurationSource buildCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Cors.ALL_ALLOWED);
        configuration.setAllowedMethods(Cors.ALLOWED_METHODS);
        configuration.setAllowedHeaders(Cors.ALL_ALLOWED);
        configuration.setExposedHeaders(Cors.EXPOSED_HEADERS);
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(Cors.PATTERN, configuration);

        return source;
    }
}
