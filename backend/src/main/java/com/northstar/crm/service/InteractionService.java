
package com.northstar.crm.service;

import com.northstar.crm.api.dto.CreateInteractionRequest;
import com.northstar.crm.api.dto.InteractionResponse;
import com.northstar.crm.domain.Interaction;
import com.northstar.crm.messaging.InteractionEventPublisher;
import com.northstar.crm.repo.InteractionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class InteractionService {

    private static final String DEFAULT_CORRELATION_ID =
            "lab-request-001";

    private final InteractionRepository interactionRepository;
    private final InteractionEventPublisher eventPublisher;

    public InteractionService(
            InteractionRepository interactionRepository,
            InteractionEventPublisher eventPublisher) {
        this.interactionRepository = interactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public InteractionResponse create(
            CreateInteractionRequest request,
            String correlationHeader) {

        String correlationId = resolveCorrelationId(
                correlationHeader,
                request.correlationId());

        if (!interactionRepository.customerExists(
                request.customerId())) {
            throw new UnknownCustomerException(
                    request.customerId());
        }

        Interaction interaction = new Interaction();
        interaction.setCustomerId(request.customerId());
        interaction.setInteractionType(request.interactionType());
        interaction.setSummary(request.summary());
        interaction.setCorrelationId(correlationId);
        interaction.setCreatedAt(Instant.now());

        Interaction saved = interactionRepository.save(interaction);

        eventPublisher.publish(saved);

        return toResponse(saved);
    }

    public List<InteractionResponse> getAll() {
        return interactionRepository.findAll()
                .stream()
                .map(InteractionService::toResponse)
                .toList();
    }

    private static String resolveCorrelationId(
            String header,
            String bodyValue) {

        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        if (bodyValue != null && !bodyValue.isBlank()) {
            return bodyValue.trim();
        }

        return DEFAULT_CORRELATION_ID;
    }

    private static InteractionResponse toResponse(
            Interaction interaction) {

        return new InteractionResponse(
                interaction.getId(),
                interaction.getCustomerId(),
                interaction.getInteractionType(),
                interaction.getSummary(),
                interaction.getCorrelationId(),
                interaction.getCreatedAt());
    }
}
