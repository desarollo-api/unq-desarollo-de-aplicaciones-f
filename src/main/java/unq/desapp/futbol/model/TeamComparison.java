package unq.desapp.futbol.model;

public class TeamComparison {

    private TeamStats teamA;
    private TeamStats teamB;
    private String verdict;

    public TeamComparison(TeamStats teamA, TeamStats teamB, String verdict) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.verdict = verdict;
    }

    public TeamStats getTeamA() {
        return teamA;
    }

    public TeamStats getTeamB() {
        return teamB;
    }

    public String getVerdict() {
        return verdict;
    }
}
