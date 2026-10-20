package com.northstar.crm.api.dto;

import com.northstar.crm.domain.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ({@code @NotBlank}, {@code @Size}) on customerId / summary / interactionType.
 */
public record CreateInteractionRequest(
        @NotBlank String customerId,
        @NotNull InteractionType interactionType,
        @NotBlank @Size(max = 1000) String summary,
        String correlationId
) {}