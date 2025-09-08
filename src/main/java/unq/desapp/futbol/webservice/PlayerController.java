package unq.desapp.futbol.webservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.service.FootballDataService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final FootballDataService footballDataService;

    public PlayerController(FootballDataService footballDataService) {
        this.footballDataService = footballDataService;
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Player>> getPlayerById(@PathVariable Long id) {
        return footballDataService.getPlayerById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
