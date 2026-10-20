package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.Interaction;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.InteractionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.northstar.crm.api.InteractionController.resolveCorrelationId;

@Service
public class InteractionService {

    private static final String DEFAULT_CORRELATION_ID = "lab-request-001";

    private final InteractionRepository interactionRepository;
    private final InteractionEventPublisher eventPublisher;

    public InteractionService(
            InteractionRepository interactionRepository, InteractionEventPublisher eventPublisher) {
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Session block: implement create flow. Full path: @Transactional + Kafka + Problem Details.
     */
    @Transactional
    public InteractionResponse create(CreateInteractionRequest request, String correlationHeader) {

        // TODO 1: resolve correlationId (header > request > "lab-request-001")
        String correlationId = resolveCorrelationId(correlationHeader, request.correlationId());

        // TODO 2: reject unknown customer (CUS-9999 / missing) — throw domain exception
        if (!interactionRepository.customerExists(request.customerId())) {
            throw new UnknownCustomerException(request.customerId());
        }

        // TODO 3: map request → Interaction, set createdAt = Instant.now()
        Interaction interaction = new Interaction();
        interaction.setCustomerId(request.customerId());
        interaction.setInteractionType(request.interactionType());
        interaction.setSummary(request.summary());
        interaction.setCorrelationId(correlationId);
        interaction.setCreatedAt(Instant.now());

        // TODO 4: save via repository
        Interaction saved = interactionRepository.save(interaction);

        // TODO 5: publish event
        eventPublisher.publish(saved);

        // TODO 6: return InteractionResponse
        return toResponse(saved);
    }

    private static String resolveCorrelationId(String header, String bodyValue) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (bodyValue != null && !bodyValue.isBlank()) {
            return  bodyValue.trim();
        }
        return DEFAULT_CORRELATION_ID;
    }

    private static InteractionResponse toResponse(Interaction interaction) {
        return new InteractionResponse(
                interaction.getId(),
                interaction.getCustomerId(),
                interaction.getInteractionType(),
                interaction.getSummary(),
                interaction.getCorrelationId(),
                interaction.getCreatedAt());
    }
}