package com.northstar.crm.messaging;

import com.northstar.crm.domain.InteractionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Versioned event contract stub.
 */
public record CustomerInteractionRecordedV1(
        String eventType,
        String eventVersion,
        UUID interactionId,
        String customerId,
        InteractionType interactionType,
        String correlationId,
        Instant occurredAt
) {
    public static final String TYPE = "CustomerInteractionRecorded";
    public static final String VERSION = "1";
}