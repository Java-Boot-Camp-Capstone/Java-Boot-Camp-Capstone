
package com.northstar.crm.api;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/interactions")
public class InteractionController {

    static final String DEFAULT_CORRELATION_ID = "lab-request-001";

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping
    public List<InteractionResponse> getAll() {
        return interactionService.getAll();
    }

    @PostMapping
    public ResponseEntity<InteractionResponse> create(
            @Valid @RequestBody CreateInteractionRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false)
            String correlationHeader) {

        InteractionResponse body =
                interactionService.create(request, correlationHeader);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public static String resolveCorrelationId(
            String header, String bodyValue) {

        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        if (bodyValue != null && !bodyValue.isBlank()) {
            return bodyValue.trim();
        }

        return DEFAULT_CORRELATION_ID;
    }
}
