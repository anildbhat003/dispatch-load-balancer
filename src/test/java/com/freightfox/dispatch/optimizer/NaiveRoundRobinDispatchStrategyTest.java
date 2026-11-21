package com.freightfox.dispatch.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.VehiclesRequest;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NaiveRoundRobinDispatchStrategyTest {

    private final DistanceCalculator haversine = new HaversineDistanceCalculator();
    private final NaiveRoundRobinDispatchStrategy naive = new NaiveRoundRobinDispatchStrategy(haversine);
    private final GreedyPriorityDispatchStrategy smart =
            new GreedyPriorityDispatchStrategy(haversine, new NearestNeighbourRouteOptimizer(haversine));

    private static DeliveryOrder order(String id, double lon, double weight, Priority p) {
        return new DeliveryOrder(id, 0, lon, "addr " + id, weight, p);
    }

    private static List<String> ids(DispatchResult.VehicleRoute r) {
        return r.stops().stream().map(DeliveryOrder::getOrderId).toList();
    }

    @Test
    void vehiclesTakeTurnsInArrivalOrder() {
        List<Vehicle> vehicles = List.of(new Vehicle("V1", 100, 0, 0, "a"), new Vehicle("V2", 100, 0, 0, "b"));
        List<DeliveryOrder> orders = List.of(order("O3", 3, 1, Priority.LOW), order("O1", 1, 1, Priority.LOW),
                order("O2", 2, 1, Priority.HIGH), order("O4", 4, 1, Priority.LOW));

        DispatchResult result = naive.plan(orders, vehicles);

        assertThat(ids(result.vehicleRoutes().get(0))).containsExactly("O1", "O3");
        assertThat(ids(result.vehicleRoutes().get(1))).containsExactly("O2", "O4");
    }

    @Test
    void ignoresPrioritySoAnEarlyLowOrderCanBlockALateHighOne() {
        List<DeliveryOrder> orders = List.of(order("A_LOW", 0, 80, Priority.LOW), order("B_HIGH", 0, 80, Priority.HIGH));
        DispatchResult result = naive.plan(orders, List.of(new Vehicle("V1", 100, 0, 0, "a")));

        assertThat(result.unassignedOrders()).extracting(u -> u.order().getOrderId()).containsExactly("B_HIGH");
    }

    @Test
    void skipsFullVehiclesAndRespectsCapacity() {
        List<Vehicle> vehicles = List.of(new Vehicle("V1", 10, 0, 0, "a"), new Vehicle("V2", 100, 0, 0, "b"));
        List<DeliveryOrder> orders = List.of(order("O1", 0, 10, Priority.LOW), order("O2", 0, 50, Priority.LOW),
                order("O3", 0, 50, Priority.LOW));

        DispatchResult result = naive.plan(orders, vehicles);

        assertThat(ids(result.vehicleRoutes().get(0))).containsExactly("O1");
        assertThat(ids(result.vehicleRoutes().get(1))).containsExactly("O2", "O3");
    }

    @Test
    void drivesStopsInAssignedOrderWithoutReordering() {
        Vehicle v = new Vehicle("V1", 100, 0, 0, "a");
        DispatchResult result = naive.plan(List.of(order("O1", 2, 1, Priority.LOW), order("O2", 1, 1, Priority.LOW)), List.of(v));

        double expected = haversine.distanceKm(new GeoPoint(0, 0), new GeoPoint(0, 2))
                + haversine.distanceKm(new GeoPoint(0, 2), new GeoPoint(0, 1));
        assertThat(result.vehicleRoutes().get(0).totalDistanceKm()).isCloseTo(expected, within(1e-6));
    }

    @Test
    void noVehiclesLeavesEverythingUnassigned() {
        DispatchResult result = naive.plan(List.of(order("O1", 0, 1, Priority.HIGH)), List.of());
        assertThat(result.unassignedOrders()).singleElement()
                .satisfies(u -> assertThat(u.reason()).isEqualTo("No vehicles available"));
    }

    @Test
    @DisplayName("On the assignment sample, smart drives less per delivery and dispatches more HIGH orders")
    void smartBeatsNaiveOnSampleData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<DeliveryOrder> orders;
        List<Vehicle> vehicles;
        try (InputStream o = getClass().getResourceAsStream("/sample-orders.json");
             InputStream v = getClass().getResourceAsStream("/sample-vehicles.json")) {
            orders = mapper.readValue(o, OrdersRequest.class).orders().stream().map(DispatchMapper::toEntity).toList();
            vehicles = mapper.readValue(v, VehiclesRequest.class).vehicles().stream().map(DispatchMapper::toEntity).toList();
        }

        PlanMetrics s = PlanMetrics.of(smart.plan(orders, vehicles));
        PlanMetrics n = PlanMetrics.of(naive.plan(orders, vehicles));

        assertThat(s.kmPerDispatchedOrder()).isLessThan(n.kmPerDispatchedOrder());
        assertThat(s.highPriorityDispatched()).isEqualTo(s.highPriorityTotal());
        assertThat(s.highPriorityDispatched()).isGreaterThanOrEqualTo(n.highPriorityDispatched());
    }
}
