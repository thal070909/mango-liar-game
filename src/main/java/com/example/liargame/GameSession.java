package com.example.liargame;

import java.util.*;

public class GameSession {
    private String roomId;
    private Map<String, String> players = new LinkedHashMap<>();
    private String liarId;
    private String word;
    private boolean isPlaying = false;

    // 기본 투표함 (라이어 지목)
    private Map<String, String> votes = new HashMap<>();

    // 턴 관리
    private List<String> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;
    private Timer gameTimer;

    // [신규] 라운드 및 찬반투표 관리
    private int totalRounds = 3;
    private int currentRound = 1;
    private String accusedFullName; // 최다 득표자(용의자) 이름
    private Map<String, String> publicVotes = new HashMap<>(); // 찬/반 투표함

    public GameSession(String roomId) { this.roomId = roomId; }

    public String getRoomId() { return roomId; }
    public Map<String, String> getPlayers() { return players; }
    public String getLiarId() { return liarId; }
    public void setLiarId(String liarId) { this.liarId = liarId; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public boolean isPlaying() { return isPlaying; }
    public void setPlaying(boolean playing) { this.isPlaying = playing; }

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

    public void cancelTimer() {
        if (this.gameTimer != null) {
            this.gameTimer.cancel();
            this.gameTimer = null;
        }
    }
    public void setGameTimer(Timer timer) { this.gameTimer = timer; }
}