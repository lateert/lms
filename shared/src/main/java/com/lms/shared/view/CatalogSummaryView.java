package com.lms.shared.view;

import java.util.List;

public record CatalogSummaryView(int total, double avgRating, List<DifficultyCountView> byDifficulty) {
}
