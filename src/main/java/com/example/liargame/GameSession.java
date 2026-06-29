package com.example.liargame;

import java.util.*;

public class GameSession {
    private String roomId;
    private String hostId;
    private String roomTitle;
    private String roomPassword;
    private Map<String, String> players = new LinkedHashMap<>();
    private Map<String, String> spectators = new LinkedHashMap<>();

    private String liarId;
    private String word;
    private boolean isPlaying = false;
    private boolean baboMode = false;

    private Map<String, String> votes = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;
    private Timer gameTimer;

    private int totalRounds = 3;
    private int currentRound = 1;
    private String accusedFullName;
    private Map<String, String> publicVotes = new HashMap<>();
    private Map<String, Double> scores = new HashMap<>();

    // [신규 추가] 턴수 제어 및 힌트 보관소
    private int maxExplanationTurns = 1;
    private int currentExplanationRound = 1;
    private Map<String, String> explanations = new HashMap<>();

    public GameSession(String roomId) { this.roomId = roomId; }

    public String getRoomId() { return roomId; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }
    public String getRoomPassword() { return roomPassword; }
    public void setRoomPassword(String roomPassword) { this.roomPassword = roomPassword; }
    public Map<String, String> getPlayers() { return players; }
    public Map<String, String> getSpectators() { return spectators; }

    public String getLiarId() { return liarId; }
    public void setLiarId(String liarId) { this.liarId = liarId; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean isPlaying) { this.isPlaying = isPlaying; }

    public boolean isBaboMode() { return baboMode; }
    public void setBaboMode(boolean baboMode) { this.baboMode = baboMode; }

    public Map<String, String> getVotes() { return votes; }
    public List<String> getTurnOrder() { return turnOrder; }
    public void setTurnOrder(List<String> turnOrder) { this.turnOrder = turnOrder; }
    public int getCurrentTurnIndex() { return currentTurnIndex; }
    public void setCurrentTurnIndex(int currentTurnIndex) { this.currentTurnIndex = currentTurnIndex; }

    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public String getAccusedFullName() { return accusedFullName; }
    public void setAccusedFullName(String accusedFullName) { this.accusedFullName = accusedFullName; }
    public Map<String, String> getPublicVotes() { return publicVotes; }
    public Map<String, Double> getScores() { return scores; }

    public int getMaxExplanationTurns() { return maxExplanationTurns; }
    public void setMaxExplanationTurns(int maxExplanationTurns) { this.maxExplanationTurns = maxExplanationTurns; }
    public int getCurrentExplanationRound() { return currentExplanationRound; }
    public void setCurrentExplanationRound(int currentExplanationRound) { this.currentExplanationRound = currentExplanationRound; }
    public Map<String, String> getExplanations() { return explanations; }

    public void cancelTimer() {
        if (this.gameTimer != null) {
            this.gameTimer.cancel();
            this.gameTimer = null;
        }
    }
    public void setGameTimer(Timer timer) { this.gameTimer = timer; }
}