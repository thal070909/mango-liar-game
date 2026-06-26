package com.example.liargame;

import java.util.List;
import java.util.Map;

public class ChatMessage {
    // 새로운 게임 상태들 추가 (반론, 찬반투표, 라운드종료 등)
    public enum MessageType { JOIN, CHAT, GAME_START, SYSTEM, PLAYER_LIST, GAME_STARTED, TURN_START, VOTE_START, DEFENSE_START, PUBLIC_VOTE_START, PUBLIC_VOTE_UPDATE, ROUND_END, GAME_OVER_ALL }

    private MessageType type;
    private String roomId;
    private String playerId;
    private String sender;
    private String content;

    private String category;
    private String word;
    private String turnPlayerId;
    private String turnPlayerName;

    // 라운드 및 찬반투표용 추가 변수
    private int totalRounds;
    private int currentRound;
    private Map<String, String> publicVotes;

    private List<String> playerList;

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
    public Map<String, String> getPublicVotes() { return publicVotes; }
    public void setPublicVotes(Map<String, String> publicVotes) { this.publicVotes = publicVotes; }

    public List<String> getPlayerList() { return playerList; }
    public void setPlayerList(List<String> playerList) { this.playerList = playerList; }
}