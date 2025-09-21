package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;

public interface FootballDataService {
    Mono<String> getTeamSquad(String teamName, String country);
}
