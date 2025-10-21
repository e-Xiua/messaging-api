package com.iwellness.messaging.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * CORS Configuration Properties
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsConfigurationProperties {
    
    private List<String> allowedOrigins;
    
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }
    
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
