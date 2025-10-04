package unq.desapp.futbol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;

class CustomReactiveUserDetailsServiceTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    CustomReactiveUserDetailsService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldReturnUserDetailsWhenUsernameExists() {
        StepVerifier.create(service.findByUsername("user@example.com"))
                .expectNextMatches(userDetails -> {
                    assertThat(userDetails).isInstanceOf(User.class);
                    User user = (User) userDetails;
                    assertThat(user.getUsername()).isEqualTo("user@example.com");
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnAdminDetailsWhenUsernameExists() {
        StepVerifier.create(service.findByUsername("admin@example.com"))
                .expectNextMatches(userDetails -> {
                    assertThat(userDetails).isInstanceOf(User.class);
                    User user = (User) userDetails;
                    assertThat(user.getUsername()).isEqualTo("admin@example.com");
                    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldEmitErrorWhenUsernameDoesNotExist() {
        StepVerifier.create(service.findByUsername("unknown@example.com"))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }

    @Test
    void shouldEncodePasswordsDuringInitialization() {
        verify(passwordEncoder, times(2)).encode(anyString());
        verify(passwordEncoder).encode("password");
        verify(passwordEncoder).encode("adminpass");
    }
}
