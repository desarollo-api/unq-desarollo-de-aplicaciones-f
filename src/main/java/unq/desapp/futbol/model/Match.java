package unq.desapp.futbol.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    private String date;
    private String competition;
    private String homeTeam;
    private String awayTeam;
}
