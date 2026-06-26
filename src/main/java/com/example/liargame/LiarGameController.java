package com.example.liargame; // 본인의 패키지 경로와 일치해야 합니다.

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Controller
public class LiarGameController {

    // 1. 메인 홈 화면
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // 2. 방 생성 처리
    @PostMapping("/create-room")
    public String createRoom() {
        // 무작위 영어+숫자 조합 6자리 방 코드 생성 (예: A1B2C3)
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 생성된 방 주소로 사용자를 즉시 이동(리다이렉트)시킵니다.
        return "redirect:/room/" + roomId;
    }

    // 3. 대기실 화면
    @GetMapping("/room/{roomId}")
    public String room(@PathVariable String roomId, Model model) {
        // URL에 있는 방 코드({roomId})를 읽어서 HTML 화면으로 넘겨줍니다.
        model.addAttribute("roomId", roomId);
        return "room";
    }
}