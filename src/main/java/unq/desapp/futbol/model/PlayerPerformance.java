package unq.desapp.futbol.model;

import java.util.List;

public class PlayerPerformance {
    private String name;
    private List<Performance> seasons;

    public PlayerPerformance() {}

    public PlayerPerformance(String name, List<Performance> seasons) {
        this.name = name;
        this.seasons = seasons;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Performance> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Performance> seasons) {
        this.seasons = seasons;
    }

    public static class Performance {
        private String season;
        private String team;
        private String competition;
        private int appearances;
        private int goals;
        private int assists;
        private double aerialWons;
        private double rating;

        public String getSeason() {
            return season;
        }

        public void setSeason(String season) {
            this.season = season;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public String getCompetition() {
            return competition;
        }

        public void setCompetition(String competition) {
            this.competition = competition;
        }

        public int getAppearances() {
            return appearances;
        }

        public void setAppearances(int appearances) {
            this.appearances = appearances;
        }

        public int getGoals() {
            return goals;
        }

        public void setGoals(int goals) {
            this.goals = goals;
        }

        public int getAssists() {
            return assists;
        }

        public void setAssists(int assists) {
            this.assists = assists;
        }

        // Getters y Setters para aerialWons y rating...
    }
}
