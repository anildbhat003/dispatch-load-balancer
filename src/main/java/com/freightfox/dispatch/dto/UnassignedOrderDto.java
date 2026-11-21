package com.freightfox.dispatch.dto;

import com.freightfox.dispatch.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An order that could not be assigned to any vehicle, with the reason")
public record UnassignedOrderDto(
        @Schema(example = "ORD099") String orderId,
        @Schema(example = "250") double packageWeight,
        @Schema(example = "LOW") Priority priority,
        @Schema(example = "Package weight 250.00 exceeds the largest vehicle capacity (150.00)") String reason
) {
}
