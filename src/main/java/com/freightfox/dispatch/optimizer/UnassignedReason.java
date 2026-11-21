package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;

import java.util.Locale;

/** Human-readable explanation of why an order could not be assigned. Shared by all strategies. */
final class UnassignedReason {

    private UnassignedReason() {
    }

    static String of(DeliveryOrder order, boolean noVehicles, double largestCapacity) {
        if (noVehicles) {
            return "No vehicles available";
        }
        if (order.getPackageWeight() > largestCapacity + GreedyPriorityDispatchStrategy.EPSILON) {
            return String.format(Locale.ROOT, "Package weight %.2f exceeds the largest vehicle capacity (%.2f)",
                    order.getPackageWeight(), largestCapacity);
        }
        return "Insufficient remaining capacity in all vehicles";
    }
}
