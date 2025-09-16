package unq.desapp.futbol.webservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Team;
import unq.desapp.futbol.model.TeamDetails;
import unq.desapp.futbol.service.FootballDataService;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final FootballDataService footballDataService;

    public TeamController(FootballDataService footballDataService) {
        this.footballDataService = footballDataService;
    }

    @GetMapping
    public Flux<Team> getTeams() {
        return footballDataService.getTeams();
    }

    @GetMapping("/{teamId}")
    public Mono<ResponseEntity<TeamDetails>> getTeamById(@PathVariable Long teamId) {
        return footballDataService.getTeamById(teamId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}