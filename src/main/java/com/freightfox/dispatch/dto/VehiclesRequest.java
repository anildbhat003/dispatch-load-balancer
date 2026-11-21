package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VehiclesRequest(
        @NotEmpty(message = "vehicles must contain at least one vehicle")
        List<@Valid @NotNull(message = "vehicle entry must not be null") VehicleDto> vehicles
) {
}
