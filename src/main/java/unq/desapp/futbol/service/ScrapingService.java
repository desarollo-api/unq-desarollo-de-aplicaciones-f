package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;

public interface ScrapingService {
    Mono<String> findTeamPage(String teamName, String country);
}
