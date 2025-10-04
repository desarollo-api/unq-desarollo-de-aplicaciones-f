package unq.desapp.futbol.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void testPlayerCreation() {
        Player player = new Player();
        player.setName("Lionel Messi");
        player.setAge(36);
        player.setNationality("Argentina");
        player.setPosition("Forward");
        player.setRating(9.5);
        player.setMatches(800);
        player.setGoals(700);
        player.setAssist(300);
        player.setRedCards(1);
        player.setYellowCards(50);

        assertEquals("Lionel Messi", player.getName());
        assertEquals(36, player.getAge());
        assertEquals("Argentina", player.getNationality());
        assertEquals("Forward", player.getPosition());
        assertEquals(9.5, player.getRating());
        assertEquals(800, player.getMatches());
        assertEquals(700, player.getGoals());
        assertEquals(300, player.getAssist());
        assertEquals(1, player.getRedCards());
        assertEquals(50, player.getYellowCards());
    }

}