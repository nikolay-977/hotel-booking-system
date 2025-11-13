package com.example.bookingservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return requestTemplate -> {
            try {
                ServletRequestAttributes requestAttributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (requestAttributes != null) {
                    HttpServletRequest request = requestAttributes.getRequest();
                    String authorizationHeader = request.getHeader("Authorization");

                    if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                        requestTemplate.header("Authorization", authorizationHeader);
                        System.out.println("Feign: Added Authorization header: " + authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())) + "...");
                    } else {
                        System.out.println("Feign: No Authorization header found");
                    }
                }
            } catch (Exception e) {
                System.err.println("Feign: INFO setting Authorization header: " + e.getMessage());
            }
        };
    }
}