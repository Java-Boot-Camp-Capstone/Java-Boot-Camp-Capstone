package com.northstar.crm.api.dto;

import com.northstar.crm.domain.InteractionType;

import java.time.Instant;
import java.util.UUID;

public record InteractionResponse(
        UUID id,
        String customerId,
        InteractionType interactionType,
        String summary,
        String correlationId,
        Instant createdAt
) {}