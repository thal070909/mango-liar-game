# 🥭 망고의 라이어 게임 (Mango's Liar Game)

> 저희 소중한 반려묘 "망고" 의 이름에서 따온 웹게임입니다.


> "부장님 죄송합니다. 저희 지금 심각한 회의 중입니다."
> **친구들끼리 합법적(?)으로 월급루팡 하려고 만든 실시간 웹 라이어 게임**

사무실 모니터에 띄워놔도 위화감 제로! 눈치 싸움과 정치질이 난무하는 텍스트 기반 라이어 게임입니다. 링크 하나만 공유하면 최대 10명까지 별도의 설치 없이 즉시 게임을 즐길 수 있습니다.

## 🛠️ 기술 스택
* **Language:** Java 17
* **Framework:** Spring Boot 3.x
* **WebSocket:** STOMP / SockJS
* **Frontend:** HTML5, CSS3, Vanilla JavaScript
* **Deployment:** Railway (Nixpacks)

## ✨ 핵심 기능 (월급루팡 최적화)
* 📊 **완벽한 위장술 (엑셀 테마 지원):** 앗, 부장님이 다가오시나요? 클릭 한 번으로 게임 화면이 지루한 엑셀 스프레드시트로 변신합니다. (다크 모드 기본 지원)
* ⚡ **실시간 웹소켓 핑퐁:** STOMP/SockJS 기반의 빠릿빠릿한 실시간 동기화로 숨 막히는 템포를 유지합니다.
* 🤪 **바보 라이어 모드:** 라이어에게 아예 백지 대신 '비슷하지만 다른 단어'를 쥐여주어 혼돈을 극대화하는 꿀잼 모드입니다.
* 🛡️ **강력한 탈주 닌자 방어:** 중간에 누군가 창을 닫아도 탈주자의 턴은 0.1초 만에 즉시 스킵하고, 남은 인원끼리 끊김 없이 게임이 굴러갑니다.
* 👀 **팝콘 뜯는 관전자 모드:** 늦게 온 친구도 소외감 없이! 관전자에게는 몰래 진짜 정답이 공개되어 허공에 헛발질하는 친구들을 구경하는 재미를 선사합니다.

## 🎮 게임 진행 방식
1. **방 생성 및 입장:** 방장이 방을 파고 코드를 공유합니다. (비밀번호 설정 가능)
2. **역할 부여:** 1명의 라이어와 다수의 시민. 시민은 제시어를, 라이어는 '???'(또는 바보 단어)를 받습니다.
3. **단어 설명:** 돌아가면서 제시어에 대해 텍스트로 설명합니다. (1~2턴 설정 가능)
4. **라이어 투표:** 30초 안에 가장 의심스러운 사람을 지목합니다.
5. **최후 변론 및 찬반 투표:** 최다 득표자는 변론을 하고, 나머지 인원이 처형 여부를 결정합니다.
6. **라이어의 마지막 발악:** 처형되더라도 라이어가 시민들의 설명을 바탕으로 '진짜 제시어'를 맞추면 대역전승!

## 🌐 WebSocket 통신 명세서

| 기능 | Action | 발신 (Send URL) | 수신 (Subscribe URL) |
| :--- | :---: | :--- | :--- |
| **방 입장** | `JOIN` | `/app/chat.join` | `/topic/room/{roomId}`, `/topic/player/{playerId}` |
| **채팅 전송** | `CHAT` | `/app/chat.sendMessage` | `/topic/room/{roomId}` |
| **게임 시작** | `GAME_START` | `/app/game.start` | `/topic/room/{roomId}`, `/topic/player/{playerId}` |
| **단어 설명** | `EXPLAIN` | `/app/game.explain` | `/topic/room/{roomId}` |
| **라이어 지목**| `VOTE` | `/app/game.vote` | `/topic/room/{roomId}` |
| **최후 변론** | `DEFEND` | `/app/game.defend` | `/topic/room/{roomId}` |
| **찬반 투표** | `PUBLIC_VOTE`| `/app/game.publicVote` | `/topic/room/{roomId}` |
| **정답 유추** | `LIAR_GUESS` | `/app/game.liarGuess` | `/topic/room/{roomId}` |
| **방 퇴장** | `LEAVE` | `/app/chat.leave` | `/topic/room/{roomId}` |

---
*💡 본 프로젝트는 업무 스트레스를 줄이고 팀워크(를 가장한 정치질)를 다지기 위해 제작되었습니다.*
