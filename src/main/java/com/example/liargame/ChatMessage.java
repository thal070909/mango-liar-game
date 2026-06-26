package com.example.liargame;

import java.util.List;
import java.util.Map;

public class ChatMessage {
    public enum MessageType { JOIN, CHAT, LEAVE, GAME_START, SYSTEM, PLAYER_LIST, GAME_STARTED, TURN_START, EXPLANATION, WAITING_FOR_VOTE, VOTE_START, DEFENSE_START, DEFENSE_MSG, WAITING_FOR_PUBLIC_VOTE, PUBLIC_VOTE_START, PUBLIC_VOTE_UPDATE, LIAR_GUESS_START, ROUND_END, GAME_OVER_ALL, GAME_RESET }

    private MessageType type;
    private String roomId;
    private String playerId;
    private String sender;
    private String content;

    private String category;
    private String word;
    private String turnPlayerId;
    private String turnPlayerName;

    private int totalRounds;
    private int currentRound;
    private String hostId;
    private boolean playing;
    private String roomTitle;
    private boolean baboMode;

    private Map<String, String> publicVotes;
    private Map<String, Double> scores;

    private List<String> playerList;
    private Map<String, String> spectatorList;

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getTurnPlayerId() { return turnPlayerId; }
    public void setTurnPlayerId(String turnPlayerId) { this.turnPlayerId = turnPlayerId; }
    public String getTurnPlayerName() { return turnPlayerName; }
    public void setTurnPlayerName(String turnPlayerName) { this.turnPlayerName = turnPlayerName; }

    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public String getRoomTitle() { return roomTitle; }
    public void setRoomTitle(String roomTitle) { this.roomTitle = roomTitle; }
    public boolean isBaboMode() { return baboMode; }
    public void setBaboMode(boolean baboMode) { this.baboMode = baboMode; }

    public Map<String, String> getPublicVotes() { return publicVotes; }
    public void setPublicVotes(Map<String, String> publicVotes) { this.publicVotes = publicVotes; }
    public Map<String, Double> getScores() { return scores; }
    public void setScores(Map<String, Double> scores) { this.scores = scores; }

    public List<String> getPlayerList() { return playerList; }
    public void setPlayerList(List<String> playerList) { this.playerList = playerList; }
    public Map<String, String> getSpectatorList() { return spectatorList; }
    public void setSpectatorList(Map<String, String> spectatorList) { this.spectatorList = spectatorList; }
}