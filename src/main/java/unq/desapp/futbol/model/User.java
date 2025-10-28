package unq.desapp.futbol.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@NoArgsConstructor
public class User implements UserDetails {
    private static final String ROLE_PREFIX = "ROLE_";

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Role role;
    private List<SearchHistoryEntry> searchHistory = new ArrayList<>(); // Initialized by Builder.Default

    // Custom constructor to maintain compatibility with existing tests and
    // AuthController.register
    // This constructor will initialize searchHistory to an empty ArrayList
    public User(String email, String password, String firstName, String lastName, Role role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.searchHistory = new ArrayList<>(); // Ensure it's always initialized
    }

    public void addSearchHistory(String query) {
        SearchHistoryEntry entry = new SearchHistoryEntry(query);
        this.searchHistory.add(entry);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
