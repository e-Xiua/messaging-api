package com.iwellness.messaging.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor de Feign que propaga el header Authorization
 * desde la petición original hacia las llamadas a otros microservicios.
 */
@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Propagar el token JWT
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authorizationHeader != null) {
                requestTemplate.header(AUTHORIZATION_HEADER, authorizationHeader);
                log.debug("Propagating Authorization header to Feign client");
            } else {
                log.warn("No Authorization header found in request context");
            }
            
            // Propagar el User ID (opcional, por si lo necesitas)
            String userIdHeader = request.getHeader(USER_ID_HEADER);
            if (userIdHeader != null) {
                requestTemplate.header(USER_ID_HEADER, userIdHeader);
                log.debug("Propagating X-User-Id header to Feign client");
            }
        } else {
            log.warn("No request attributes available - cannot propagate headers");
        }
    }
}
