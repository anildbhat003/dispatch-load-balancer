package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NearestNeighbourRouteOptimizerTest {

    private final DistanceCalculator haversine = new HaversineDistanceCalculator();
    private final NearestNeighbourRouteOptimizer optimizer = new NearestNeighbourRouteOptimizer(haversine);

    private static DeliveryOrder order(String id, double lat, double lon) {
        return new DeliveryOrder(id, lat, lon, "addr " + id, 1, Priority.MEDIUM);
    }

    @Test
    void emptyRouteHasZeroDistance() {
        Route route = optimizer.optimise(new GeoPoint(0, 0), List.of());
        assertThat(route.stops()).isEmpty();
        assertThat(route.distanceKm()).isZero();
    }

    @Test
    void visitsClosestStopFirst() {
        // Points along the equator: far (3°), near (1°), middle (2°), given out of order
        DeliveryOrder far = order("FAR", 0, 3);
        DeliveryOrder near = order("NEAR", 0, 1);
        DeliveryOrder mid = order("MID", 0, 2);

        Route route = optimizer.optimise(new GeoPoint(0, 0), List.of(far, near, mid));

        assertThat(route.stops()).extracting(DeliveryOrder::getOrderId).containsExactly("NEAR", "MID", "FAR");
        double expected = haversine.distanceKm(new GeoPoint(0, 0), new GeoPoint(0, 3));
        assertThat(route.distanceKm()).isCloseTo(expected, within(1e-6));
    }

    @Test
    void stopsAtSameLocationAddNoDistance() {
        DeliveryOrder a = order("A", 28.6139, 77.2090);
        DeliveryOrder b = order("B", 28.6139, 77.2090);
        Route route = optimizer.optimise(new GeoPoint(28.6139, 77.2090), List.of(a, b));
        assertThat(route.distanceKm()).isZero();
        assertThat(route.stops()).hasSize(2);
    }
}
