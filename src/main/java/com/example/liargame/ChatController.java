package com.example.liargame;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private Map<String, GameSession> sessions = new HashMap<>();

    // 💡 유저님이 구축해놓으신 대형 단어장 코드를 여기에 유지해주세요!!
    private final Map<String, List<String>> gameDictionary = new HashMap<>() {{
        put("과일/야채", Arrays.asList("사과", "바나나", "수박"));
        put("나라", Arrays.asList("대한민국", "미국", "일본"));
    }};

    @GetMapping("/api/rooms")
    @ResponseBody
    public List<Map<String, Object>> getActiveRooms() {
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (GameSession session : sessions.values()) {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", session.getRoomId());
            roomInfo.put("roomTitle", session.getRoomTitle());
            roomInfo.put("playerCount", session.getPlayers().size() + session.getSpectators().size());
            roomInfo.put("isPlaying", session.isPlaying());
            roomInfo.put("totalRounds", session.getTotalRounds());
            roomInfo.put("hasPassword", session.getRoomPassword() != null && !session.getRoomPassword().trim().isEmpty());
            roomList.add(roomInfo);
        }
        return roomList;
    }

    @MessageMapping("/chat.join")
    public void joinRoom(ChatMessage msg, SimpMessageHeaderAccessor headerAccessor) {
        GameSession session = sessions.computeIfAbsent(msg.getRoomId(), k -> new GameSession(msg.getRoomId()));

        if(session.getPlayers().isEmpty() && session.getSpectators().isEmpty()) {
            if(msg.getTotalRounds() > 0) session.setTotalRounds(msg.getTotalRounds());
            session.setHostId(msg.getPlayerId());
            String title = (msg.getRoomTitle() != null && !msg.getRoomTitle().trim().isEmpty()) ? msg.getRoomTitle() : "망고의 라이어 게임방";
            session.setRoomTitle(title);
            if(msg.getRoomPassword() != null && !msg.getRoomPassword().trim().isEmpty()) {
                session.setRoomPassword(msg.getRoomPassword().trim());
            }
        }

        if (session.getRoomPassword() != null && !session.getRoomPassword().isEmpty()) {
            if (!session.getRoomPassword().equals(msg.getRoomPassword())) {
                ChatMessage error = new ChatMessage();
                error.setType(ChatMessage.MessageType.SYSTEM);
                error.setContent("ERROR_PASSWORD");
                messagingTemplate.convertAndSend("/topic/player/" + msg.getPlayerId(), error);
                return;
            }
        }

        if (session.getPlayers().size() + session.getSpectators().size() >= 10 &&
                !session.getPlayers().containsKey(msg.getPlayerId()) && !session.getSpectators().containsKey(msg.getPlayerId())) {
            ChatMessage error = new ChatMessage();
            error.setType(ChatMessage.MessageType.SYSTEM);
            error.setContent("❌ 방이 가득 찼습니다!");
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), error);
            return;
        }

        headerAccessor.getSessionAttributes().put("roomId", msg.getRoomId());
        headerAccessor.getSessionAttributes().put("playerId", msg.getPlayerId());
        headerAccessor.getSessionAttributes().put("sender", msg.getSender());

        if (session.isPlaying()) {
            session.getSpectators().put(msg.getPlayerId(), msg.getSender());
        } else {
            session.getPlayers().put(msg.getPlayerId(), msg.getSender());
            session.getScores().putIfAbsent(msg.getSender(), 0.0);
        }
        sendPlayerListUpdate(session);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String roomId = (String) sessionAttributes.get("roomId");
            String playerId = (String) sessionAttributes.get("playerId");
            String sender = (String) sessionAttributes.get("sender");

            if (roomId != null && playerId != null && sender != null) {
                GameSession session = sessions.get(roomId);
                if (session != null) {
                    session.getPlayers().remove(playerId);
                    session.getSpectators().remove(playerId);
                    session.getScores().remove(sender);

                    ChatMessage systemMsg = new ChatMessage();
                    systemMsg.setType(ChatMessage.MessageType.SYSTEM);
                    systemMsg.setContent(sender + "님이 창을 닫아 퇴장하셨습니다. 🚪");
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMsg);

                    if (session.getPlayers().isEmpty() && session.getSpectators().isEmpty()) {
                        session.cancelTimer();
                        sessions.remove(roomId);
                    } else {
                        if (playerId.equals(session.getHostId())) {
                            List<String> candidates = new ArrayList<>(session.getPlayers().keySet());
                            if(candidates.isEmpty()) candidates.addAll(session.getSpectators().keySet());

                            if(!candidates.isEmpty()) {
                                String newHostId = candidates.get(new Random().nextInt(candidates.size()));
                                session.setHostId(newHostId);
                                ChatMessage hostNotice = new ChatMessage();
                                hostNotice.setType(ChatMessage.MessageType.SYSTEM);
                                hostNotice.setContent("👑 방장이 퇴장하여 랜덤으로 새로운 방장이 선출되었습니다!");
                                messagingTemplate.convertAndSend("/topic/room/" + roomId, hostNotice);
                            }
                        }
                        sendPlayerListUpdate(session);
                    }
                }
            }
        }
    }

    @MessageMapping("/chat.leave")
    public void leaveRoom(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null) {
            session.getPlayers().remove(msg.getPlayerId());
            session.getSpectators().remove(msg.getPlayerId());
            session.getScores().remove(msg.getSender());

            ChatMessage systemMsg = new ChatMessage();
            systemMsg.setType(ChatMessage.MessageType.SYSTEM);
            systemMsg.setContent(msg.getSender() + "님이 퇴장하셨습니다. 🚪");
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), systemMsg);

            if (session.getPlayers().isEmpty() && session.getSpectators().isEmpty()) {
                session.cancelTimer();
                sessions.remove(msg.getRoomId());
            } else {
                if (msg.getPlayerId().equals(session.getHostId())) {
                    List<String> candidates = new ArrayList<>(session.getPlayers().keySet());
                    if(candidates.isEmpty()) candidates.addAll(session.getSpectators().keySet());

                    if(!candidates.isEmpty()) {
                        String newHostId = candidates.get(new Random().nextInt(candidates.size()));
                        session.setHostId(newHostId);
                        ChatMessage hostNotice = new ChatMessage();
                        hostNotice.setType(ChatMessage.MessageType.SYSTEM);
                        hostNotice.setContent("👑 방장이 퇴장하여 랜덤으로 새로운 방장이 선출되었습니다!");
                        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), hostNotice);
                    }
                }
                sendPlayerListUpdate(session);
            }
        }
    }

    @MessageMapping("/game.spectatorJoin")
    public void spectatorJoin(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && !session.isPlaying() && session.getCurrentRound() == 1 && session.getSpectators().containsKey(msg.getPlayerId())) {
            String sName = session.getSpectators().remove(msg.getPlayerId());
            session.getPlayers().put(msg.getPlayerId(), sName);
            session.getScores().putIfAbsent(sName, 0.0);

            ChatMessage systemMsg = new ChatMessage();
            systemMsg.setType(ChatMessage.MessageType.SYSTEM);
            systemMsg.setContent("--- [" + sName + "] 님이 정식 멤버로 게임에 합류했습니다! ---");
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), systemMsg);

            sendPlayerListUpdate(session);
        }
    }

    private void sendPlayerListUpdate(GameSession session) {
        ChatMessage listMsg = new ChatMessage();
        listMsg.setType(ChatMessage.MessageType.PLAYER_LIST);
        listMsg.setPlayerList(new ArrayList<>(session.getPlayers().values()));
        listMsg.setSpectatorList(session.getSpectators());
        listMsg.setTotalRounds(session.getTotalRounds());
        listMsg.setCurrentRound(session.getCurrentRound());
        listMsg.setHostId(session.getHostId());
        listMsg.setScores(session.getScores());
        listMsg.setPlaying(session.isPlaying());
        listMsg.setRoomTitle(session.getRoomTitle());
        listMsg.setExplanations(session.getExplanations());

        messagingTemplate.convertAndSend("/topic/room/" + session.getRoomId(), listMsg);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage msg) { messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), msg); }

    @MessageMapping("/game.start")
    public void startGame(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || session.getPlayers().isEmpty() || !msg.getPlayerId().equals(session.getHostId())) return;

        session.cancelTimer();
        session.getVotes().clear();
        session.getPublicVotes().clear();
        session.setAccusedFullName(null);

        session.setPlaying(true);
        session.setBaboMode(msg.isBaboMode());
        session.setMaxExplanationTurns(msg.getMaxExplanationTurns());
        session.setCurrentExplanationRound(1);
        session.getExplanations().clear();

        List<String> categories = new ArrayList<>(gameDictionary.keySet());
        String selectedCategory = categories.get(new Random().nextInt(categories.size()));
        List<String> words = gameDictionary.get(selectedCategory);
        String selectedWord = words.get(new Random().nextInt(words.size()));

        List<String> playerIds = new ArrayList<>(session.getPlayers().keySet());
        String liarId = playerIds.get(new Random().nextInt(playerIds.size()));

        session.setWord(selectedWord);
        session.setLiarId(liarId);

        String liarWord = "???";
        if (session.isBaboMode() && words.size() > 1) {
            List<String> dummyWords = new ArrayList<>(words);
            dummyWords.remove(selectedWord);
            liarWord = dummyWords.get(new Random().nextInt(dummyWords.size()));
        }

        sendPlayerListUpdate(session);

        int startIndex = new Random().nextInt(playerIds.size());
        List<String> orderedIds = new ArrayList<>();
        for (int i = 0; i < playerIds.size(); i++) orderedIds.add(playerIds.get((startIndex + i) % playerIds.size()));
        session.setTurnOrder(orderedIds);
        session.setCurrentTurnIndex(0);

        ChatMessage startNotice = new ChatMessage();
        startNotice.setType(ChatMessage.MessageType.GAME_STARTED);
        startNotice.setContent("🚨 [" + session.getCurrentRound() + "라운드] 게임 시작! 주제는 [" + selectedCategory + "] 입니다.");
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), startNotice);

        for (String playerId : playerIds) {
            ChatMessage privateMsg = new ChatMessage();
            privateMsg.setType(ChatMessage.MessageType.GAME_START);
            privateMsg.setCategory(selectedCategory);
            if (playerId.equals(liarId)) {
                privateMsg.setContent("당신은 라이어입니다!");
                privateMsg.setWord(liarWord);
            } else {
                privateMsg.setContent("당신은 시민입니다.");
                privateMsg.setWord(selectedWord);
            }
            messagingTemplate.convertAndSend("/topic/player/" + playerId, privateMsg);
        }

        Timer startTimer = new Timer();
        session.setGameTimer(startTimer);
        startTimer.schedule(new TimerTask() { @Override public void run() { startNextTurn(msg.getRoomId()); } }, 5000);
    }

    @MessageMapping("/game.explain")
    public void receiveExplanation(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying()) {

            String existingHint = session.getExplanations().getOrDefault(msg.getSender(), "");
            String newHint = existingHint.isEmpty() ? msg.getContent() : existingHint + " / " + msg.getContent();
            session.getExplanations().put(msg.getSender(), newHint);

            ChatMessage expMsg = new ChatMessage();
            expMsg.setType(ChatMessage.MessageType.EXPLANATION);
            expMsg.setSender(msg.getSender());
            expMsg.setContent(newHint);
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), expMsg);

            session.cancelTimer();
            session.setCurrentTurnIndex(session.getCurrentTurnIndex() + 1);
            startNextTurn(msg.getRoomId());
        }
    }

    @MessageMapping("/game.passTurn")
    public void passTurn(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying()) {
            session.cancelTimer();
            session.setCurrentTurnIndex(session.getCurrentTurnIndex() + 1);
            startNextTurn(msg.getRoomId());
        }
    }

    private void startNextTurn(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null) return;
        session.cancelTimer();

        if (session.getCurrentTurnIndex() >= session.getTurnOrder().size()) {
            if (session.getCurrentExplanationRound() < session.getMaxExplanationTurns()) {
                session.setCurrentExplanationRound(session.getCurrentExplanationRound() + 1);
                session.setCurrentTurnIndex(0);

                ChatMessage nextRoundMsg = new ChatMessage();
                nextRoundMsg.setType(ChatMessage.MessageType.SYSTEM);
                nextRoundMsg.setContent("🔄 모든 인원의 " + (session.getCurrentExplanationRound() - 1) + "차 설명이 종료되었습니다. 이어서 [" + session.getCurrentExplanationRound() + "차 설명]을 시작합니다!");
                messagingTemplate.convertAndSend("/topic/room/" + roomId, nextRoundMsg);

                startNextTurn(roomId);
            } else {
                ChatMessage waitMsg = new ChatMessage();
                waitMsg.setType(ChatMessage.MessageType.WAITING_FOR_VOTE);
                waitMsg.setContent("👀 모든 설명 종료! 추리 완료 후 방장이 수동으로 투표를 시작하세요.");
                messagingTemplate.convertAndSend("/topic/room/" + roomId, waitMsg);
            }
            return;
        }

        String currentTurnId = session.getTurnOrder().get(session.getCurrentTurnIndex());
        String currentTurnName = session.getPlayers().get(currentTurnId);

        ChatMessage turnMsg = new ChatMessage();
        turnMsg.setType(ChatMessage.MessageType.TURN_START);
        turnMsg.setTurnPlayerId(currentTurnId);
        turnMsg.setTurnPlayerName(currentTurnName);
        turnMsg.setContent("🗣️ [" + currentTurnName + "] 님의 " + session.getCurrentExplanationRound() + "차 설명 차례!");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, turnMsg);

        Timer turnTimer = new Timer();
        session.setGameTimer(turnTimer);
        turnTimer.schedule(new TimerTask() { @Override public void run() {
            session.setCurrentTurnIndex(session.getCurrentTurnIndex() + 1);
            startNextTurn(roomId);
        } }, 30000);
    }

    @MessageMapping("/game.startVotePhase")
    public void startVotePhase(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying() && msg.getPlayerId().equals(session.getHostId())) {
            startVoteLogic(msg.getRoomId());
        }
    }

    private void startVoteLogic(String roomId) {
        GameSession session = sessions.get(roomId);
        session.cancelTimer();
        ChatMessage voteNotice = new ChatMessage();
        voteNotice.setType(ChatMessage.MessageType.VOTE_START);
        voteNotice.setContent("⚖️ 30초 내에 라이어를 지목하세요!");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, voteNotice);

        Timer voteTimer = new Timer();
        session.setGameTimer(voteTimer);
        voteTimer.schedule(new TimerTask() { @Override public void run() { checkInitialVoteResult(roomId); } }, 30000);
    }

    @MessageMapping("/game.vote")
    public void receiveVote(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || !session.isPlaying()) return;

        session.getVotes().put(msg.getPlayerId(), msg.getContent());
        if (session.getVotes().size() == session.getPlayers().size()) {
            session.cancelTimer();
            checkInitialVoteResult(msg.getRoomId());
        }
    }

    private void checkInitialVoteResult(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null || !session.isPlaying()) return;

        Map<String, Integer> voteCount = new HashMap<>();
        for (String voted : session.getVotes().values()) voteCount.put(voted, voteCount.getOrDefault(voted, 0) + 1);

        String maxVoted = null; int maxVotes = 0; boolean tie = false;
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) { maxVotes = entry.getValue(); maxVoted = entry.getKey(); tie = false; }
            else if (entry.getValue() == maxVotes) { tie = true; }
        }

        if (session.getVotes().isEmpty() || tie) {
            calculateLiarWinScore(session, 1.0);
            endRound(roomId, "⚖️ 투표 동률! 용의자를 가리지 못해 라이어가 무사히 생존했습니다.\n(진짜 라이어: " + session.getPlayers().get(session.getLiarId()) + ")\n\n😈 라이어 승리! (+1점)");
            return;
        }

        session.setAccusedFullName(maxVoted);
        ChatMessage defenseMsg = new ChatMessage();
        defenseMsg.setType(ChatMessage.MessageType.DEFENSE_START);
        defenseMsg.setContent(maxVoted);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, defenseMsg);

        Timer defenseTimer = new Timer();
        session.setGameTimer(defenseTimer);
        defenseTimer.schedule(new TimerTask() { @Override public void run() { triggerPublicVoteWaitingPhase(roomId); } }, 30000);
    }

    @MessageMapping("/game.defend")
    public void receiveDefense(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying()) {
            ChatMessage defMsg = new ChatMessage();
            defMsg.setType(ChatMessage.MessageType.DEFENSE_MSG);
            defMsg.setSender(msg.getSender());
            defMsg.setContent(msg.getContent());
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), defMsg);

            session.cancelTimer();
            triggerPublicVoteWaitingPhase(msg.getRoomId());
        }
    }

    @MessageMapping("/game.skipDefense")
    public void skipDefense(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying()) {
            session.cancelTimer();
            triggerPublicVoteWaitingPhase(msg.getRoomId());
        }
    }

    private void triggerPublicVoteWaitingPhase(String roomId) {
        GameSession session = sessions.get(roomId);
        if(session == null) return;

        ChatMessage waitMsg = new ChatMessage();
        waitMsg.setType(ChatMessage.MessageType.WAITING_FOR_PUBLIC_VOTE);
        waitMsg.setContent("👀 최후 변론이 끝났습니다! 충분히 추리한 뒤 방장이 찬반 투표를 개시하세요.");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, waitMsg);
    }

    @MessageMapping("/game.startPublicVotePhase")
    public void startPublicVotePhase(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying() && msg.getPlayerId().equals(session.getHostId())) {
            startPublicVote(msg.getRoomId());
        }
    }

    private void startPublicVote(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null) return;
        session.cancelTimer();
        session.getPublicVotes().clear();

        // [버그 방어] 혹시라도 용의자 정보가 서버에서 지워졌다면 안전하게 무효 처리 후 라운드를 넘김
        if (session.getAccusedFullName() == null) {
            endRound(roomId, "⚠️ 투표 처리에 오류가 발생했습니다. (무효 처리)\n\n라운드를 강제로 종료합니다.");
            return;
        }

        List<String> unvotedList = new ArrayList<>();
        for (String pName : session.getPlayers().values()) {
            if (!pName.equals(session.getAccusedFullName())) unvotedList.add(pName);
        }
        String unvotedStr = unvotedList.isEmpty() ? "없음" : String.join(", ", unvotedList);

        ChatMessage publicVoteMsg = new ChatMessage();
        publicVoteMsg.setType(ChatMessage.MessageType.PUBLIC_VOTE_START);
        publicVoteMsg.setContent(session.getAccusedFullName());
        publicVoteMsg.setTurnPlayerName(unvotedStr);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, publicVoteMsg);

        Timer pVoteTimer = new Timer();
        session.setGameTimer(pVoteTimer);
        pVoteTimer.schedule(new TimerTask() { @Override public void run() { processPublicVoteResult(roomId); } }, 30000);
    }

    @MessageMapping("/game.publicVote")
    public void receivePublicVote(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || !session.isPlaying()) return;

        session.getPublicVotes().put(msg.getSender(), msg.getContent());

        List<String> unvotedList = new ArrayList<>();
        for (String pName : session.getPlayers().values()) {
            if (!pName.equals(session.getAccusedFullName()) && !session.getPublicVotes().containsKey(pName)) {
                unvotedList.add(pName);
            }
        }
        String unvotedStr = unvotedList.isEmpty() ? "없음 (전원 완료)" : String.join(", ", unvotedList);

        ChatMessage updateMsg = new ChatMessage();
        updateMsg.setType(ChatMessage.MessageType.PUBLIC_VOTE_UPDATE);
        updateMsg.setPublicVotes(session.getPublicVotes());
        updateMsg.setContent(unvotedStr);
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), updateMsg);

        if (session.getPublicVotes().size() >= session.getPlayers().size() - 1) {
            session.cancelTimer();
            processPublicVoteResult(msg.getRoomId());
        }
    }

    private void processPublicVoteResult(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null || !session.isPlaying()) return;

        int killCount = 0; int saveCount = 0;
        for (String vote : session.getPublicVotes().values()) {
            if ("KILL".equals(vote)) killCount++; else if ("SAVE".equals(vote)) saveCount++;
        }

        String liarFullName = session.getPlayers().get(session.getLiarId());

        if (killCount > saveCount) {
            if (session.getAccusedFullName().equals(liarFullName)) {
                ChatMessage guessNotice = new ChatMessage();
                guessNotice.setType(ChatMessage.MessageType.LIAR_GUESS_START);
                guessNotice.setContent(liarFullName);
                messagingTemplate.convertAndSend("/topic/room/" + roomId, guessNotice);

                Timer guessTimer = new Timer();
                session.setGameTimer(guessTimer);
                guessTimer.schedule(new TimerTask() { @Override public void run() {
                    calculateCitizenWinScore(session);
                    endRound(roomId, "⏰ 시간 초과! 라이어가 정답을 맞추지 못했습니다.\n\n🎊 시민 승리!\n(제시어: " + session.getWord() + ")");
                } }, 30000);
                return;
            } else {
                calculateLiarWinScore(session, 1.0);
                endRound(roomId, "🤦‍♂️ 애먼 시민 [" + session.getAccusedFullName() + "] 님이 처형당했습니다.\n진짜 라이어는 [" + liarFullName + "] 님입니다.\n\n😈 라이어 승리! (+1점)\n(제시어: " + session.getWord() + ")");
            }
        } else {
            calculateLiarWinScore(session, 1.0);
            endRound(roomId, "🛡️ 처형 부결! [" + session.getAccusedFullName() + "] 님이 살아남았습니다.\n진짜 라이어는 [" + liarFullName + "] 님입니다.\n\n😈 라이어 승리! (+1점)\n(제시어: " + session.getWord() + ")");
        }
    }

    @MessageMapping("/game.liarGuess")
    public void receiveLiarGuess(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || !session.isPlaying()) return;
        session.cancelTimer();

        String guess = msg.getContent().replace(" ", "").trim();
        String answer = session.getWord().replace(" ", "").trim();

        if (guess.equals(answer)) {
            calculateLiarWinScore(session, 2.0);
            endRound(msg.getRoomId(), "😱 대반전!\n라이어가 처형 직전 정답 [" + session.getWord() + "]을(를) 정확히 맞췄습니다!\n\n😈 라이어 역전 승리! (+2점 / 시민 0점)");
        } else {
            calculateCitizenWinScore(session);
            endRound(msg.getRoomId(), "🎉 검거 확정!\n라이어가 정답 유추에 실패했습니다. (답변: " + msg.getContent() + ")\n\n🎊 시민 승리!\n(제시어: " + session.getWord() + ")");
        }
    }

    private void calculateCitizenWinScore(GameSession session) {
        String liarId = session.getLiarId();
        String liarName = session.getPlayers().get(liarId);

        for (Map.Entry<String, String> entry : session.getPlayers().entrySet()) {
            String pId = entry.getKey();
            String pName = entry.getValue();
            if (pId.equals(liarId)) continue;

            String initialVote = session.getVotes().get(pId);
            String publicVote = session.getPublicVotes().get(pName);

            if (liarName.equals(initialVote)) {
                session.getScores().put(pName, session.getScores().getOrDefault(pName, 0.0) + 1.0);
            } else if ("KILL".equals(publicVote)) {
                session.getScores().put(pName, session.getScores().getOrDefault(pName, 0.0) + 0.5);
            }
        }
    }

    private void calculateLiarWinScore(GameSession session, double points) {
        String liarName = session.getPlayers().get(session.getLiarId());
        session.getScores().put(liarName, session.getScores().getOrDefault(liarName, 0.0) + points);
    }

    private void endRound(String roomId, String resultMessage) {
        GameSession session = sessions.get(roomId);
        session.setPlaying(false);

        boolean isGameOver = session.getCurrentRound() >= session.getTotalRounds();

        if (!isGameOver) {
            session.setCurrentRound(session.getCurrentRound() + 1);
        }

        sendPlayerListUpdate(session);

        if (isGameOver) {
            ChatMessage finalMsg = new ChatMessage();
            finalMsg.setType(ChatMessage.MessageType.GAME_OVER_ALL);
            finalMsg.setContent(resultMessage);
            finalMsg.setScores(session.getScores());
            messagingTemplate.convertAndSend("/topic/room/" + roomId, finalMsg);
        } else {
            ChatMessage roundMsg = new ChatMessage();
            roundMsg.setType(ChatMessage.MessageType.ROUND_END);
            roundMsg.setContent(resultMessage);
            roundMsg.setScores(session.getScores());
            messagingTemplate.convertAndSend("/topic/room/" + roomId, roundMsg);
        }
    }

    @MessageMapping("/game.continue")
    public void continueGame(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || !msg.getPlayerId().equals(session.getHostId())) return;

        session.setCurrentRound(1);
        session.getPlayers().putAll(session.getSpectators());
        session.getSpectators().clear();

        for(String pName : session.getPlayers().values()) session.getScores().put(pName, 0.0);

        ChatMessage resetMsg = new ChatMessage();
        resetMsg.setType(ChatMessage.MessageType.GAME_RESET);
        resetMsg.setTotalRounds(session.getTotalRounds());
        resetMsg.setCurrentRound(1);
        resetMsg.setScores(session.getScores());
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), resetMsg);
        sendPlayerListUpdate(session);
    }
}