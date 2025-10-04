package unq.desapp.futbol.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveUserPasswordAuthenticationManagerTest {

    private static final String USERNAME = "lionel";
    private static final String PASSWORD = "worldcup";
    private static final String ENCODED_PASSWORD = "encoded-worldcup";
    private static final List<GrantedAuthority> AUTHORITIES =
        List.of(new SimpleGrantedAuthority("ROLE_USER"));

    @Mock
    private ReactiveUserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ReactiveUserPasswordAuthenticationManager authenticationManager;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.withUsername(USERNAME)
            .password(ENCODED_PASSWORD)
            .authorities(AUTHORITIES)
            .build();
    }

    @Test
    void shouldAuthenticate_whenCredentialsAreValid() {
        // Verifies that a valid username/password produces an authenticated token.
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(PASSWORD, userDetails.getPassword())).thenReturn(true);

        Authentication request = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);

        StepVerifier.create(authenticationManager.authenticate(request))
            .assertNext(authentication -> {
                assertThat(authentication)
                    .isInstanceOf(UsernamePasswordAuthenticationToken.class)
                    .extracting(Authentication::isAuthenticated)
                    .isEqualTo(true);
                assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
                assertThat(authentication.getCredentials()).isEqualTo(PASSWORD);
                assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            })
            .verifyComplete();

        verify(userDetailsService).findByUsername(USERNAME);
        verify(passwordEncoder).matches(PASSWORD, userDetails.getPassword());
    }

    @Test
    void shouldFailAuthentication_whenPasswordDoesNotMatch() {
        // Ensures password mismatch yields BadCredentialsException.
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(PASSWORD, userDetails.getPassword())).thenReturn(false);

        Authentication request = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);

        StepVerifier.create(authenticationManager.authenticate(request))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BadCredentialsException.class);
                assertThat(error).hasMessage("Invalid credentials");
            })
            .verify();

        verify(userDetailsService).findByUsername(USERNAME);
        verify(passwordEncoder).matches(PASSWORD, userDetails.getPassword());
    }

    @Test
    void shouldFailAuthentication_whenUserDoesNotExist() {
        // Confirms missing user triggers the same BadCredentialsException.
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.empty());

        Authentication request = new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD);

        StepVerifier.create(authenticationManager.authenticate(request))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(BadCredentialsException.class);
                assertThat(error).hasMessage("Invalid credentials");
            })
            .verify();

        verify(userDetailsService).findByUsername(USERNAME);
        verifyNoInteractions(passwordEncoder);
    }
}
