package unq.desapp.futbol.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.security.core.GrantedAuthority;

@Tag("unit")
class UserTest {

    @Nested
    class UserDetailsTest {
        @Test
        void getUsername_returns_email() {
            User user = new User("john@example.com", "pass", "John", "Smith", Role.USER);

            assertEquals("john@example.com", user.getUsername());
        }

        @Test
        void getAuthorities_returns_prefixed_role_USER() {
            User user = new User("u@e.com", "p", "F", "L", Role.USER);
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            assertEquals(1, authorities.size());

            GrantedAuthority authority = authorities.iterator().next();
            assertEquals("ROLE_USER", authority.getAuthority());
        }

        @Test
        void getAuthorities_returns_prefixed_role_ADMIN_and_is_unmodifiable() {
            User user = new User("a@e.com", "p", "F", "L", Role.ADMIN);
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            assertEquals(1, authorities.size());

            GrantedAuthority granted = authorities.iterator().next();
            assertEquals("ROLE_ADMIN", granted.getAuthority());

            assertThrows(UnsupportedOperationException.class, authorities::clear);
        }

        @Test
        void account_flags_are_always_true() {
            User user = new User("x@y.com", "p", "F", "L", Role.USER);

            assertTrue(user.isAccountNonExpired());
            assertTrue(user.isAccountNonLocked());
            assertTrue(user.isCredentialsNonExpired());
            assertTrue(user.isEnabled());
        }
    }

    @Nested
    class BasicTest {
        @Test
        void noArgsConstructor_setters_and_getters_work() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPassword("secret");
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setRole(Role.USER);

            assertEquals("user@example.com", user.getEmail());
            assertEquals("secret", user.getPassword());
            assertEquals("John", user.getFirstName());
            assertEquals("Doe", user.getLastName());
            assertEquals(Role.USER, user.getRole());
        }

        @Test
        void allArgsConstructor_sets_all_fields() {
            User user = new User("admin@example.com", "adminpass", "Alice", "Admin", Role.ADMIN);

            assertEquals("admin@example.com", user.getEmail());
            assertEquals("adminpass", user.getPassword());
            assertEquals("Alice", user.getFirstName());
            assertEquals("Admin", user.getLastName());
            assertEquals(Role.ADMIN, user.getRole());
        }

        @Test
        void equals_and_hashCode_respect_field_values() {
            User u1 = new User("mail@e.com", "p", "N1", "LN", Role.USER);
            User u2 = new User("mail@e.com", "p", "N1", "LN", Role.USER);
            User u3 = new User("other@e.com", "p", "N1", "LN", Role.USER);

            assertEquals(u1, u2);
            assertEquals(u1.hashCode(), u2.hashCode());
            assertNotEquals(u1, u3);
            assertNotEquals(u1.hashCode(), u3.hashCode());
            assertNotEquals(null, u1);
            assertNotEquals(u1, new Object());
        }

        @Test
        void toString_contains_relevant_fields() {
            User user = new User("ts@example.com", "pwd", "Tom", "Sawyer", Role.USER);
            String s = user.toString();

            assertTrue(s.contains("ts@example.com"));
            assertTrue(s.contains("Tom"));
            assertTrue(s.contains("Sawyer"));
            assertTrue(s.contains("USER"));
        }
    }
}
