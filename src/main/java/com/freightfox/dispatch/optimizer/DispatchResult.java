package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;

import java.util.List;

/** Output of a {@link DispatchStrategy}: one route per vehicle plus any orders that did not fit. */
public record DispatchResult(List<VehicleRoute> vehicleRoutes, List<UnassignedOrder> unassignedOrders) {

    public DispatchResult {
        vehicleRoutes = List.copyOf(vehicleRoutes);
        unassignedOrders = List.copyOf(unassignedOrders);
    }

    public record VehicleRoute(Vehicle vehicle, List<DeliveryOrder> stops, double totalLoad, double totalDistanceKm) {
        public VehicleRoute {
            stops = List.copyOf(stops);
        }
    }

    public record UnassignedOrder(DeliveryOrder order, String reason) {
    }
}
