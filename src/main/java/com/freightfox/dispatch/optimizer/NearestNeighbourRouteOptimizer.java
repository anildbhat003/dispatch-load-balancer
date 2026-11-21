package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Orders stops with the nearest-neighbour heuristic: from the current position, always drive to
 * the closest undelivered drop point next. The route is open (the vehicle does not return to its
 * start). Complexity O(k²) for k stops.
 */
@Component
public class NearestNeighbourRouteOptimizer implements RouteOptimizer {

    private final DistanceCalculator distanceCalculator;

    public NearestNeighbourRouteOptimizer(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public Route optimise(GeoPoint start, List<DeliveryOrder> orders) {
        List<DeliveryOrder> remaining = new LinkedList<>(orders);
        List<DeliveryOrder> stops = new ArrayList<>(orders.size());
        GeoPoint current = start;
        double total = 0.0;

        while (!remaining.isEmpty()) {
            DeliveryOrder nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (DeliveryOrder candidate : remaining) {
                double d = distanceCalculator.distanceKm(current, GeoPoint.of(candidate));
                if (d < nearestDistance) {
                    nearestDistance = d;
                    nearest = candidate;
                }
            }
            remaining.remove(nearest);
            stops.add(nearest);
            total += nearestDistance;
            current = GeoPoint.of(nearest);
        }
        return new Route(stops, total);
    }
}
