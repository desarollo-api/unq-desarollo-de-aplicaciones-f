package unq.desapp.futbol.webservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.service.FootballDataService;

import java.util.List;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final FootballDataService footballDataService;

    public TeamController(FootballDataService footballDataService) {
        this.footballDataService = footballDataService;
    }

    @GetMapping("/{country}/{name}/squad")
    public Mono<ResponseEntity<List<Player>>> getSquadFromScraping(@PathVariable String country, @PathVariable String name) {
        String teamName = name.replace('-', ' ');
        return footballDataService.getTeamSquad(teamName, country)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}