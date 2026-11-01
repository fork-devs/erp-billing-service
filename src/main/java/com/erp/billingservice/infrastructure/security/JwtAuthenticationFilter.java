package com.erp.billingservice.infrastructure.security;

import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwksKeyManager jwksKeyManager;

    public JwtAuthenticationFilter(JwksKeyManager jwksKeyManager) {
        this.jwksKeyManager = jwksKeyManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String tenantHeader = request.getHeader("X-Tenant-ID");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            String keyId = signedJwt.getHeader().getKeyID();
            RSAPublicKey publicKey = jwksKeyManager.getPublicKey(keyId);

            if (publicKey == null || !signedJwt.verify(new RSASSAVerifier(publicKey))) {
                log.warn("Invalid JWT signature or key not found: keyId={}", keyId);
                filterChain.doFilter(request, response);
                return;
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                log.warn("Expired JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            String tenantClaim = claims.getStringClaim("tenant_id");
            if (tenantClaim == null || tenantClaim.isBlank()) {
                log.warn("JWT missing tenant_id claim");
                filterChain.doFilter(request, response);
                return;
            }

            UUID tokenTenantId = UUID.fromString(tenantClaim);

            // Contrast X-Tenant-ID header if present
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                UUID headerTenantId = UUID.fromString(tenantHeader.trim());
                if (!tokenTenantId.equals(headerTenantId)) {
                    log.warn("X-Tenant-ID mismatch: header={}, token={}", headerTenantId, tokenTenantId);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant header does not match authenticated token");
                    return;
                }
            }

            TenantContextHolder.setTenantId(tokenTenantId);

            List<String> roles = claims.getStringListClaim("roles");
            List<SimpleGrantedAuthority> authorities = (roles != null)
                    ? roles.stream().map(SimpleGrantedAuthority::new).toList()
                    : List.of();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("Error processing JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
