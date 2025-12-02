package unq.desapp.futbol.service;

import java.util.List;
import java.util.Optional;
import unq.desapp.futbol.model.User;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> findByEmail(String email);

    Optional<User> loginUser(String email, String password);

    User addUser(User user);

    User saveUser(User user);
}
