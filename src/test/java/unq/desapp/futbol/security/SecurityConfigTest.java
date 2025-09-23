package unq.desapp.futbol.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "app.security.jwt.secret-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.security.jwt.expiration=3600000"
    }
)
@ActiveProfiles("dev")
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    void contextLoads_securityBeansCreated() {
        // Assert
        assertNotNull(securityFilterChain);
        assertNotNull(passwordEncoder);
        assertNotNull(corsConfigurationSource);
        assertNotNull(authenticationManager);
    }

    @Test
    void passwordEncoder_isBCrypt() {
        // Arrange & Act
        String encoded = passwordEncoder.encode("secret");

        // Assert
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        assertTrue(passwordEncoder.matches("secret", encoded));
    }

    @Test
    void corsConfiguration_isConfiguredAsExpected() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any/path");

        // Act
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(request);

        // Assert
        assertNotNull(corsConfiguration);
        assertEquals(Constants.Cors.ALL_ALLOWED, corsConfiguration.getAllowedOriginPatterns());
        assertEquals(Constants.Cors.ALLOWED_METHODS, corsConfiguration.getAllowedMethods());
        assertEquals(Constants.Cors.ALL_ALLOWED, corsConfiguration.getAllowedHeaders());
        assertEquals(Constants.Cors.EXPOSED_HEADERS, corsConfiguration.getExposedHeaders());
        assertFalse(corsConfiguration.getAllowCredentials());
    }

    @Test
    void filterChain_containsJwtAuthenticationFilter() {
        // Arrange & Act
        DefaultSecurityFilterChain chain = (DefaultSecurityFilterChain) securityFilterChain;
        List<Filter> filters = chain.getFilters();

        // Assert
        assertTrue(filters.contains(jwtAuthenticationFilter));
    }
}
