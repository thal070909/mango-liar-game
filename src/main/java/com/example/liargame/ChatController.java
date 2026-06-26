package com.example.liargame;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private Map<String, GameSession> sessions = new HashMap<>();

    // 💡여기에 이전에 드렸던 700개 대형 단어장 코드를 그대로 유지해주세요! (공간상 축약)
    private final Map<String, List<String>> gameDictionary = new HashMap<>() {{
        put("과일/야채", Arrays.asList("사과", "바나나", "파인애플", "복숭아", "수박"));
        put("동물", Arrays.asList("사자", "호랑이", "코끼리", "기린", "원숭이"));
        // ... (이전에 추가한 단어들 전체 포함) ...
    }};

    @MessageMapping("/chat.join")
    public void joinRoom(ChatMessage msg) {
        GameSession session = sessions.computeIfAbsent(msg.getRoomId(), k -> new GameSession(msg.getRoomId()));

        // 첫 입장 시 방의 총 라운드 설정
        if(session.getPlayers().isEmpty() && msg.getTotalRounds() > 0) {
            session.setTotalRounds(msg.getTotalRounds());
        }

        if (session.getPlayers().size() >= 10) {
            ChatMessage error = new ChatMessage();
            error.setType(ChatMessage.MessageType.SYSTEM);
            error.setContent("❌ 방이 가득 찼습니다!");
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), error);
            return;
        }

        session.getPlayers().put(msg.getPlayerId(), msg.getSender());

        ChatMessage systemMsg = new ChatMessage();
        systemMsg.setType(ChatMessage.MessageType.SYSTEM);
        systemMsg.setContent(msg.getSender() + "님이 입장하셨습니다.");
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), systemMsg);

        ChatMessage listMsg = new ChatMessage();
        listMsg.setType(ChatMessage.MessageType.PLAYER_LIST);
        listMsg.setPlayerList(new ArrayList<>(session.getPlayers().values()));
        listMsg.setTotalRounds(session.getTotalRounds());
        listMsg.setCurrentRound(session.getCurrentRound());
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), listMsg);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage msg) {
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), msg);
    }

    @MessageMapping("/game.start")
    public void startGame(ChatMessage msg) {
        String roomId = msg.getRoomId();
        GameSession session = sessions.get(roomId);
        if (session == null || session.getPlayers().isEmpty()) return;

        session.cancelTimer();
        session.getVotes().clear();
        session.getPublicVotes().clear();
        session.setAccusedFullName(null);

        List<String> categories = new ArrayList<>(gameDictionary.keySet());
        String selectedCategory = categories.get(new Random().nextInt(categories.size()));
        List<String> words = gameDictionary.get(selectedCategory);
        String selectedWord = words.get(new Random().nextInt(words.size()));

        List<String> playerIds = new ArrayList<>(session.getPlayers().keySet());
        String liarId = playerIds.get(new Random().nextInt(playerIds.size()));

        session.setWord(selectedWord);
        session.setLiarId(liarId);
        session.setPlaying(true);

        int startIndex = new Random().nextInt(playerIds.size());
        List<String> orderedIds = new ArrayList<>();
        for (int i = 0; i < playerIds.size(); i++) orderedIds.add(playerIds.get((startIndex + i) % playerIds.size()));

        session.setTurnOrder(orderedIds);
        session.setCurrentTurnIndex(0);

        ChatMessage startNotice = new ChatMessage();
        startNotice.setType(ChatMessage.MessageType.GAME_STARTED);
        startNotice.setContent("🚨 [" + session.getCurrentRound() + "라운드] 게임 시작! 주제는 [" + selectedCategory + "] 입니다.");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, startNotice);

        for (String playerId : playerIds) {
            ChatMessage privateMsg = new ChatMessage();
            privateMsg.setType(ChatMessage.MessageType.GAME_START);
            privateMsg.setCategory(selectedCategory);
            if (playerId.equals(liarId)) {
                privateMsg.setContent("당신은 라이어입니다!");
                privateMsg.setWord("???");
            } else {
                privateMsg.setContent("당신은 시민입니다.");
                privateMsg.setWord(selectedWord);
            }
            messagingTemplate.convertAndSend("/topic/player/" + playerId, privateMsg);
        }

        Timer startTimer = new Timer();
        session.setGameTimer(startTimer);
        startTimer.schedule(new TimerTask() { @Override public void run() { startNextTurn(roomId); } }, 5000);
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
            startVoteLogic(roomId);
            return;
        }

        String currentTurnId = session.getTurnOrder().get(session.getCurrentTurnIndex());
        String currentTurnName = session.getPlayers().get(currentTurnId);

        ChatMessage turnMsg = new ChatMessage();
        turnMsg.setType(ChatMessage.MessageType.TURN_START);
        turnMsg.setTurnPlayerId(currentTurnId);
        turnMsg.setTurnPlayerName(currentTurnName);
        turnMsg.setContent("🗣️ [" + currentTurnName + "] 님의 차례!");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, turnMsg);

        Timer turnTimer = new Timer();
        session.setGameTimer(turnTimer);
        turnTimer.schedule(new TimerTask() { @Override public void run() {
            session.setCurrentTurnIndex(session.getCurrentTurnIndex() + 1);
            startNextTurn(roomId);
        } }, 30000);
    }

    private void startVoteLogic(String roomId) {
        GameSession session = sessions.get(roomId);
        session.cancelTimer();
        session.getVotes().clear();

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

    // 1차 투표 결과 확인 -> 반론 시작
    private void checkInitialVoteResult(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null || !session.isPlaying()) return;

        Map<String, Integer> voteCount = new HashMap<>();
        for (String voted : session.getVotes().values()) {
            voteCount.put(voted, voteCount.getOrDefault(voted, 0) + 1);
        }

        String maxVoted = null;
        int maxVotes = 0;
        boolean tie = false;

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                maxVoted = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) { tie = true; }
        }

        if (session.getVotes().isEmpty() || tie) {
            endRound(roomId, "⚖️ 투표 동률/무효 발생! 아무도 용의자로 지목되지 않았습니다.\n(진짜 라이어: " + session.getPlayers().get(session.getLiarId()) + ")");
            return;
        }

        // 최다 득표자 확정 -> 반론 시작
        session.setAccusedFullName(maxVoted);
        ChatMessage defenseMsg = new ChatMessage();
        defenseMsg.setType(ChatMessage.MessageType.DEFENSE_START);
        defenseMsg.setContent(maxVoted); // 최다 득표자 이름 전달
        messagingTemplate.convertAndSend("/topic/room/" + roomId, defenseMsg);

        Timer defenseTimer = new Timer();
        session.setGameTimer(defenseTimer);
        defenseTimer.schedule(new TimerTask() { @Override public void run() { startPublicVote(roomId); } }, 30000);
    }

    // 반론 스킵
    @MessageMapping("/game.skipDefense")
    public void skipDefense(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && session.isPlaying()) {
            session.cancelTimer();
            startPublicVote(msg.getRoomId());
        }
    }

    // 최종 찬반 공개 투표 시작
    private void startPublicVote(String roomId) {
        GameSession session = sessions.get(roomId);
        session.cancelTimer();

        ChatMessage publicVoteMsg = new ChatMessage();
        publicVoteMsg.setType(ChatMessage.MessageType.PUBLIC_VOTE_START);
        publicVoteMsg.setContent(session.getAccusedFullName());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, publicVoteMsg);

        Timer pVoteTimer = new Timer();
        session.setGameTimer(pVoteTimer);
        pVoteTimer.schedule(new TimerTask() { @Override public void run() { processFinalResult(roomId); } }, 30000);
    }

    // 찬반 투표 접수 및 실시간 현황 중계
    @MessageMapping("/game.publicVote")
    public void receivePublicVote(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session == null || !session.isPlaying()) return;

        session.getPublicVotes().put(msg.getSender(), msg.getContent()); // sender(이름) -> 찬/반 기록

        ChatMessage updateMsg = new ChatMessage();
        updateMsg.setType(ChatMessage.MessageType.PUBLIC_VOTE_UPDATE);
        updateMsg.setPublicVotes(session.getPublicVotes());
        messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), updateMsg);

        // 용의자를 제외한 전원이 투표했으면 결과 집계 (총원 - 1)
        if (session.getPublicVotes().size() >= session.getPlayers().size() - 1) {
            session.cancelTimer();
            processFinalResult(msg.getRoomId());
        }
    }

    // 라운드 승패 판정
    private void processFinalResult(String roomId) {
        GameSession session = sessions.get(roomId);
        if (session == null || !session.isPlaying()) return;

        int killCount = 0;
        int saveCount = 0;
        for (String vote : session.getPublicVotes().values()) {
            if ("KILL".equals(vote)) killCount++;
            else if ("SAVE".equals(vote)) saveCount++;
        }

        String liarFullName = session.getPlayers().get(session.getLiarId());
        String resultMessage;

        if (killCount > saveCount) { // 처형 확정
            if (session.getAccusedFullName().equals(liarFullName)) {
                resultMessage = "🎉 처형 성공!\n[" + session.getAccusedFullName() + "] 님은 진짜 라이어였습니다.\n\n🎊 시민 승리!\n(제시어: " + session.getWord() + ")";
            } else {
                resultMessage = "🤦‍♂️ 애먼 사람을 죽였습니다!\n[" + session.getAccusedFullName() + "] 님은 선량한 시민이었습니다.\n진짜 라이어는 [" + liarFullName + "] 님입니다.\n\n😈 라이어 승리!\n(제시어: " + session.getWord() + ")";
            }
        } else { // 생존 (반대가 많거나 동률)
            resultMessage = "🛡️ 처형 부결!\n[" + session.getAccusedFullName() + "] 님이 살아남았습니다.\n진짜 라이어는 [" + liarFullName + "] 님입니다.\n\n😈 라이어 승리!\n(제시어: " + session.getWord() + ")";
        }

        endRound(roomId, resultMessage);
    }

    // 라운드 종료 처리
    private void endRound(String roomId, String resultMessage) {
        GameSession session = sessions.get(roomId);
        session.setPlaying(false);

        if (session.getCurrentRound() >= session.getTotalRounds()) {
            // 게임 완전 종료
            ChatMessage finalMsg = new ChatMessage();
            finalMsg.setType(ChatMessage.MessageType.GAME_OVER_ALL);
            finalMsg.setContent(resultMessage + "\n\n🏁 모든 라운드가 종료되었습니다!\n(최종 스코어보드 집계 준비 중...)");
            messagingTemplate.convertAndSend("/topic/room/" + roomId, finalMsg);
        } else {
            // 다음 라운드로
            ChatMessage roundMsg = new ChatMessage();
            roundMsg.setType(ChatMessage.MessageType.ROUND_END);
            roundMsg.setContent(resultMessage + "\n\n👉 다음 라운드를 준비해주세요.");
            messagingTemplate.convertAndSend("/topic/room/" + roomId, roundMsg);

            session.setCurrentRound(session.getCurrentRound() + 1);
        }
    }
}