package cn.lut.imserver.config;

import cn.lut.imserver.handle.WebSocketHandle;
import cn.lut.imserver.util.interceptor.JwtWebsocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {
    @Autowired
    private JwtWebsocketInterceptor jwtWebsocketInterceptor;

    @Autowired
    WebSocketHandle webSocketHandle; // 需要使用 spring 的 bean 管理

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandle, "/ws")
                .addInterceptors(jwtWebsocketInterceptor)
                .setAllowedOrigins("*");
    }
}
