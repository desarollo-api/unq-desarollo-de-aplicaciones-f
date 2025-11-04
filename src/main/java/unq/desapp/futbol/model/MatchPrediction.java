package unq.desapp.futbol.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchPrediction {
    private String homeTeam;
    private String awayTeam;
    private TeamStats homeStats;
    private TeamStats awayStats;
    private List<HeadToHeadMatch> recentMeetings;
    private String predictedResult;
}