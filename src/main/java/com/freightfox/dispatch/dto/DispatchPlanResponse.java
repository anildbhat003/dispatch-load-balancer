package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Optimised dispatch plan")
public record DispatchPlanResponse(
        @Schema(description = "One entry per vehicle in the fleet")
        List<VehiclePlanDto> dispatchPlan,

        @Schema(description = "Orders that could not be assigned (empty when everything fits)")
        List<UnassignedOrderDto> unassignedOrders
) {
}
