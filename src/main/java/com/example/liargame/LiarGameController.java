package com.example.liargame; // 본인의 패키지 경로와 일치해야 합니다.

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LiarGameController {

    // 사용자가 http://localhost:8080/ 으로 접속했을 때 실행됩니다.
    @GetMapping("/")
    public String home() {
        // resources/templates/index.html 파일을 보여주라는 의미입니다.
        return "index";
    }
}