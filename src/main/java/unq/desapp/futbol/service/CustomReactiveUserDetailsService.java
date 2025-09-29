package unq.desapp.futbol.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;

@Service
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {
    private final List<User> users = new ArrayList<>();

    public CustomReactiveUserDetailsService(PasswordEncoder passwordEncoder) {
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
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.justOrEmpty(
            users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .map(user -> (UserDetails) user)
        ).switchIfEmpty(
            Mono.error(new UsernameNotFoundException("User not found with email: " + username))
        );
    }
}
