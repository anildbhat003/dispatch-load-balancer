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
 * Default dispatch heuristic. Assigning orders to capacity-limited vehicles while minimising
 * distance is the Capacitated Vehicle Routing Problem (NP-hard), so we use a fast, explainable
 * greedy approach:
 *
 * <ol>
 *   <li><b>Sort orders</b> by priority (HIGH → MEDIUM → LOW), then by weight descending (heavier
 *       packages are harder to fit, so place them while capacity is still free), then by orderId
 *       for deterministic output.</li>
 *   <li><b>Assign each order</b> to the vehicle that can still carry it and is <i>closest to that
 *       vehicle's last planned stop</i> (its start position if it has none yet). Using the last stop
 *       rather than the depot makes the choice route-aware: a vehicle already heading towards an
 *       area naturally picks up more orders there. Ties go to the vehicle with the least spare
 *       capacity (best fit), keeping larger vehicles free for bigger packages.</li>
 *   <li><b>Order each vehicle's stops</b> with the {@link RouteOptimizer} (nearest neighbour) and
 *       sum the Haversine distance from the vehicle's start through every drop point.</li>
 *   <li>Orders no vehicle can carry are reported as <b>unassigned with a reason</b>.</li>
 * </ol>
 *
 * Because HIGH orders are placed first, a lower-priority order can never take capacity a
 * higher-priority order needed. Complexity: O(n log n + n·v + Σk²).
 */
@Component
public class GreedyPriorityDispatchStrategy implements DispatchStrategy {

    static final double EPSILON = 1e-9;

    static final Comparator<DeliveryOrder> DISPATCH_ORDER = Comparator
            .comparingInt((DeliveryOrder o) -> o.getPriority().getRank())
            .thenComparing(DeliveryOrder::getPackageWeight, Comparator.reverseOrder())
            .thenComparing(DeliveryOrder::getOrderId);

    private final DistanceCalculator distanceCalculator;
    private final RouteOptimizer routeOptimizer;

    public GreedyPriorityDispatchStrategy(DistanceCalculator distanceCalculator, RouteOptimizer routeOptimizer) {
        this.distanceCalculator = distanceCalculator;
        this.routeOptimizer = routeOptimizer;
    }

    @Override
    public StrategyType type() {
        return StrategyType.SMART;
    }

    @Override
    public DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        List<VehicleState> fleet = vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .map(VehicleState::new)
                .toList();
        double largestCapacity = vehicles.stream().mapToDouble(Vehicle::getCapacity).max().orElse(0);

        List<UnassignedOrder> unassigned = new ArrayList<>();
        orders.stream().sorted(DISPATCH_ORDER).forEach(order -> {
            VehicleState best = findBestVehicle(order, fleet);
            if (best == null) {
                unassigned.add(new UnassignedOrder(order, UnassignedReason.of(order, fleet.isEmpty(), largestCapacity)));
            } else {
                best.assign(order);
            }
        });

        List<VehicleRoute> routes = fleet.stream().map(this::buildRoute).toList();
        return new DispatchResult(routes, unassigned);
    }

    private VehicleState findBestVehicle(DeliveryOrder order, List<VehicleState> fleet) {
        GeoPoint drop = GeoPoint.of(order);
        VehicleState best = null;
        double bestDistance = Double.MAX_VALUE;

        for (VehicleState candidate : fleet) {
            if (!candidate.canCarry(order.getPackageWeight())) {
                continue;
            }
            double distance = distanceCalculator.distanceKm(candidate.lastPosition, drop);
            boolean closer = distance < bestDistance - EPSILON;
            boolean tieWithTighterFit = Math.abs(distance - bestDistance) <= EPSILON
                    && best != null && candidate.remainingCapacity() < best.remainingCapacity();
            if (closer || tieWithTighterFit) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private VehicleRoute buildRoute(VehicleState state) {
        Route route = routeOptimizer.optimise(GeoPoint.of(state.vehicle), state.assigned);
        return new VehicleRoute(state.vehicle, route.stops(), state.load, route.distanceKm());
    }

    /** Mutable working state for one vehicle while the plan is being built. */
    private static final class VehicleState {
        private final Vehicle vehicle;
        private final List<DeliveryOrder> assigned = new ArrayList<>();
        private double load;
        private GeoPoint lastPosition;

        private VehicleState(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.lastPosition = GeoPoint.of(vehicle);
        }

        private boolean canCarry(double weight) {
            return load + weight <= vehicle.getCapacity() + EPSILON;
        }

        private double remainingCapacity() {
            return vehicle.getCapacity() - load;
        }

        private void assign(DeliveryOrder order) {
            assigned.add(order);
            load += order.getPackageWeight();
            lastPosition = GeoPoint.of(order);
        }
    }
}
