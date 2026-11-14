package com.lms.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.shared.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    static final String TOPIC = "lms.events";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final EventDrain drain;

    @Override
    public void publish(Object event) {
        String id = UUID.randomUUID().toString();
        drain.published(id);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        send(id, event);
                    } catch (RuntimeException ex) {
                        drain.finished(id);
                        throw ex;
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        drain.finished(id);
                    }
                }
            });
            return;
        }
        try {
            send(id, event);
        } catch (RuntimeException ex) {
            drain.finished(id);
            throw ex;
        }
    }

    private void send(String id, Object event) {
        try {
            String body = json.writeValueAsString(new EventMessage(id, event.getClass().getSimpleName(), json.valueToTree(event)));
            kafka.send(TOPIC, event.getClass().getSimpleName(), body).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось опубликовать событие " + event.getClass().getSimpleName(), ex);
        }
    }

    public record EventMessage(String id, String type, JsonNode body) {
    }
}
