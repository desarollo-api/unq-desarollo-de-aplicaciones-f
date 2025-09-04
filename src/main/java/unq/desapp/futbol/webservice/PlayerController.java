package unq.desapp.futbol.webservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<Player> getPlayerById(@PathVariable Long id) {
        Player player = footballDataService.getPlayerById(id).block(); // .block() converts Mono to a blocking call
        return ResponseEntity.ok(player);
    }
}
