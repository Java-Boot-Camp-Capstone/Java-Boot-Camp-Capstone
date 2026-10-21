package com.northstar.crm.security;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Training-environment token endpoint: exchanges demo credentials for a short-lived JWT.
 * In a real enterprise this is replaced by the corporate IdP (OIDC) - the API would only validate tokens.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            log.info("login success user={}", auth.getName());
            return ResponseEntity.ok(tokenService.issue(auth));
        } catch (AuthenticationException e) {
            // Same message for unknown user and bad password (no user enumeration). Password never logged.
            // strip CR/LF so a crafted username cannot forge log lines (log injection, CWE-117)
            log.warn("login failed user={}", request.username().replaceAll("[\\r\\n]", "_"));
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }
    }
}
