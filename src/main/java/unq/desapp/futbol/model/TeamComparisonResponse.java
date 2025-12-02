package unq.desapp.futbol.model;

public class TeamComparisonResponse {

    private TeamComparisonDetails teamA;
    private TeamComparisonDetails teamB;
    private String verdict;

    public TeamComparisonResponse(TeamComparisonDetails teamA, TeamComparisonDetails teamB, String verdict) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.verdict = verdict;
    }

    public TeamComparisonDetails getTeamA() {
        return teamA;
    }

    public TeamComparisonDetails getTeamB() {
        return teamB;
    }

    public String getVerdict() {
        return verdict;
    }
}
