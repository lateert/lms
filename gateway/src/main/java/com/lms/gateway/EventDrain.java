package com.lms.gateway;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class EventDrain {
    private final Set<String> pending = new HashSet<>();

    public synchronized void published(String id) {
        pending.add(id);
    }

    public synchronized void finished(String id) {
        pending.remove(id);
        notifyAll();
    }

    public synchronized void awaitSettled() {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!pending.isEmpty()) {
            long left = deadline - System.nanoTime();
            if (left <= 0) {
                throw new IllegalStateException("Обработчики Kafka не завершились");
            }
            try {
                wait(Math.min(200, Math.max(1, left / 1_000_000)));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание Kafka прервано", ex);
            }
        }
    }
}
