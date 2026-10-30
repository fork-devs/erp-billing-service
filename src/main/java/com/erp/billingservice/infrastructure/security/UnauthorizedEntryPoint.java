package com.erp.billingservice.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "type", URI.create("about:blank"),
                "title", "Authentication Required",
                "status", HttpStatus.UNAUTHORIZED.value(),
                "detail", "Full authentication is required to access this resource",
                "instance", request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
