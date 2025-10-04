package unq.desapp.futbol.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveJwtAuthenticationManagerTest {

    private static final String TOKEN = "jwt-token";
    private static final String USERNAME = "some.user";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ReactiveUserDetailsService userDetailsService;

    private ReactiveJwtAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new ReactiveJwtAuthenticationManager(jwtTokenProvider, userDetailsService);
    }

    @Test
    void shouldReturnAuthentication_whenTokenIsValid() {
        // Arrange - token valid and user exists
        UserDetails userDetails = User.withUsername(USERNAME)
                .password("password")
                .authorities("ROLE_USER")
                .build();

        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(userDetails));

        Authentication inputAuthentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        // Act & Assert - expect authentication with same user and authorities
        StepVerifier.create(authenticationManager.authenticate(inputAuthentication))
                .expectNextMatches(authentication -> {
                    assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
                    assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
                    assertThat(authentication.getCredentials()).isNull();
                    List<String> actualAuthorities = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList();
                    List<String> expectedAuthorities = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList();
                    assertThat(actualAuthorities)
                            .containsExactlyInAnyOrderElementsOf(expectedAuthorities);
                    return true;
                })
                .verifyComplete();

        verify(jwtTokenProvider).validateToken(TOKEN);
        verify(jwtTokenProvider).getUsernameFromToken(TOKEN);
        verify(userDetailsService).findByUsername(USERNAME);
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
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldReturnEmpty_whenUserNotFound() {
        // Arrange - valid token but user lookup empty
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.empty());

        Authentication inputAuthentication = new UsernamePasswordAuthenticationToken(null, TOKEN);

        // Act & Assert - expect empty mono when user not found
        StepVerifier.create(authenticationManager.authenticate(inputAuthentication))
                .verifyComplete();

        verify(jwtTokenProvider).validateToken(TOKEN);
        verify(jwtTokenProvider).getUsernameFromToken(TOKEN);
        verify(userDetailsService).findByUsername(USERNAME);
    }
}
