package unq.desapp.futbol.model;

public record SeasonPerformance(
        String season,
        String team,
        String competition,
        int appearances,
        int goals,
        int assists,
        double rating) {
}
