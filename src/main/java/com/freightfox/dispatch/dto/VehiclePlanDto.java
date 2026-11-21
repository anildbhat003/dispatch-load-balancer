package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Orders assigned to one vehicle, listed in the order they should be delivered")
public record VehiclePlanDto(
        @Schema(example = "VEH001") String vehicleId,
        @Schema(example = "10") double totalLoad,
        @Schema(example = "5.06 km") String totalDistance,
        List<OrderDto> assignedOrders
) {
}
