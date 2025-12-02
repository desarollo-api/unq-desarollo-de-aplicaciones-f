package unq.desapp.futbol.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;

@DataJpaTest
@Tag("e2e")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should save and retrieve user by ID")
    void shouldSaveAndRetrieveUser() {
        // Arrange
        User user = new User("test@example.com", "password123", "John", "Doe", Role.USER);

        // Act
        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getFirstName()).isEqualTo("John");
        assertThat(foundUser.get().getLastName()).isEqualTo("Doe");
        assertThat(foundUser.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("should find user by email (case-insensitive)")
    void shouldFindByEmailIgnoreCase() {
        // Arrange
        User user = new User("Test@Example.COM", "password123", "Jane", "Smith", Role.ADMIN);
        entityManager.persist(user);
        entityManager.flush();

        // Act
        Optional<User> foundByLowerCase = userRepository.findByEmailIgnoreCase("test@example.com");
        Optional<User> foundByUpperCase = userRepository.findByEmailIgnoreCase("TEST@EXAMPLE.COM");
        Optional<User> foundByMixedCase = userRepository.findByEmailIgnoreCase("TeSt@ExAmPlE.cOm");

        // Assert
        assertThat(foundByLowerCase).isPresent();
        assertThat(foundByUpperCase).isPresent();
        assertThat(foundByMixedCase).isPresent();
        assertThat(foundByLowerCase.get().getEmail()).isEqualTo("Test@Example.COM");
        assertThat(foundByUpperCase.get().getId()).isEqualTo(foundByLowerCase.get().getId());
        assertThat(foundByMixedCase.get().getId()).isEqualTo(foundByLowerCase.get().getId());
    }

    @Test
    @DisplayName("should return empty when email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // Act
        Optional<User> foundUser = userRepository.findByEmailIgnoreCase("nonexistent@example.com");

        // Assert
        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("should retrieve all users")
    void shouldRetrieveAllUsers() {
        // Arrange
        User user1 = new User("user1@example.com", "pass1", "User", "One", Role.USER);
        User user2 = new User("user2@example.com", "pass2", "User", "Two", Role.ADMIN);
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // Act
        List<User> allUsers = userRepository.findAll();

        // Assert
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    @DisplayName("should delete user by ID")
    void shouldDeleteUser() {
        // Arrange
        User user = new User("delete@example.com", "password", "Delete", "Me", Role.USER);
        User savedUser = entityManager.persist(user);
        entityManager.flush();
        Long userId = savedUser.getId();

        // Act
        userRepository.deleteById(userId);
        Optional<User> foundUser = userRepository.findById(userId);

        // Assert
        assertThat(foundUser).isNotPresent();
    }

    @Test
    @DisplayName("should enforce unique email constraint")
    void shouldEnforceUniqueEmail() {
        // Arrange
        User user1 = new User("unique@example.com", "pass1", "First", "User", Role.USER);
        entityManager.persist(user1);
        entityManager.flush();
        entityManager.clear();

        User user2 = new User("unique@example.com", "pass2", "Second", "User", Role.ADMIN);

        // Act & Assert
        try {
            entityManager.persist(user2);
            entityManager.flush();
            fail("Expected unique constraint violation");
        } catch (Exception e) {
            // Expected: unique constraint violation
            assertThat(e).isNotNull();
        }
    }
}
