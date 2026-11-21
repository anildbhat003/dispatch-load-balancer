package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;

import java.util.List;

/** Strategy for ordering a vehicle's stops to keep its travel distance low. */
public interface RouteOptimizer {

    Route optimise(GeoPoint start, List<DeliveryOrder> orders);
}
