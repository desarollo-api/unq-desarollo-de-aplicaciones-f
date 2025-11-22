package unq.desapp.futbol.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeasonPerformanceTest {

    @Test
    void testSeasonPerformanceCreation() {
        SeasonPerformance season = new SeasonPerformance(
                "2023/2024", "Inter Miami", "MLS", 20, 15, 10, 8.5);

        assertEquals("2023/2024", season.season());
        assertEquals("Inter Miami", season.team());
        assertEquals("MLS", season.competition());
        assertEquals(20, season.appearances());
        assertEquals(15, season.goals());
        assertEquals(10, season.assists());
        assertEquals(8.5, season.rating());
    }
}
