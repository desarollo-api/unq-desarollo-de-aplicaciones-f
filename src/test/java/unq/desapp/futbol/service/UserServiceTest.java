package unq.desapp.futbol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.User;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(passwordEncoder);
    }

    @Test
    @DisplayName("Constructor should initialize with an empty user list")
    void constructor_initializesWithEmptyList() {
        List<User> users = userService.getAllUsers();
        assertThat(users).isEmpty();
    }

    @Test
    @DisplayName("getAllUsers() should return an unmodifiable list")
    void getAllUsers_returnsUnmodifiableList() {        
        List<User> users = userService.getAllUsers();
        assertThatThrownBy(users::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {
        @Test
        @DisplayName("should return user when it exists (case-insensitive)")
        void findByEmail_isCaseInsensitive() {
            // Arrange
            User user = new User("user@example.com", "encoded-password", "Test", "User", Role.USER);
            userService.addUser(user);

            // Act
            Optional<User> foundUser = userService.findByEmail("USER@EXAMPLE.COM");
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should return empty when user does not exist")
        void findByEmail_whenUserDoesNotExist_returnsEmpty() {            
            Optional<User> foundUser = userService.findByEmail("nonexistent@example.com");
            assertThat(foundUser).isNotPresent();
        }
    }

    @Nested
    @DisplayName("loginUser()")
    class LoginUser {
        @Test
        @DisplayName("should return user with valid credentials")
        void loginUser_withValidCredentials_returnsUser() {
            User user = new User("user@example.com", "password", "Test", "User", Role.USER);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            userService.addUser(user);
            when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

            Optional<User> loggedInUser = userService.loginUser("user@example.com", "password");
            assertThat(loggedInUser).isPresent();
            assertThat(loggedInUser.get().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should return empty with invalid password")
        void loginUser_withInvalidPassword_returnsEmpty() {
            User user = new User("user@example.com", "password", "Test", "User", Role.USER);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            userService.addUser(user);
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
            Optional<User> loggedInUser = userService.loginUser("user@example.com", "wrong-password");
            assertThat(loggedInUser).isNotPresent();
        }
    }

    @Nested
    @DisplayName("addUser()")
    class AddUser {
        @Test
        @DisplayName("should add a new user and encode password")
        void addUser_success() {
            User newUser = new User("new@example.com", "newpass", "New", "User", Role.USER);
            when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");

            User addedUser = userService.addUser(newUser);

            assertThat(addedUser.getEmail()).isEqualTo("new@example.com");
            assertThat(addedUser.getPassword()).isEqualTo("encoded-newpass");
            verify(passwordEncoder).encode("newpass");
            assertThat(userService.getAllUsers()).hasSize(1);
        }

        @Test
        @DisplayName("should throw exception if email is already taken")
        void addUser_whenEmailExists_throwsException() {
            // Arrange
            User existingUser = new User("user@example.com", "pass", "Name", "Last", Role.USER);
            userService.addUser(existingUser);

            // Act & Assert
            assertThatThrownBy(() -> userService.addUser(existingUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email is already taken: user@example.com");
        }
    }
}
