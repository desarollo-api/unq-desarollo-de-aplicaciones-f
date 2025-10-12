package unq.desapp.futbol.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FootballService {

    private final List<User> users = new CopyOnWriteArrayList<>();
    private final PasswordEncoder passwordEncoder;

    public FootballService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        users.add(new User(
                "user@example.com",
                this.passwordEncoder.encode("password"),
                "John",
                "User",
                Role.USER
        ));
        users.add(new User(
                "admin@example.com",
                this.passwordEncoder.encode("adminpass"),
                "Jane",
                "Admin",
                Role.ADMIN
        ));
    }

    public List<User> getAllUsers() {
        return Collections.unmodifiableList(users);
    }

    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Optional<User> loginUser(String email, String password) {
        return findByEmail(email)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    public User addUser(User user) {
        if (findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already taken: " + user.getEmail());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        users.add(user);
        return user;
    }
}
