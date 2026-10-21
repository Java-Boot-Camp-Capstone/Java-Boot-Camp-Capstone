package com.northstar.crm.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security: Spring Boot validates JWTs and enforces role-based access (architecture.md).
 *
 * <ul>
 *   <li>Anonymous request to /api/** -> 401</li>
 *   <li>Authenticated, wrong role -> 403</li>
 *   <li>AGENT or MANAGER -> allowed</li>
 *   <li>Anything not explicitly listed -> denied</li>
 * </ul>
 * Angular is NOT the security boundary - every rule here is enforced server-side.
 */
@Configuration
public class SecurityConfig {

    public static final String ROLE_AGENT = "AGENT";
    public static final String ROLE_MANAGER = "MANAGER";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                // Stateless bearer-token API: no cookies/sessions, so CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ROLE_AGENT, ROLE_MANAGER)
                        .requestMatchers(HttpMethod.POST, "/api/v1/interactions").hasAnyRole(ROLE_AGENT, ROLE_MANAGER)
                        // deny-by-default: new endpoints must be added above on purpose
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Maps the JWT "roles" claim (e.g. ["AGENT"]) to Spring authorities (ROLE_AGENT). */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecretKey jwtSigningKey(@Value("${app.security.jwt.secret:}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be set and at least 32 characters (HS256). Never commit it.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, @Value("${app.security.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // validates exp/nbf (with default clock skew) AND that we issued the token
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Synthetic demo accounts for the training environment. Passwords come from env/secrets only. */
    @Bean
    UserDetailsService demoUsers(PasswordEncoder encoder,
                                 @Value("${app.security.demo-users.agent-password:}") String agentPassword,
                                 @Value("${app.security.demo-users.manager-password:}") String managerPassword) {
        if (agentPassword.isBlank() || managerPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_DEMO_AGENT_PASSWORD and APP_DEMO_MANAGER_PASSWORD must be set (env / secret).");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername("agent").password(encoder.encode(agentPassword)).roles(ROLE_AGENT).build(),
                User.withUsername("manager").password(encoder.encode(managerPassword)).roles(ROLE_MANAGER).build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
