package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Headline numbers for one strategy's plan")
public record StrategyMetricsDto(
        @Schema(example = "smart") String strategy,
        @Schema(example = "Priority first, nearest vehicle with room, stops ordered nearest-next") String description,
        @Schema(example = "22") int ordersDispatched,
        @Schema(example = "30") int ordersTotal,
        @Schema(example = "12") int highPriorityDispatched,
        @Schema(example = "12") int highPriorityTotal,
        @Schema(example = "73.42") double totalDistanceKm,
        @Schema(example = "3.34") double kmPerDispatchedOrder,
        @Schema(example = "98.0") double capacityUsedPercent
) {
}
