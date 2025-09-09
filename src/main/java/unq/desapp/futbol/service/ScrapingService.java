package unq.desapp.futbol.service;

import reactor.core.publisher.Mono;

public interface ScrapingService {
    Mono<Double> getPlayerRating(String playerName);
}
