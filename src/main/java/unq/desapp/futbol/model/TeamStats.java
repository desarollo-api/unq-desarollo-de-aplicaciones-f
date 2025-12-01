package unq.desapp.futbol.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TeamStats {

    private String teamName;
    private String country;
    private double averageAge;
    private double averageRating;
    private int wins;
    private int draws;
    private int defeats;
    private double winRate;
    private String bestPlayer;

    public TeamStats(String teamName, String country) {
        this.teamName = teamName;
        this.country = country;
    }
}