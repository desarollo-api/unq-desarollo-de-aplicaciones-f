package unq.desapp.futbol.model;

public record AveragePerformance(
        int appearances,
        int goals,
        int assists,
        double rating,
        double performanceScore) {
}