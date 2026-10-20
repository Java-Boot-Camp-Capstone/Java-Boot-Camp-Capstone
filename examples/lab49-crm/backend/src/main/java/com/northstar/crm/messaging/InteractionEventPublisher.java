package com.northstar.crm.messaging;

import com.northstar.crm.domain.Interaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InteractionEventPublisher {

    /**
     * Session stub: log or no-op. Full path: KafkaTemplate + topic from ADR-002.
     */

    private static final Logger log = LoggerFactory.getLogger(InteractionEventPublisher.class);
    public static final String DEFAULT_TOPIC ="customer.interaction.recorded";

    private final KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate;
    private final String topic;

    public InteractionEventPublisher() {
        this(null, DEFAULT_TOPIC);
    }

    public InteractionEventPublisher(KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(Interaction interaction) {
        // TODO: build CustomerInteractionRecordedV1 and publish to Kafka
        CustomerInteractionRecordedV1 event = new CustomerInteractionRecordedV1(
                CustomerInteractionRecordedV1.TYPE,
                CustomerInteractionRecordedV1.VERSION,
                interaction.getId(),
                interaction.getCustomerId(),
                interaction.getInteractionType(),
                interaction.getCorrelationId(),
                interaction.getCreatedAt());

        if (kafkaTemplate == null) {
            log.info("Kafka not configured, skipping publish of {} customerId={} correlationId={}",
                    CustomerInteractionRecordedV1.TYPE, event.customerId(), event.correlationId());
            return;
        }

        kafkaTemplate.send(topic, event.customerId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {} customerId={} correlationId={} error={}",
                                CustomerInteractionRecordedV1.TYPE,
                                event.customerId(),
                                event.correlationId(),
                                error);
                    } else {
                        log.info("Published {} customer={} correlation={} topic={}",
                                CustomerInteractionRecordedV1.TYPE,
                                event.customerId(),
                                event.correlationId(),
                                topic);
                    }
                });
    }
}