package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrdersRequest(
        @NotEmpty(message = "orders must contain at least one order")
        List<@Valid @NotNull(message = "order entry must not be null") OrderDto> orders
) {
}
