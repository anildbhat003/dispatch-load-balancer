package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.optimizer.DispatchResult.UnassignedOrder;
import com.freightfox.dispatch.optimizer.DispatchResult.VehicleRoute;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Baseline strategy used only to measure how much the smart strategy helps. It represents how
 * dispatch is often done by hand:
 *
 * <ul>
 *   <li>orders are taken first come, first served (by orderId), ignoring priority;</li>
 *   <li>vehicles take turns (round robin); a vehicle without room is skipped;</li>
 *   <li>location is ignored, and each vehicle drives its stops in the order they were assigned.</li>
 * </ul>
 *
 * It still never exceeds capacity, so the comparison with {@link GreedyPriorityDispatchStrategy} is fair.
 */
@Component
public class NaiveRoundRobinDispatchStrategy implements DispatchStrategy {

    private final DistanceCalculator distanceCalculator;

    public NaiveRoundRobinDispatchStrategy(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public StrategyType type() {
        return StrategyType.NAIVE;
    }

    @Override
    public DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        List<Vehicle> fleet = vehicles.stream().sorted(Comparator.comparing(Vehicle::getVehicleId)).toList();
        List<List<DeliveryOrder>> assigned = new ArrayList<>();
        double[] load = new double[fleet.size()];
        fleet.forEach(v -> assigned.add(new ArrayList<>()));
        double largestCapacity = fleet.stream().mapToDouble(Vehicle::getCapacity).max().orElse(0);

        List<UnassignedOrder> unassigned = new ArrayList<>();
        int turn = 0;
        for (DeliveryOrder order : orders.stream().sorted(Comparator.comparing(DeliveryOrder::getOrderId)).toList()) {
            int chosen = -1;
            for (int tried = 0; tried < fleet.size(); tried++) {
                int i = (turn + tried) % fleet.size();
                if (load[i] + order.getPackageWeight() <= fleet.get(i).getCapacity() + GreedyPriorityDispatchStrategy.EPSILON) {
                    chosen = i;
                    break;
                }
            }
            if (chosen < 0) {
                unassigned.add(new UnassignedOrder(order, UnassignedReason.of(order, fleet.isEmpty(), largestCapacity)));
            } else {
                assigned.get(chosen).add(order);
                load[chosen] += order.getPackageWeight();
                turn = (chosen + 1) % fleet.size();
            }
        }

        List<VehicleRoute> routes = new ArrayList<>();
        for (int i = 0; i < fleet.size(); i++) {
            routes.add(new VehicleRoute(fleet.get(i), assigned.get(i), load[i], driveInOrder(fleet.get(i), assigned.get(i))));
        }
        return new DispatchResult(routes, unassigned);
    }

    private double driveInOrder(Vehicle vehicle, List<DeliveryOrder> stops) {
        GeoPoint current = GeoPoint.of(vehicle);
        double total = 0;
        for (DeliveryOrder stop : stops) {
            GeoPoint next = GeoPoint.of(stop);
            total += distanceCalculator.distanceKm(current, next);
            current = next;
        }
        return total;
    }
}
