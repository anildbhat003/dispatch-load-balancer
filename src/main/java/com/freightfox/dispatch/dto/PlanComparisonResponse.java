package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The smart plan measured against a naive first-come, round-robin baseline on the same data")
public record PlanComparisonResponse(
        StrategyMetricsDto smart,
        StrategyMetricsDto naive,
        @Schema(description = "Reduction in km driven per delivered order, smart vs naive (positive = smart drives less)",
                example = "61.4")
        double distanceSavedPercent,
        @Schema(example = "Smart dispatch drives 61.4% less per delivered order and dispatches 12/12 HIGH-priority "
                + "orders (naive: 9/12).")
        String summary
) {
}
