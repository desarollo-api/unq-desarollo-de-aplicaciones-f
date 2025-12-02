package unq.desapp.futbol.controller;

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
import unq.desapp.futbol.model.TeamComparisonResponse;
import unq.desapp.futbol.model.UpcomingMatch;
import unq.desapp.futbol.model.MatchPrediction;
import unq.desapp.futbol.model.TeamStats;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.User;
import unq.desapp.futbol.service.TeamService;
import unq.desapp.futbol.config.metrics.BusinessMetric;
import java.util.List;

@RestController
@Tag(name = "Teams")
@SecurityRequirement(name = "BearerAuth")
@RequestMapping("/teams")
public class TeamController {

        private final TeamService teamService;

        public TeamController(TeamService teamService) {
                this.teamService = teamService;
        }

        @GetMapping("/{country}/{name}/squad")
        @BusinessMetric(name = "team_squad_search", help = "Counts team squad searches")
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
                return teamService.getTeamSquad(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/{country}/{name}/matches")
        @BusinessMetric(name = "team_matches_search", help = "Counts team matches searches")
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
                return teamService.getUpcomingMatches(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/{country}/{name}/prediction")
        @BusinessMetric(name = "team_prediction_search", help = "Counts team prediction searches")
        @Operation(summary = "Predict Next Match Result", description = "Predicts the outcome of the team's next match using head-to-head history and current team statistics. This action is recorded in the user's search history.")
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
                return teamService.getNextMatchPrediction(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());

        }

        @GetMapping("/{country}/{name}/stats")
        @BusinessMetric(name = "team_stats_search", help = "Counts team stats searches")
        @Operation(summary = "Get Team Statistics", description = "Returns aggregated statistics for a given team, based on its current squad and season performance. This action is recorded in the user's search history.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved team statistics", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamStats.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
        })
        public Mono<ResponseEntity<TeamStats>> getTeamStats(
                        @Parameter(description = "Country of the team", required = true, example = "England") @PathVariable String country,
                        @Parameter(description = "Name of the team, use hyphens for spaces", required = true, example = "manchester-united") @PathVariable String name,
                        @AuthenticationPrincipal User user) {

                String teamName = name.replace('-', ' ');
                return teamService.getSingleTeamStats(teamName, country, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping("/compare/{countryA}/{nameA}/vs/{countryB}/{nameB}")
        @BusinessMetric(name = "team_comparison_search", help = "Counts team comparison searches")
        @Operation(summary = "Compare two teams", description = "Provides a side-by-side comparison of two teams based on their squad statistics. This action is recorded in the user's search history.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated comparison", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TeamComparisonResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid", content = @Content),
                        @ApiResponse(responseCode = "404", description = "One or both teams not found", content = @Content)
        })
        public Mono<ResponseEntity<TeamComparisonResponse>> compareTeams(
                        @Parameter(description = "Country of the first team", required = true) @PathVariable String countryA,
                        @Parameter(description = "Name of the first team", required = true) @PathVariable String nameA,
                        @Parameter(description = "Country of the second team", required = true) @PathVariable String countryB,
                        @Parameter(description = "Name of the second team", required = true) @PathVariable String nameB,
                        @AuthenticationPrincipal User user) {

                String teamNameA = nameA.replace('-', ' ');
                String teamNameB = nameB.replace('-', ' ');
                return teamService.getTeamsComparasion(teamNameA, countryA, teamNameB, countryB, user)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }
}
