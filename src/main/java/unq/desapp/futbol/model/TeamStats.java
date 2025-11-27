package unq.desapp.futbol.model;

public class TeamStats {

    private String teamName;
    private String country;
    private int squadSize;
    private double averageAge;
    private double averageRating;
    private int totalGoals;
    private int totalAssists;

    public TeamStats(String teamName, String country) {
        this.teamName = teamName;
        this.country = country;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getCountry() {
        return country;
    }

    public int getSquadSize() {
        return squadSize;
    }

    public void setSquadSize(int squadSize) {
        this.squadSize = squadSize;
    }

    public double getAverageAge() {
        return averageAge;
    }

    public void setAverageAge(double averageAge) {
        this.averageAge = averageAge;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalGoals() {
        return totalGoals;
    }

    public void setTotalGoals(int totalGoals) {
        this.totalGoals = totalGoals;
    }

    public int getTotalAssists() {
        return totalAssists;
    }

    public void setTotalAssists(int totalAssists) {
        this.totalAssists = totalAssists;
    }
}
