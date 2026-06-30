package com.example.liargame; // ⚠️ 본인의 패키지명에 맞게 수정하세요 (예: com.example.liargame)

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest; // Java 17, Spring Boot 3.x 기준
import java.util.Map;

public class IpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 레일웨이 같은 클라우드 환경에서 진짜 IP를 찾는 로직
            String ip = servletRequest.getHeader("X-Forwarded-For");

            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = servletRequest.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = servletRequest.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = servletRequest.getRemoteAddr();
            }

            // X-Forwarded-For에 여러 IP가 찍힐 경우 맨 앞의 진짜 IP만 추출
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            // 추출한 IP를 웹소켓 세션에 저장
            attributes.put("ipAddress", ip);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}