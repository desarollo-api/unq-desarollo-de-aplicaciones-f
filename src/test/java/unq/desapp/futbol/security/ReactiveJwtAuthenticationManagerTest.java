package unq.desapp.futbol.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.UserService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReactiveJwtAuthenticationManagerTest {

    private static final String TOKEN = "jwt-token";
    private static final String USERNAME = "some.user";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserService footballService;

    private ReactiveJwtAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new ReactiveJwtAuthenticationManager(jwtTokenProvider, footballService);
    }

    @Test
    void shouldReturnAuthentication_whenTokenIsValid() {
        // Arrange - token valid and user exists
        User user = new User(USERNAME, "password", "Some", "User", Role.USER);

        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(TOKEN)).thenReturn(USERNAME);
        when(footballService.findByEmail(USERNAME)).thenReturn(Optional.of(user));

        Authentication inputAuthentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        // Act & Assert - expect authentication with same user and authorities
        StepVerifier.create(authenticationManager.authenticate(inputAuthentication))
                .expectNextMatches(authentication -> {
                    assertThat(authentication.getPrincipal()).isEqualTo(user);
                    assertThat(authentication.getAuthorities()).hasSize(1);
                    assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
                    return true;
                })
                .verifyComplete();

        verify(jwtTokenProvider).validateToken(TOKEN);
        verify(jwtTokenProvider).getUsernameFromToken(TOKEN);
        verify(footballService).findByEmail(USERNAME);
    }

    @Test
    void shouldReturnEmpty_whenTokenIsInvalid() {
        // Arrange - token fails validation
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(false);

        Authentication inputAuthentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        // Act & Assert - expect empty mono
        StepVerifier.create(authenticationManager.authenticate(inputAuthentication))
                .verifyComplete();

        verify(jwtTokenProvider).validateToken(TOKEN);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verifyNoInteractions(footballService);
    }

    @Test
    void shouldReturnEmpty_whenUserNotFound() {
        // Arrange - valid token but user lookup empty
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(TOKEN)).thenReturn(USERNAME);
        when(footballService.findByEmail(USERNAME)).thenReturn(Optional.empty());

        Authentication inputAuthentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        // Act & Assert - expect empty mono when user not found
        StepVerifier.create(authenticationManager.authenticate(inputAuthentication))
                .verifyComplete();

        verify(jwtTokenProvider).validateToken(TOKEN);
        verify(jwtTokenProvider).getUsernameFromToken(TOKEN);
        verify(footballService).findByEmail(USERNAME);
    }
}
