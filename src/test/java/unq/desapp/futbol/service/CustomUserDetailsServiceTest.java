package unq.desapp.futbol.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Test
    void loadUserByUsername_givenExistingUser_shouldReturnUserDetails() {
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        String existingUsername = "user@example.com";

        CustomUserDetailsService service = new CustomUserDetailsService(passwordEncoder);
        UserDetails user = service.loadUserByUsername(existingUsername);

        assertNotNull(user);
        assertEquals(existingUsername, user.getUsername());
    }

    @Test
    void loadUserByUsername_givenNonExistingUser_shouldThrowUsernameNotFoundException() {
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        String nonExistingUsername = "nonexistent@example.com";

        CustomUserDetailsService service = new CustomUserDetailsService(passwordEncoder);
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> service.loadUserByUsername(nonExistingUsername));

        assertEquals("User not found with email: " + nonExistingUsername, exception.getMessage());
    }
}
