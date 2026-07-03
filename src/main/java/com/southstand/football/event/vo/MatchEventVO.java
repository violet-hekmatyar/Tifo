package com.southstand.football.event.vo;

public class MatchEventVO {

    private Long eventId;
    private String eventType;
    private Integer minute;
    private Integer extraMinute;
    private Long teamId;
    private String teamName;
    private Long playerId;
    private String playerName;
    private Long assistPlayerId;
    private String assistPlayerName;
    private String scoreAfter;
    private String description;
    private Boolean hasDebate;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }
    public Integer getExtraMinute() { return extraMinute; }
    public void setExtraMinute(Integer extraMinute) { this.extraMinute = extraMinute; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Long getAssistPlayerId() { return assistPlayerId; }
    public void setAssistPlayerId(Long assistPlayerId) { this.assistPlayerId = assistPlayerId; }
    public String getAssistPlayerName() { return assistPlayerName; }
    public void setAssistPlayerName(String assistPlayerName) { this.assistPlayerName = assistPlayerName; }
    public String getScoreAfter() { return scoreAfter; }
    public void setScoreAfter(String scoreAfter) { this.scoreAfter = scoreAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getHasDebate() { return hasDebate; }
    public void setHasDebate(Boolean hasDebate) { this.hasDebate = hasDebate; }
}
