package unq.desapp.futbol.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import unq.desapp.futbol.model.Role;
import unq.desapp.futbol.model.SearchHistoryEntry;
import unq.desapp.futbol.model.SearchType;
import unq.desapp.futbol.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Integration Tests")
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@user.com", "password", "Test", "User", Role.USER);
    }

    @Nested
    @DisplayName("getSearchHistory")
    class GetSearchHistory {

        @Test
        @DisplayName("should return OK with full history when no type is provided")
        void getHistory_noType_returnsFullHistory() {
            // Arrange
            testUser.addSearchHistory(SearchType.PLAYER, "lionel messi");
            testUser.addSearchHistory(SearchType.TEAM, "boca juniors");

            // Act
            Mono<ResponseEntity<List<SearchHistoryEntry>>> result = userController.getSearchHistory(testUser, null);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotNull().hasSize(2);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return OK with only PLAYER history when filtered by PLAYER")
        void getHistory_filteredByPlayer_returnsOnlyPlayerEntries() {
            // Arrange
            testUser.addSearchHistory(SearchType.PLAYER, "lionel messi");
            testUser.addSearchHistory(SearchType.TEAM, "boca juniors");

            // Act
            Mono<ResponseEntity<List<SearchHistoryEntry>>> result = userController.getSearchHistory(testUser,
                    SearchType.PLAYER);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotNull().hasSize(1)
                                .first()
                                .extracting(SearchHistoryEntry::getType, SearchHistoryEntry::getQuery)
                                .containsExactly(SearchType.PLAYER, "lionel messi");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return OK with only TEAM history when filtered by TEAM")
        void getHistory_filteredByTeam_returnsOnlyTeamEntries() {
            // Arrange
            testUser.addSearchHistory(SearchType.PLAYER, "lionel messi");
            testUser.addSearchHistory(SearchType.TEAM, "boca juniors");

            // Act
            Mono<ResponseEntity<List<SearchHistoryEntry>>> result = userController.getSearchHistory(testUser,
                    SearchType.TEAM);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotNull().hasSize(1)
                                .first()
                                .extracting(SearchHistoryEntry::getType).isEqualTo(SearchType.TEAM);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return OK with an empty list for a user with no history")
        void getHistory_withNoHistory_returnsEmptyList() {
            // Act
            Mono<ResponseEntity<List<SearchHistoryEntry>>> result = userController.getSearchHistory(testUser, null);

            // Assert
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        assertThat(response.getBody()).isNotNull().isEmpty();
                    })
                    .verifyComplete();
        }
    }
}
