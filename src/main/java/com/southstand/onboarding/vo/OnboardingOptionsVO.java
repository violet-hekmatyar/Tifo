package com.southstand.onboarding.vo;

import java.util.ArrayList;
import java.util.List;

public class OnboardingOptionsVO {

    private List<TeamOptionVO> recommendedTeams = new ArrayList<>();
    private List<TeamOptionVO> hotTeams = new ArrayList<>();
    private List<PlayerOptionVO> recommendedPlayers = new ArrayList<>();
    private List<PlayerOptionVO> hotPlayers = new ArrayList<>();

    public List<TeamOptionVO> getRecommendedTeams() {
        return recommendedTeams;
    }

    public void setRecommendedTeams(List<TeamOptionVO> recommendedTeams) {
        this.recommendedTeams = recommendedTeams;
    }

    public List<TeamOptionVO> getHotTeams() {
        return hotTeams;
    }

    public void setHotTeams(List<TeamOptionVO> hotTeams) {
        this.hotTeams = hotTeams;
    }

    public List<PlayerOptionVO> getRecommendedPlayers() {
        return recommendedPlayers;
    }

    public void setRecommendedPlayers(List<PlayerOptionVO> recommendedPlayers) {
        this.recommendedPlayers = recommendedPlayers;
    }

    public List<PlayerOptionVO> getHotPlayers() {
        return hotPlayers;
    }

    public void setHotPlayers(List<PlayerOptionVO> hotPlayers) {
        this.hotPlayers = hotPlayers;
    }
}
