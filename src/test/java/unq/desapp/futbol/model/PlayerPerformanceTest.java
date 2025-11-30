package unq.desapp.futbol.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("unit")
class PlayerPerformanceTest {

    @Test
    void testPlayerPerformanceCreationWithSeasons() {
        List<SeasonPerformance> seasons = new ArrayList<>();
        seasons.add(new SeasonPerformance("2023", "Team A", "Comp A", 10, 5, 2, 7.0));
        seasons.add(new SeasonPerformance("2022", "Team B", "Comp B", 20, 10, 5, 8.0));

        PlayerPerformance performance = new PlayerPerformance("Player 1", seasons);

        assertEquals("Player 1", performance.getName());
        assertEquals(2, performance.getSeasons().size());
        assertNotNull(performance.getAverage());
    }

    @Test
    void testPlayerPerformanceSortsAndLimitsSeasons() {
        List<SeasonPerformance> seasons = new ArrayList<>();
        seasons.add(new SeasonPerformance("2020", "Team A", "Comp A", 10, 1, 1, 6.0));
        seasons.add(new SeasonPerformance("2021", "Team A", "Comp A", 10, 1, 1, 6.0));
        seasons.add(new SeasonPerformance("2022", "Team A", "Comp A", 10, 1, 1, 6.0));
        seasons.add(new SeasonPerformance("2023", "Team A", "Comp A", 10, 1, 1, 6.0));
        seasons.add(new SeasonPerformance("2024", "Team A", "Comp A", 10, 1, 1, 6.0));
        seasons.add(new SeasonPerformance("2019", "Team A", "Comp A", 10, 1, 1, 6.0)); // Should be excluded

        PlayerPerformance performance = new PlayerPerformance("Player 1", seasons);

        assertEquals(5, performance.getSeasons().size());
        assertEquals("2024", performance.getSeasons().get(0).season());
        assertEquals("2020", performance.getSeasons().get(4).season());
    }

    @Test
    void testCalculateAverage() {
        List<SeasonPerformance> seasons = new ArrayList<>();
        // Season 1: 10 apps, 2 goals, 1 assist, 7.0 rating. Weighted rating: 70
        seasons.add(new SeasonPerformance("2023", "Team A", "Comp A", 10, 2, 1, 7.0));
        // Season 2: 10 apps, 4 goals, 3 assists, 8.0 rating. Weighted rating: 80
        seasons.add(new SeasonPerformance("2022", "Team A", "Comp A", 10, 4, 3, 8.0));

        // Total apps: 20. Avg apps: 10
        // Total goals: 6. Avg goals: 3
        // Total assists: 4. Avg assists: 2
        // Total weighted rating: 150. Avg rating: 150 / 20 = 7.5

        // Performance Score: (3 * 1.5 + 2 * 1.2 + 7.5 * 10) / 3.0
        // (4.5 + 2.4 + 75) / 3.0 = 81.9 / 3.0 = 27.3

        PlayerPerformance performance = new PlayerPerformance("Player 1", seasons);
        AveragePerformance avg = performance.getAverage();

        assertEquals(10, avg.appearances());
        assertEquals(3, avg.goals());
        assertEquals(2, avg.assists());
        assertEquals(7.5, avg.rating(), 0.01);
        assertEquals(27.3, avg.performanceScore(), 0.01);
    }

    @Test
    void testCalculateAverageWithEmptySeasons() {
        PlayerPerformance performance = new PlayerPerformance("Player 1", new ArrayList<>());
        AveragePerformance avg = performance.getAverage();

        assertEquals(0, avg.appearances());
        assertEquals(0, avg.goals());
        assertEquals(0, avg.assists());
        assertEquals(0.0, avg.rating());
        assertEquals(0.0, avg.performanceScore());
    }

    @Test
    void testCalculateAverageWithZeroApps() {
        List<SeasonPerformance> seasons = new ArrayList<>();
        seasons.add(new SeasonPerformance("2023", "Team A", "Comp A", 0, 0, 0, 0.0));

        PlayerPerformance performance = new PlayerPerformance("Player 1", seasons);
        AveragePerformance avg = performance.getAverage();

        assertEquals(0, avg.appearances());
        assertEquals(0.0, avg.rating());
    }
}
