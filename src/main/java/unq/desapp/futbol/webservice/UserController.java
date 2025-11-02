package unq.desapp.futbol.webservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.List;
import unq.desapp.futbol.model.SearchHistoryEntry;
import unq.desapp.futbol.model.SearchType;
import unq.desapp.futbol.model.User;

@RestController
@Tag(name = "User")
@RequestMapping("/user")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

        @GetMapping("/history")
        @Operation(summary = "Get User Search History", description = "Returns the search history for the authenticated user.")
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search history", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SearchHistoryEntry.class))))
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content)
        public Mono<ResponseEntity<List<SearchHistoryEntry>>> getSearchHistory(@AuthenticationPrincipal User user,
                        @Parameter(description = "Filter history by type (TEAM or PLAYER). If omitted, all history is returned.", required = false) @RequestParam(required = false) SearchType type) {

                return Mono.just(user)
                                .map(User::getSearchHistory)
                                .map(history -> {
                                        if (type == null) {
                                                return history;
                                        }
                                        return history.stream()
                                                        .filter(entry -> entry.getType() == type)
                                                        .toList();
                                })
                                .map(ResponseEntity::ok);
        }
}
