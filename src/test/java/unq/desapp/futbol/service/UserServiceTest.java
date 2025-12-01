package unq.desapp.futbol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.impl.UserServiceImpl;
import unq.desapp.futbol.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
@Tag("unit")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("getAllUsers() should return all users from repository")
    void getAllUsers_returnsAllUsers() {
        // Arrange
        User user1 = new User("user1@example.com", "pass1", "User", "One", Role.USER);
        User user2 = new User("user2@example.com", "pass2", "User", "Two", Role.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertThat(users).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsers() should return empty list when no users exist")
    void getAllUsers_returnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertThat(users).isEmpty();
        verify(userRepository).findAll();
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {
        @Test
        @DisplayName("should return user when it exists (case-insensitive)")
        void findByEmail_isCaseInsensitive() {
            // Arrange
            User user = new User("user@example.com", "encoded-password", "Test", "User", Role.USER);
            when(userRepository.findByEmailIgnoreCase("USER@EXAMPLE.COM")).thenReturn(Optional.of(user));

            // Act
            Optional<User> foundUser = userService.findByEmail("USER@EXAMPLE.COM");

            // Assert
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("user@example.com");
            verify(userRepository).findByEmailIgnoreCase("USER@EXAMPLE.COM");
        }

        @Test
        @DisplayName("should return empty when user does not exist")
        void findByEmail_whenUserDoesNotExist_returnsEmpty() {
            // Arrange
            when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

            // Act
            Optional<User> foundUser = userService.findByEmail("nonexistent@example.com");

            // Assert
            assertThat(foundUser).isNotPresent();
            verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("loginUser()")
    class LoginUser {
        @Test
        @DisplayName("should return user with valid credentials")
        void loginUser_withValidCredentials_returnsUser() {
            // Arrange
            User user = new User("user@example.com", "encoded-password", "Test", "User", Role.USER);
            when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

            // Act
            Optional<User> loggedInUser = userService.loginUser("user@example.com", "password");

            // Assert
            assertThat(loggedInUser).isPresent();
            assertThat(loggedInUser.get().getEmail()).isEqualTo("user@example.com");
            verify(userRepository).findByEmailIgnoreCase("user@example.com");
            verify(passwordEncoder).matches("password", "encoded-password");
        }

        @Test
        @DisplayName("should return empty with invalid password")
        void loginUser_withInvalidPassword_returnsEmpty() {
            // Arrange
            User user = new User("user@example.com", "encoded-password", "Test", "User", Role.USER);
            when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

            // Act
            Optional<User> loggedInUser = userService.loginUser("user@example.com", "wrong-password");

            // Assert
            assertThat(loggedInUser).isNotPresent();
            verify(userRepository).findByEmailIgnoreCase("user@example.com");
            verify(passwordEncoder).matches("wrong-password", "encoded-password");
        }

        @Test
        @DisplayName("should return empty when user does not exist")
        void loginUser_whenUserDoesNotExist_returnsEmpty() {
            // Arrange
            when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

            // Act
            Optional<User> loggedInUser = userService.loginUser("nonexistent@example.com", "password");

            // Assert
            assertThat(loggedInUser).isNotPresent();
            verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        }
    }

    @Nested
    @DisplayName("addUser()")
    class AddUser {
        @Test
        @DisplayName("should add a new user and encode password")
        void addUser_success() {
            // Arrange
            User newUser = new User("new@example.com", "newpass", "New", "User", Role.USER);
            User savedUser = new User("new@example.com", "encoded-newpass", "New", "User", Role.USER);
            when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // Act
            User addedUser = userService.addUser(newUser);

            // Assert
            assertThat(addedUser.getEmail()).isEqualTo("new@example.com");
            assertThat(addedUser.getPassword()).isEqualTo("encoded-newpass");
            verify(passwordEncoder).encode("newpass");
            verify(userRepository).findByEmailIgnoreCase("new@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw exception if email is already taken")
        void addUser_whenEmailExists_throwsException() {
            // Arrange
            User existingUser = new User("user@example.com", "pass", "Name", "Last", Role.USER);
            when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existingUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.addUser(existingUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is already taken: user@example.com");
            verify(userRepository).findByEmailIgnoreCase("user@example.com");
        }
    }
}
