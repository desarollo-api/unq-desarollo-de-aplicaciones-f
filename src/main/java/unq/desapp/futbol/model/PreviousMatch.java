package unq.desapp.futbol.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviousMatch {
    private String homeTeam;
    private String homeScore;
    private String awayTeam;
    private String awayScore;
}
