package com.iwellness.messaging.interceptor;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.iwellness.messaging.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                
                attributes.put("userId", userId);
                attributes.put("username", username);
                
                log.info("WebSocket handshake exitoso para usuario: {}", username);
                return true;
            }
        }
        
        log.warn("WebSocket handshake fallido: token inválido o ausente");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No se necesita lógica post-handshake
    }
}
