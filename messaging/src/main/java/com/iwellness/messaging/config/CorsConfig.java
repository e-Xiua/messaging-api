package com.iwellness.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CorsConfigurationProperties corsProperties;

    public CorsConfig(CorsConfigurationProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins() != null ? 
            corsProperties.getAllowedOrigins().toArray(new String[0]) : 
            new String[]{"http://localhost:4200", "http://localhost:3000"};
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

}
