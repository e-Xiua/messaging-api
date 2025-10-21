package com.iwellness.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.iwellness.messaging.interceptor.JwtHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final AppProperties appProperties;

    // Inyecta el bean de propiedades a través del constructor
    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor, AppProperties appProperties) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.appProperties = appProperties;
    }

    @Override
    public void configureMessageBroker(@org.springframework.lang.NonNull MessageBrokerRegistry config) {
        AppProperties.Websocket websocketProps = appProperties.getWebsocket();
        
        config.enableSimpleBroker(websocketProps.getTopicPrefix(), "/queue");
        config.setApplicationDestinationPrefixes(websocketProps.getAppPrefix());
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@org.springframework.lang.NonNull StompEndpointRegistry registry) {
        AppProperties.Websocket websocketProps = appProperties.getWebsocket();
        
        // Convierte la lista a un array para el método setAllowedOrigins
        String[] allowedOrigins = websocketProps.getAllowedOrigins().toArray(new String[0]);

        registry.addEndpoint(websocketProps.getEndpoint())
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
        
        registry.addEndpoint(websocketProps.getEndpoint())
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(jwtHandshakeInterceptor);
    }
}
