package unq.desapp.futbol.model;

public class TeamComparisonDetails {

    private String teamName;
    private String country;
    private String averageAge;
    private String averageRating;
    private String winRate;
    private String bestPlayer;

    public TeamComparisonDetails(String teamName, String country) {
        this.teamName = teamName;
        this.country = country;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getCountry() {
        return country;
    }

    public String getAverageAge() {
        return averageAge;
    }

    public void setAverageAge(String averageAge) {
        this.averageAge = averageAge;
    }

    public String getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(String averageRating) {
        this.averageRating = averageRating;
    }

    public String getWinRate() {
        return winRate;
    }

    public void setWinRate(String winRate) {
        this.winRate = winRate;
    }

    public String getBestPlayer() {
        return bestPlayer;
    }

    public void setBestPlayer(String bestPlayer) {
        this.bestPlayer = bestPlayer;
    }
}
