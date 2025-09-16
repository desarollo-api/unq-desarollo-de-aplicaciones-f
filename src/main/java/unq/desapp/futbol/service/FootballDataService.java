package unq.desapp.futbol.service;

import reactor.core.publisher.Flux;

import reactor.core.publisher.Mono;
import unq.desapp.futbol.model.Player;
import unq.desapp.futbol.model.Team;
import unq.desapp.futbol.model.TeamDetails;

public interface FootballDataService {
    Mono<Player> getPlayerById(Long id);
    Flux<Team> getTeams();
    Mono<TeamDetails> getTeamById(Long teamId);
}
