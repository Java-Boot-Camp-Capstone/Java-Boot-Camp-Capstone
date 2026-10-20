package com.northstar.crm.api.dto;

import com.northstar.crm.domain.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * TODO: Align field names with Lab 48 contracts. Session block: add Bean Validation
 * ({@code @NotBlank}, {@code @Size}) on customerId / summary / interactionType.
 */
public record CreateInteractionRequest(
        // TODO: @NotBlank on customerId
        @NotBlank String customerId,
        // TODO: constrain interactionType (e.g. CALL, EMAIL, NOTE)
        @NotNull InteractionType interactionType,
        // TODO: @NotBlank @Size(max = ...)
        @NotBlank @Size(max = 1000) String summary,
        String correlationId
) {}