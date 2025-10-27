package com.iwellness.messaging.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Websocket websocket = new Websocket();
    private Rabbitmq rabbitmq = new Rabbitmq();
    private Messaging messaging = new Messaging();

    // Getters y Setters para todos los campos

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Websocket getWebsocket() { return websocket; }
    public void setWebsocket(Websocket websocket) { this.websocket = websocket; }
    public Rabbitmq getRabbitmq() { return rabbitmq; }
    public void setRabbitmq(Rabbitmq rabbitmq) { this.rabbitmq = rabbitmq; }
    public Messaging getMessaging() { return messaging; }
    public void setMessaging(Messaging messaging) { this.messaging = messaging; }

    // Clases anidadas para representar la estructura del YML

    public static class Jwt {
        private String secret;
        private long expiration;
        // Getters y Setters
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
    }

    public static class Websocket {
        private List<String> allowedOrigins;
        private String endpoint;
        private String topicPrefix;
        private String appPrefix;
        // Getters y Setters
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getTopicPrefix() { return topicPrefix; }
        public void setTopicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; }
        public String getAppPrefix() { return appPrefix; }
        public void setAppPrefix(String appPrefix) { this.appPrefix = appPrefix; }
    }

    public static class Rabbitmq {
        private Exchange exchange;
        private Map<String, String> routingKeys;
        // Getters y Setters
        public Exchange getExchange() { return exchange; }
        public void setExchange(Exchange exchange) { this.exchange = exchange; }
        public Map<String, String> getRoutingKeys() { return routingKeys; }
        public void setRoutingKeys(Map<String, String> routingKeys) { this.routingKeys = routingKeys; }
    }

    public static class Exchange {
        private String name;
        // Getters y Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Messaging {
        private int maxMessageLength;
        private Pagination pagination;
        // Getters y Setters
        public int getMaxMessageLength() { return maxMessageLength; }
        public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }
        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    public static class Pagination {
        private int defaultSize;
        private int maxSize;
        // Getters y Setters
        public int getDefaultSize() { return defaultSize; }
        public void setDefaultSize(int defaultSize) { this.defaultSize = defaultSize; }
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }
}
