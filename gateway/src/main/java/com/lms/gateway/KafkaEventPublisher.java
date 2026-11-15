package com.lms.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.shared.AppException;
import com.lms.shared.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
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
                        log.error("Операция уже сохранена, событие {} не доставлено", event.getClass().getSimpleName());
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
            throw AppException.unavailable("Событие не опубликовано");
        }
    }

    private void send(String id, Object event) {
        String type = event.getClass().getSimpleName();
        try {
            String body = json.writeValueAsString(new EventMessage(id, type, json.valueToTree(event)));
            kafka.send(TOPIC, type, body).get();
            log.info("Kafka {} опубликовано, id {}", type, id);
        } catch (Exception ex) {
            log.error("Kafka {} не опубликовано, id {}", type, id, ex);
            throw new IllegalStateException("Не удалось опубликовать событие " + type, ex);
        }
    }

    public record EventMessage(String id, String type, JsonNode body) {
    }
}
