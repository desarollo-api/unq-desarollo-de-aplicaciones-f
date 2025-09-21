package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;

import java.util.List;

public interface ScrapingService {
    Mono<List<Player>> getTeamSquad(String teamName, String country);
}
