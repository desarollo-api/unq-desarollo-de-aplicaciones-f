package unq.desapp.futbol.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("unit")
class AveragePerformanceTest {

    @Test
    void testAveragePerformanceCreation() {
        AveragePerformance avg = new AveragePerformance(10, 5, 3, 7.5, 8.2);

        assertEquals(10, avg.appearances());
        assertEquals(5, avg.goals());
        assertEquals(3, avg.assists());
        assertEquals(7.5, avg.rating());
        assertEquals(8.2, avg.performanceScore());
    }
}
