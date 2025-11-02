package unq.desapp.futbol.model;

import lombok.Getter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PlayerPerformance {
    private String name;
    private AveragePerformance average;
    private List<SeasonPerformance> seasons;

    public PlayerPerformance(String name, List<SeasonPerformance> seasons) {
        this.name = name;
        this.seasons = seasons.stream()
                .sorted(Comparator.comparing(SeasonPerformance::season).reversed())
                .limit(5)
                .collect(Collectors.toList());
        this.average = calculateAverage(this.seasons);
    }

    private AveragePerformance calculateAverage(List<SeasonPerformance> lastFive) {
        if (lastFive == null || lastFive.isEmpty()) {
            return new AveragePerformance(0, 0, 0, 0.0, 0.0);
        }

        double totalApps = 0;
        double totalGoals = 0;
        double totalAssists = 0;
        double totalWeightedRating = 0;

        for (SeasonPerformance s : lastFive) {
            totalApps += s.appearances();
            totalGoals += s.goals();
            totalAssists += s.assists();
            totalWeightedRating += s.rating() * s.appearances();
        }

        double avgApps = totalApps / lastFive.size();
        double avgGoals = totalGoals / lastFive.size();
        double avgAssists = totalAssists / lastFive.size();
        double avgRating = (totalApps > 0) ? totalWeightedRating / totalApps : 0.0;

        double performanceScore = calculatePerformanceScore(avgGoals, avgAssists, avgRating);

        return new AveragePerformance(
                (int) Math.round(avgApps),
                (int) Math.round(avgGoals),
                (int) Math.round(avgAssists),
                Math.round(avgRating * 100.0) / 100.0,
                Math.round(performanceScore * 100.0) / 100.0);
    }

    private double calculatePerformanceScore(double goals, double assists, double rating) {
        return (goals * 1.5 + assists * 1.2 + rating * 10) / 3.0;
    }
}