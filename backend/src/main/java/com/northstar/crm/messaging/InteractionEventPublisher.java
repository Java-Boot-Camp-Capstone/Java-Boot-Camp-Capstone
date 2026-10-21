
package com.northstar.crm.messaging;

import com.northstar.crm.domain.Interaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InteractionEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(InteractionEventPublisher.class);

    public static final String DEFAULT_TOPIC =
            "customer.interaction.recorded";

    private final KafkaTemplate<String, CustomerInteractionRecordedV1>
            kafkaTemplate;
    private final String topic;

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\r', '_').replace('\n', '_');
    }

    public InteractionEventPublisher() {
        this(null, DEFAULT_TOPIC);
    }

    public InteractionEventPublisher(
            KafkaTemplate<String, CustomerInteractionRecordedV1> kafkaTemplate,
            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(Interaction interaction, String customerPublicId) {
        CustomerInteractionRecordedV1 event =
                new CustomerInteractionRecordedV1(
                        CustomerInteractionRecordedV1.TYPE,
                        CustomerInteractionRecordedV1.VERSION,
                        interaction.getId(),
                        customerPublicId,
                        interaction.getInteractionType(),
                        interaction.getCorrelationId(),
                        interaction.getCreatedAt());

        if (kafkaTemplate == null) {
            log.info(
                    "Kafka not configured, skipping publish of {} customerId={} correlationId={}",
                    CustomerInteractionRecordedV1.TYPE,
                    event.customerId(),
                    sanitizeForLog(event.correlationId()));
            return;
        }

        kafkaTemplate.send(topic, event.customerId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error(
                                "Failed to publish {} customerId={} correlationId={} error={}",
                                CustomerInteractionRecordedV1.TYPE,
                                event.customerId(),
                                sanitizeForLog(event.correlationId()),
                                error);
                    } else {
                        log.info(
                                "Published {} customer={} correlation={} topic={}",
                                CustomerInteractionRecordedV1.TYPE,
                                event.customerId(),
                                sanitizeForLog(event.correlationId()),
                                topic);
                    }
                });
    }
}
