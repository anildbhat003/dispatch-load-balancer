package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;

/**
 * Headline numbers for a plan. {@code kmPerDispatchedOrder} is the fair efficiency measure when two
 * plans deliver a different number of orders.
 */
public record PlanMetrics(
        int ordersDispatched,
        int ordersTotal,
        int highPriorityDispatched,
        int highPriorityTotal,
        double totalDistanceKm,
        double kmPerDispatchedOrder,
        double capacityUsedPercent
) {

    public static PlanMetrics of(DispatchResult result) {
        int dispatched = 0;
        int highDispatched = 0;
        double distance = 0;
        double load = 0;
        double capacity = 0;
        for (DispatchResult.VehicleRoute route : result.vehicleRoutes()) {
            dispatched += route.stops().size();
            highDispatched += (int) route.stops().stream().filter(PlanMetrics::isHigh).count();
            distance += route.totalDistanceKm();
            load += route.totalLoad();
            capacity += route.vehicle().getCapacity();
        }
        int highLeft = (int) result.unassignedOrders().stream().map(DispatchResult.UnassignedOrder::order)
                .filter(PlanMetrics::isHigh).count();
        return new PlanMetrics(
                dispatched,
                dispatched + result.unassignedOrders().size(),
                highDispatched,
                highDispatched + highLeft,
                distance,
                dispatched == 0 ? 0 : distance / dispatched,
                capacity == 0 ? 0 : load * 100 / capacity);
    }

    private static boolean isHigh(DeliveryOrder order) {
        return order.getPriority() == Priority.HIGH;
    }
}
