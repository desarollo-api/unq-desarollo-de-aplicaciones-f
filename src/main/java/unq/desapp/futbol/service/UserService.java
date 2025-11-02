package unq.desapp.futbol.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unq.desapp.futbol.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UserService {

    private final List<User> users = new CopyOnWriteArrayList<>();
    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
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
        User newUser = new User(
                user.getEmail(),
                passwordEncoder.encode(user.getPassword()),
                user.getFirstName(),
                user.getLastName(),
                user.getRole());
        users.add(newUser);
        return newUser;
    }
}
