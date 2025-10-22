package unq.desapp.futbol.model;

import lombok.Getter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PlayerPerformance {
    private String name;
    private List<SeasonPerformance> seasons;
    private SeasonPerformance averagePerformance;

    public PlayerPerformance(String name, List<SeasonPerformance> seasons) {
        this.name = name;
        // Ordenamos las temporadas de más reciente a más antigua y tomamos las últimas 5
        this.seasons = seasons.stream()
                .sorted(Comparator.comparing(SeasonPerformance::season).reversed())
                .limit(5)
                .collect(Collectors.toList());
        this.averagePerformance = calculateAverage(this.seasons);
    }

    private SeasonPerformance calculateAverage(List<SeasonPerformance> lastFiveSeasons) {
        if (lastFiveSeasons == null || lastFiveSeasons.isEmpty()) {
            return new SeasonPerformance("Average", "-", "-", 0, 0, 0, 0.0, 0.0);
        }

        double totalAppearances = 0;
        double totalGoals = 0;
        double totalAssists = 0;
        double totalRating = 0;

        for (SeasonPerformance season : lastFiveSeasons) {
            totalAppearances += season.appearances();
            totalGoals += season.goals();
            totalAssists += season.assists();
            totalRating += season.rating() * season.appearances(); // Ponderamos el rating por partidos jugados
        }

        int numSeasons = lastFiveSeasons.size();
        double avgRating = (totalAppearances > 0) ? totalRating / totalAppearances : 0.0;

        return new SeasonPerformance("Average (Last 5)", "-", "-", (int) Math.round(totalAppearances / numSeasons),
                (int) Math.round(totalGoals / numSeasons), (int) Math.round(totalAssists / numSeasons), 0.0,
                Math.round(avgRating * 100.0) / 100.0); // Redondeamos a 2 decimales
    }
}
