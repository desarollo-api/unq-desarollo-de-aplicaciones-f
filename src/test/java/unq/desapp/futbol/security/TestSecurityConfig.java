package unq.desapp.futbol.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Profile("test")
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity)
        throws Exception {
        return httpSecurity
            .csrf(csrfConfig -> csrfConfig.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sessionConfig ->
                sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requestConfig ->
                requestConfig.anyRequest().permitAll())
            .build();
    }
}
