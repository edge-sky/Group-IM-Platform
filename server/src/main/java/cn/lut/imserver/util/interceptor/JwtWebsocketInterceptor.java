package cn.lut.imserver.util.interceptor;

import cn.lut.imserver.util.JWTUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

@Component
@Slf4j
public class JwtWebsocketInterceptor implements HandshakeInterceptor {
    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler,
                                   @NotNull Map<String, Object> attributes) throws Exception {
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("Authorization");
        }

        // If the request is an OPTIONS request, allow it (CORS preflight)
        // For WebSockets, OPTIONS check might not be standard, but keeping similar logic
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            log.info("WebSocket preflight request received, allowing it.");
            response.setStatusCode(HttpStatus.OK);
            return false;
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty in WebSocket handshake.");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", 401);
            jsonObject.put("message", "token is null");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getBody().write(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
            return false;
        }

        Jws<Claims> jws = jwtUtil.parseToken(token);

        if (jws == null) {
            log.warn("Token: {} is unauthorized in WebSocket handshake.", token);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", 401);
            jsonObject.put("message", "token is unauthorized");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getBody().write(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
            return false;
        }

        // Store user information in attributes, which will be available in the WebSocketSession
        attributes.put("uid", jws.getPayload().get("uid"));
        attributes.put("username", jws.getPayload().get("username"));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {

    }
}

