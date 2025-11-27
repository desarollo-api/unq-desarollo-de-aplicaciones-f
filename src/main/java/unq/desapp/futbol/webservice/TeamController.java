package unq.desapp.futbol.webservice;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.TeamComparison;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.FootballDataService;
import java.util.List;

@RestController
@Tag(name = "Teams")
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/teams")
public class TeamController {

        private final FootballDataService footballDataService;

        public TeamController(FootballDataService footballDataService) {
                this.footballDataService = footballDataService;
        }

        @GetMapping("/{country}/{name}/squad")
        @Operation(summary = "Get Team Squad", description = "Returns the squad for a given team. This action is recorded in the user's search history.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the squad", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Player.class)))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
        })
        public Mono<ResponseEntity<List<Player>>> getSquadFromScraping(
                        @Parameter(description = "Country of the team", required = true, example = "England") @PathVariable String country,
                        @Parameter(description = "Name of the team, use hyphens for spaces", required = true, example = "manchester-united") @PathVariable String name,
                        @AuthenticationPrincipal User user) {

                String teamName = name.replace('-', ' ');
                return footballDataService.getTeamSquad(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/{country}/{name}/matches")
        @Operation(summary = "Get Team Upcoming Matches", description = "Returns the upcoming matches for a given team. This action is recorded in the user's search history.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the upcoming matches", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UpcomingMatch.class)))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
        })
        public Mono<ResponseEntity<List<UpcomingMatch>>> getUpcomingMatches(
                        @Parameter(description = "Country of the team", required = true, example = "England") @PathVariable String country,
                        @Parameter(description = "Name of the team, use hyphens for spaces", required = true, example = "manchester-united") @PathVariable String name,
                        @AuthenticationPrincipal User user) {

                String teamName = name.replace('-', ' ');
                return footballDataService.getUpcomingMatches(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/{country}/{name}/prediction")
        @Operation(summary = "Predict Next Match Result", description = "Predicts the outcome of the team's next match using head-to-head history and current team statistics.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated prediction", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MatchPrediction.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Team not found or no upcoming match found", content = @Content)
        })
        public Mono<ResponseEntity<MatchPrediction>> predictNextMatch(
                        @PathVariable String country,
                        @PathVariable String name,
                        @AuthenticationPrincipal User user) {

                String teamName = name.replace('-', ' ');
                return footballDataService.predictNextMatch(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/compare/{countryA}/{nameA}/vs/{countryB}/{nameB}")
        @Operation(summary = "Compare two teams", description = "Provides a side-by-side comparison of two teams based on their squad statistics. This action is recorded in the user's search history.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated comparison", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamComparison.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "One or both teams not found", content = @Content)
        })
        public Mono<ResponseEntity<TeamComparison>> compareTeams(
                        @Parameter(description = "Country of the first team", required = true) @PathVariable String countryA,
                        @Parameter(description = "Name of the first team", required = true) @PathVariable String nameA,
                        @Parameter(description = "Country of the second team", required = true) @PathVariable String countryB,
                        @Parameter(description = "Name of the second team", required = true) @PathVariable String nameB,
                        @AuthenticationPrincipal User user) {

                String teamNameA = nameA.replace('-', ' ');
                String teamNameB = nameB.replace('-', ' ');
                return footballDataService.compareTeams(teamNameA, countryA, teamNameB, countryB, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }
}
