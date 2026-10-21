package com.northstar.crm.security;

import com.northstar.crm.api.CustomerController;
import com.northstar.crm.api.InteractionController;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.InteractionType;
import com.northstar.crm.repo.CustomerRepository;
import com.northstar.crm.service.InteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 51 AuthorizationTests: proves deny-by-default (401 / 403 / 200) on the real security filter chain.
 * Test-only synthetic secrets below - never reuse outside tests.
 */
@WebMvcTest(controllers = {CustomerController.class, InteractionController.class, AuthController.class})
@Import({SecurityConfig.class, JwtTokenService.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-only-secret-key-that-is-at-least-32-bytes",
        "app.security.jwt.issuer=northstar-crm",
        "app.security.demo-users.agent-password=test-agent-pw",
        "app.security.demo-users.manager-password=test-manager-pw"
})
class AuthorizationTests {

    private static final String CORRELATION = "lab-request-001";
    private static final String VALID_INTERACTION =
            "{\"customerId\":\"CUS-1001\",\"interactionType\":\"CALL\",\"summary\":\"Billing follow-up\"}";

    @Autowired MockMvc mvc;
    @Autowired JwtTokenService tokens;

    @MockitoBean CustomerRepository customerRepository;
    @MockitoBean InteractionService interactionService;

    private String bearer(String... roles) {
        return "Bearer " + tokens.issue("test-user", List.of(roles)).token();
    }

    @Test
    void anonymousCustomerSearch_is401() throws Exception {
        mvc.perform(get("/api/customers").header("X-Correlation-Id", CORRELATION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCreateInteraction_is401() throws Exception {
        mvc.perform(post("/api/v1/interactions").contentType(MediaType.APPLICATION_JSON).content(VALID_INTERACTION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRole_is403() throws Exception {
        mvc.perform(get("/api/customers").header("Authorization", bearer("AUDITOR")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/interactions").header("Authorization", bearer("AUDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_INTERACTION))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanSearchCustomers_is200() throws Exception {
        when(customerRepository.findAllByOrderByCustomerIdAsc()).thenReturn(List.of());
        mvc.perform(get("/api/customers").header("Authorization", bearer("AGENT")))
                .andExpect(status().isOk());
    }

    @Test
    void agentCanRecordInteraction_is201() throws Exception {
        when(interactionService.create(any(), anyString())).thenReturn(new InteractionResponse(
                UUID.randomUUID(), "CUS-1001", InteractionType.CALL, "Billing follow-up", CORRELATION, Instant.now()));
        mvc.perform(post("/api/v1/interactions").header("Authorization", bearer("AGENT"))
                        .header("X-Correlation-ID", CORRELATION)
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_INTERACTION))
                .andExpect(status().isCreated());
    }

    @Test
    void tamperedToken_is401() throws Exception {
        String token = bearer("AGENT");
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        mvc.perform(get("/api/customers").header("Authorization", tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKey_is401() throws Exception {
        // Same claims/issuer, but signed with an attacker's key -> signature check must reject it
        javax.crypto.SecretKey attackerKey = new javax.crypto.spec.SecretKeySpec(
                "attacker-controlled-key-also-32-bytes-long!".getBytes(), "HmacSHA256");
        String foreign = new JwtTokenService(
                new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
                        new com.nimbusds.jose.jwk.source.ImmutableSecret<>(attackerKey)),
                "northstar-crm", 30).issue("mallory", List.of("MANAGER")).token();
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithValidCredentials_returnsUsableToken() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"agent\",\"password\":\"test-agent-pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("AGENT"))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        when(customerRepository.findAllByOrderByCustomerIdAsc()).thenReturn(List.of());
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithBadPassword_is401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"agent\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unlistedEndpoint_isDeniedByDefault() throws Exception {
        mvc.perform(post("/api/customers").header("Authorization", bearer("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
