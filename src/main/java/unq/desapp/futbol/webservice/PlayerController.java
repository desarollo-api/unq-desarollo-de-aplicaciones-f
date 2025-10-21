package unq.desapp.futbol.webservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.service.FootballDataService;

@RestController
@Tag(name = "Players")
@RequestMapping("/player")
public class PlayerController {

    private final FootballDataService footballDataService;

    public PlayerController(FootballDataService footballDataService) {
        this.footballDataService = footballDataService;
    }

    @GetMapping("/{country}/{name}")
    @Operation(
            summary = "Get Player Performance",
            description = "Returns performance for a specific player."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved player data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "Player not found", content = @Content)
    })
    public Mono<ResponseEntity<Player>> getPlayerPerformance(
            @Parameter(description = "Player's nationality", required = true, example = "Portugal")
            @PathVariable String country,
            @Parameter(description = "Name of the player, use hyphens for spaces", required = true, example = "cristiano-ronaldo")
            @PathVariable String name) {
        return Mono.just(ResponseEntity.notFound().build());
    }
}
