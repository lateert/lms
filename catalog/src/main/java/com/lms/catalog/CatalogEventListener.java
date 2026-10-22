package com.lms.catalog;

import com.lms.shared.event.CourseRatingUpdated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CatalogEventListener {
    private final CatalogService catalog;

    @TransactionalEventListener(fallbackExecution = true)
    public void on(CourseRatingUpdated event) {
        catalog.handleRatingUpdated(event);
    }
}
