package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;

import java.util.List;

/** Ordered list of stops for one vehicle and the total distance to drive them. */
public record Route(List<DeliveryOrder> stops, double distanceKm) {

    public Route {
        stops = List.copyOf(stops);
    }
}
