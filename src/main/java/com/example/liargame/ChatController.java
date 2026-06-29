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

       private final Map<String, List<String>> gameDictionary = new HashMap<>() {{
        put("과일/야채", Arrays.asList(
                "사과", "바나나", "파인애플", "복숭아", "수박", "망고", "딸기", "포도", "멜론", "참외",
                "자두", "살구", "감", "귤", "오렌지", "자몽", "레몬", "라임", "키위", "블루베리",
                "체리", "석류", "무화과", "대추", "밤", "배", "토마토", "오이", "당근", "감자",
                "고구마", "양파", "마늘", "대파", "배추", "상추", "시금치", "브로콜리", "양배추", "고추",
                "파프리카", "피망", "표고버섯", "호박", "가지", "옥수수", "완두콩", "아스파라거스", "셀러리", "생강"
        ));

        put("동물", Arrays.asList(
                "사자", "호랑이", "코끼리", "기린", "원숭이", "고양이", "강아지", "펭귄", "판다", "북극곰",
                "여우", "늑대", "토끼", "다람쥐", "생쥐", "얼룩말", "하마", "코뿔소", "낙타", "캥거루",
                "코알라", "치타", "표범", "하이에나", "멧돼지", "사슴", "고슴도치", "두더지", "박쥐", "나무늘보",
                "독수리", "부엉이", "까마귀", "비둘기", "참새", "공작", "타조", "갈매기", "뱀", "악어",
                "거북이", "도마뱀", "개구리", "맹꽁이", "돌고래", "범고래", "상어", "문어", "오징어", "바다거북"
        ));

        put("나라", Arrays.asList(
                "대한민국", "일본", "중국", "미국", "영국", "프랑스", "독일", "러시아", "캐나다", "이탈리아",
                "호주", "뉴질랜드", "스페인", "포르투갈", "네덜란드", "벨기에", "스위스", "오스트리아", "그리스", "튀르키예",
                "브라질", "아르헨티나", "멕시코", "칠레", "콜롬비아", "페루", "우루과이", "베네수엘라", "인도", "인도네시아",
                "베트남", "태국", "필리핀", "싱가포르", "말레이시아", "사우디아라비아", "아랍에미리트", "이란", "이라크", "이스라엘",
                "이집트", "남아프리카공화국", "케냐", "나이지리아", "가나", "에티오피아", "모로코", "알제리", "튀니지", "리비아",
                "수단", "우크라이나", "벨라루스", "폴란드", "체코", "헝가리", "루마니아", "불가리아", "슬로바키아", "크로아티아",
                "세르비아", "노르웨이", "스웨덴", "핀란드", "덴마크", "아이슬란드", "아일랜드", "룩셈부르크", "모나코", "몽골",
                "북한", "캄보디아", "라오스", "미얀마", "방글라데시", "파키스탄", "아프가니스탄", "네팔", "스리랑카", "몰디브",
                "우즈베키스탄", "카자흐스탄", "키르기스스탄", "타지키스탄", "투르크메니스탄", "조지아", "아르메니아", "아제르바이잔", "요르단", "시리아",
                "레바논", "쿠웨이트", "카타르", "바레인", "오만", "예멘", "쿠바", "자메이카", "아이티", "도미니카공화국",
                "바하마", "파나마", "코스타리카", "온두라스", "과테말라", "엘살바도르", "니카라과", "에콰도르", "볼리비아", "파라과이",
                "가이아나", "수리남", "마다가스카르", "앙골라", "모잠비크", "잠비아", "짐바브웨", "보츠와나", "나미비아", "콩고민주공화국",
                "콩고공화국", "가봉", "카메룬", "중앙아프리카공화국", "차드", "니제르", "말리", "모리타니", "세네갈", "감비아",
                "기니", "기니비사우", "시에라리온", "라이베리아", "코트디부아르", "부르키나파소", "토고", "베냉", "소말리아", "지부티",
                "우간다", "르완다", "부룬디", "탄자니아", "말라위", "레소토", "에스와티니", "세이셸", "모리셔스", "코모로",
                "카보베르데", "상투메프린시페", "적도기니", "에리트레아", "파푸아뉴기니", "피지", "솔로몬제도", "바누아투", "사모아", "통가",
                "키리바시", "투발루", "나우루", "마셜제도", "팔라우", "마이크로네시아", "안도라", "산마리노", "리히텐슈타인", "몰타",
                "키프로스", "알바니아", "보스니아헤르체고비나", "북마케도니아", "몬테네그로", "몰도바", "에스토니아", "라트비아", "리투아니아", "안티구아바부다",
                "세인트키츠네비스", "도미니카연방", "세인트루시아", "세인트빈센트그레나딘", "바베이도스", "트리니다드토바고", "벨리즈"
        ));

        put("직업", Arrays.asList(
                "경찰관", "소방관", "의사", "간호사", "약사", "수의사", "유치원교사", "초등교사", "대학교수", "프로그래머",
                "요리사", "유튜버", "가수", "영화배우", "패션모델", "직업군인", "외교관", "판사", "검사", "변호사",
                "공인회계사", "세무사", "건축가", "비행기조종사", "객실승무원", "기자", "아나운서", "기상캐스터", "패션디자이너", "미용사",
                "웹툰작가", "소설가", "화가", "사진작가", "피아니스트", "작곡가", "프로게이머", "영화감독", "야구심판", "마술사",
                "개그맨", "성우", "제빵사", "바리스타", "소믈리에", "동물사육사", "정원사", "전업농부", "선장", "카레이서",
                "뮤지컬배우", "연극배우", "백댄서", "성악가", "오케스트라지휘자", "박물관큐레이터", "사서", "임상영양사", "물리치료사", "치과의사",
                "한의사", "방사선사", "임상병리사", "요양보호사", "사회복지사", "심리상담사", "공인중개사", "감정평가사", "공인노무사", "변리사",
                "관세사", "법무사", "웹디자이너", "인테리어디자이너", "만화가", "인형극배우", "스턴트맨", "치과위생사", "도배사", "한옥목수",
                "용접공", "조경업자", "환경미화원", "특수경비원", "보석감정사", "속기사", "번역가", "동시통역사", "비서", "은행원",
                "보험설계사", "펀드매니저", "외환딜러", "경매사", "기획자", "마케터", "속전기기사", "배관공", "안무가", "조각가"
        ));

        put("스포츠", Arrays.asList(
                "축구", "농구", "야구", "배구", "테니스", "수영", "태권도", "골프", "탁구", "배드민턴",
                "족구", "핸드볼", "필드하키", "아이스하키", "피겨스케이팅", "쇼트트랙", "스피드스케이팅", "알파인스키", "스노보드", "봅슬레이",
                "컬링", "유도", "레슬링", "복싱", "펜싱", "양궁", "사격", "역도", "기계체조", "리듬체조",
                "100m달리기", "마라톤", "수구", "다이빙", "아티스틱스위밍", "조정", "카누", "요트", "사이클", "경륜",
                "승마", "근대5종", "트라이애슬론", "비치발리볼", "럭비", "미식축구", "크리켓", "소프트볼", "볼링", "당구",
                "포켓볼", "게이트볼", "파크골프", "스쿼시", "라켓볼", "정구", "무에타이", "킥복싱", "종합격투기", "주짓수",
                "공수도", "우슈", "씨름", "스모", "검도", "크로스핏", "필라테스", "요가", "에어로빅", "보디빌딩",
                "등산", "스포츠클라이밍", "바다낚시", "바둑", "체스", "장기", "서핑", "윈드서핑", "수상스키", "웨이크보드",
                "제트스키", "패러글라이딩", "스카이다이빙", "번지점프", "스케이트보드", "인라인스케이트", "롤러스케이트", "전동킥보드", "F1레이싱", "카트레이싱",
                "모터사이클", "마장마술", "장애물물두기", "줄다리기", "피구", "발야구", "티볼", "스피드민턴", "넷볼", "라크로스"
        ));

        put("음식", Arrays.asList(
                "김치찌개", "된장찌개", "부대찌개", "순두부찌개", "청국장", "비빔밥", "소불고기", "삼겹살구이", "갈비찜", "닭볶음탕",
                "삼계탕", "돼지보쌈", "장충동족발", "감자탕", "순대국밥", "콩나물해장국", "국물떡볶이", "찰순대", "모듬튀김", "부산어묵",
                "야채김밥", "신라면", "간짜장", "삼선짬뽕", "찹쌀탕수육", "사골마라탕", "마라샹궈", "꿔바로우", "양꼬치", "소고기쌀국수",
                "팟타이", "나시고랭", "모듬초밥", "돈코츠라멘", "사누끼우동", "메밀소바", "일본식돈카츠", "규동", "타코야끼", "오코노미야끼",
                "안심스테이크", "까르보나라", "페퍼로니피자", "치즈버거", "클럽샌드위치", "닭가슴살샐러드", "멕시칸타코", "비프브리또", "치즈퀘사디아", "인도카레",
                "하이라이스", "옛날돈가스", "오므라이스", "계란볶음밥", "물냉면", "비빔밀면", "바지락칼국수", "김치수제비", "잔치국수", "함흥비빔국수",
                "매콤쫄면", "해물파전", "녹두김치전", "후라이드치킨", "양념치킨", "간장치킨", "매운닭발", "오돌뼈볶음", "소곱창구이", "소대창구이",
                "돼지막창구이", "한우육회", "탕탕산낙지", "모듬물회", "매운탕", "조개구이", "대하소금구이", "풍천장어구이", "춘천닭갈비", "안동찜닭",
                "육개장", "갈비탕", "설렁탕", "도가니탕", "추어탕", "알탕", "꽃게탕", "아구찜", "해물찜", "코다리조림",
                "수육국밥", "선지해장국", "뼈해장국", "평양냉면", "비빔냉면", "쟁반짜장", "고추잡채", "양장피", "깐풍기", "크림새우"
        ));

        put("인물", Arrays.asList(
                "세종대왕", "이순신", "백범김구", "안중근", "유관순", "신사임당", "율곡이이", "퇴계이황", "다산정약용", "장영실",
                "태조왕건", "대조영", "김유신", "계백장군", "연개소문", "광개토대왕", "태조이성계", "정도전", "한석봉", "단원김홍도",
                "혜원신윤복", "의성허준", "원효대사", "도산안창호", "매헌윤봉길", "이봉창", "녹두장군전봉준", "방정환", "백남준", "봉준호",
                "박찬욱", "김연아", "박지성", "손흥민", "류현진", "박찬호", "이승엽", "서장훈", "안정환", "유재석",
                "강호동", "신동엽", "이경규", "가왕조용필", "나훈아", "이미자", "월드스타싸이", "방탄소년단", "블랙핑크", "아이유",
                "임영웅", "조수미", "정명훈", "이병철", "정주영", "알베르트아인슈타인", "아이작뉴턴", "토마스에디슨", "마리퀴리", "갈릴레오갈릴레이",
                "찰스다윈", "스티브잡스", "빌게이츠", "일론머스크", "워런버핏", "에이브러햄링컨", "조지워싱턴", "버락오바마", "도널드트럼프", "나폴레옹",
                "율리우스시저", "알렉산더대왕", "징기스칸", "마하트마간디", "넬슨만델라", "체게바라", "마더테레사", "알베르트슈바이처", "윌리엄셰익스피어", "빈센트반고흐",
                "파블로피카소", "루트비히반베토벤", "볼프강아마데우스모차르트", "레오나르도다빈치", "미켈란젤로", "칭기즈칸", "콜럼버스", "마르코폴로", "조앤롤링", "월트디즈니",
                "마이클잭슨", "비틀즈", "퀸", "엘비스프레슬리", "마릴린먼로", "찰리채플린", "오드리햅번", "톰크루즈", "키아누리브스", "브래드피트"
        ));

        put("영화", Arrays.asList(
                "기생충", "명량", "극한직업", "신과함께", "국제시장", "베테랑", "도둑들", "7번방의선물", "암살", "광해왕이된남자",
                "왕의남자", "부산행", "택시운전사", "변호인", "해운대", "실미도", "태극기휘날리며", "쉬리", "공동경비구역JSA", "올드보이",
                "살인의추억", "추격자", "내부자들", "신세계", "타짜", "범죄와의전쟁", "아저씨", "늑대소년", "건축학개론", "써니",
                "과속스캔들", "한산용의출현", "노량죽음의바다", "서울의봄", "파묘", "곡성", "괴물", "범죄도시", "범죄도시2", "범죄도시3",
                "범죄도시4", "아바타", "겨울왕국", "타이타닉", "어벤져스엔드게임", "아이언맨", "스파이더맨노웨이홈", "해리포터와마법사의돌", "반지의제왕왕의귀환", "스타워즈에피소드4",
                "쥬라기공원", "탑건매버릭", "인셉션", "인터스텔라", "다크나이트", "조커", "라라랜드", "위플래쉬", "매트릭스", "터미네이터2",
                "에일리언", "글래디에이터", "킹스맨시크릿에이전트", "캐리비안의해적", "킹콩", "고질라", "트랜스포머", "미션임파서블", "슈렉", "토이스토리",
                "라이온킹", "알라딘", "미녀와야수", "인어공주", "주토피아", "인사이드아웃", "엘리멘탈", "스즈메의문단속", "너의이름은", "센과치히로의행방불명",
                "이웃집토토로", "하울의움직이는성", "명탐정코난기타", "짱구는못말려기타", "도라에몽기타", "슬램덩크더퍼스트", "레옹", "인디아나존스", "쥬만지", "나홀로집에",
                "포레스트검프", "쇼생크탈출", "인생은아름다워", "글래디에이터2", "라따뚜이", "코코", "겨울왕국2", "주먹왕랄프", "몬스터주식회사", "업"
        ));
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
            // 비밀번호 설정 유무를 보스 정보로 가공해서 전달
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
            // 방 생성자가 보낸 비밀번호를 세션에 안착
            if(msg.getRoomPassword() != null && !msg.getRoomPassword().trim().isEmpty()) {
                session.setRoomPassword(msg.getRoomPassword().trim());
            }
        }

        // [비밀번호 보안 검증 가드]
        if (session.getRoomPassword() != null && !session.getRoomPassword().isEmpty()) {
            if (!session.getRoomPassword().equals(msg.getRoomPassword())) {
                ChatMessage error = new ChatMessage();
                error.setType(ChatMessage.MessageType.SYSTEM);
                error.setContent("ERROR_PASSWORD"); // 약속된 오류 키워드 발송
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
                        if (playerId.equals(session.getHostId()) && !session.getPlayers().isEmpty()) {
                            String newHostId = session.getPlayers().keySet().iterator().next();
                            session.setHostId(newHostId);
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
                if (msg.getPlayerId().equals(session.getHostId()) && !session.getPlayers().isEmpty()) {
                    String newHostId = session.getPlayers().keySet().iterator().next();
                    session.setHostId(newHostId);
                }
                sendPlayerListUpdate(session);
            }
        }
    }

    @MessageMapping("/game.spectatorJoin")
    public void spectatorJoin(ChatMessage msg) {
        GameSession session = sessions.get(msg.getRoomId());
        if (session != null && !session.isPlaying() && session.getSpectators().containsKey(msg.getPlayerId())) {
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
        session.setBaboMode(msg.isBaboMode());

        List<String> categories = new ArrayList<>(gameDictionary.keySet());
        String selectedCategory = categories.get(new Random().nextInt(categories.size()));
        List<String> words = gameDictionary.get(selectedCategory);
        String selectedWord = words.get(new Random().nextInt(words.size()));

        List<String> playerIds = new ArrayList<>(session.getPlayers().keySet());
        String liarId = playerIds.get(new Random().nextInt(playerIds.size()));

        session.setWord(selectedWord);
        session.setLiarId(liarId);
        session.setPlaying(true);

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
            ChatMessage expMsg = new ChatMessage();
            expMsg.setType(ChatMessage.MessageType.EXPLANATION);
            expMsg.setSender(msg.getSender());
            expMsg.setContent(msg.getContent());
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
            ChatMessage waitMsg = new ChatMessage();
            waitMsg.setType(ChatMessage.MessageType.WAITING_FOR_VOTE);
            waitMsg.setContent("👀 모든 설명 종료! 추리 완료 후 방장이 수동으로 투표를 시작하세요.");
            messagingTemplate.convertAndSend("/topic/room/" + roomId, waitMsg);
            return;
        }

        String currentTurnId = session.getTurnOrder().get(session.getCurrentTurnIndex());
        String currentTurnName = session.getPlayers().get(currentTurnId);

        ChatMessage turnMsg = new ChatMessage();
        turnMsg.setType(ChatMessage.MessageType.TURN_START);
        turnMsg.setTurnPlayerId(currentTurnId);
        turnMsg.setTurnPlayerName(currentTurnName);
        turnMsg.setContent("🗣️ [" + currentTurnName + "] 님의 설명 차례!");
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
        session.cancelTimer();
        session.getPublicVotes().clear();

        List<String> unvotedList = new ArrayList<>();
        for (String pName : session.getPlayers().values()) {
            if (!pName.equals(session.getAccusedFullName())) unvotedList.add(pName);
        }
        String unvotedStr = String.join(", ", unvotedList);

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
        sendPlayerListUpdate(session);

        if (session.getCurrentRound() >= session.getTotalRounds()) {
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
            session.setCurrentRound(session.getCurrentRound() + 1);
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