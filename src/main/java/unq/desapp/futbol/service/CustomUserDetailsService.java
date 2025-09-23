package unq.desapp.futbol.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final List<User> users = new ArrayList<>();

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        users.add(new User(
                "user@example.com",
                passwordEncoder.encode("password"),
                "John",
                "User",
                Role.USER
        ));
        users.add(new User(
                "admin@example.com",
                passwordEncoder.encode("adminpass"),
                "Jane",
                "Admin",
                Role.ADMIN
        ));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.stream()
            .filter(user -> user.getUsername().equals(username))
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }
}
