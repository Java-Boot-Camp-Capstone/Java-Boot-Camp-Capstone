package com.northstar.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Issues short-lived HS256 JWTs for authenticated demo users. */
@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;

    public JwtTokenService(JwtEncoder encoder,
                           @Value("${app.security.jwt.issuer}") String issuer,
                           @Value("${app.security.jwt.ttl-minutes:30}") long ttlMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public LoginResponse issue(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();
        return issue(authentication.getName(), roles);
    }

    public LoginResponse issue(String subject, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("roles", roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", ttl.toSeconds(), roles);
    }
}
