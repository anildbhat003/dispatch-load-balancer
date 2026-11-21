package com.freightfox.dispatch.optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.VehiclesRequest;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.optimizer.DispatchResult.UnassignedOrder;
import com.freightfox.dispatch.optimizer.DispatchResult.VehicleRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GreedyPriorityDispatchStrategyTest {

    private final DistanceCalculator haversine = new HaversineDistanceCalculator();
    private final GreedyPriorityDispatchStrategy strategy =
            new GreedyPriorityDispatchStrategy(haversine, new NearestNeighbourRouteOptimizer(haversine));

    private static DeliveryOrder order(String id, double lat, double lon, double weight, Priority p) {
        return new DeliveryOrder(id, lat, lon, "addr " + id, weight, p);
    }

    private static Vehicle vehicle(String id, double capacity, double lat, double lon) {
        return new Vehicle(id, capacity, lat, lon, "depot " + id);
    }

    private static VehicleRoute routeOf(DispatchResult result, String vehicleId) {
        return result.vehicleRoutes().stream()
                .filter(r -> r.vehicle().getVehicleId().equals(vehicleId))
                .findFirst().orElseThrow();
    }

    private static Set<String> orderIds(VehicleRoute route) {
        return route.stops().stream().map(DeliveryOrder::getOrderId).collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("Capacity")
    class Capacity {

        @Test
        void neverExceedsVehicleCapacity() {
            List<Vehicle> vehicles = List.of(vehicle("V1", 50, 0, 0), vehicle("V2", 30, 0, 0.1));
            List<DeliveryOrder> orders = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                orders.add(order("O" + i, 0, i * 0.01, 10, Priority.MEDIUM));
            }

            DispatchResult result = strategy.plan(orders, vehicles);

            for (VehicleRoute route : result.vehicleRoutes()) {
                assertThat(route.totalLoad()).isLessThanOrEqualTo(route.vehicle().getCapacity());
            }
            assertThat(result.unassignedOrders()).hasSize(4); // 120kg of orders, 80kg of capacity
        }

        @Test
        void orderExactlyEqualToCapacityFits() {
            DispatchResult result = strategy.plan(
                    List.of(order("O1", 0, 0, 100, Priority.LOW)), List.of(vehicle("V1", 100, 0, 0)));
            assertThat(result.unassignedOrders()).isEmpty();
            assertThat(routeOf(result, "V1").totalLoad()).isEqualTo(100);
        }

        @Test
        void overweightOrderIsUnassignedWithReason() {
            DispatchResult result = strategy.plan(
                    List.of(order("HEAVY", 0, 0, 500, Priority.HIGH)),
                    List.of(vehicle("V1", 100, 0, 0), vehicle("V2", 150, 0, 0)));

            assertThat(result.unassignedOrders()).singleElement().satisfies(u -> {
                assertThat(u.order().getOrderId()).isEqualTo("HEAVY");
                assertThat(u.reason()).contains("exceeds the largest vehicle capacity (150.00)");
            });
        }

        @Test
        void orderThatFitsAVehicleButNoRemainingSpaceGetsCapacityReason() {
            DispatchResult result = strategy.plan(
                    List.of(order("A", 0, 0, 80, Priority.HIGH), order("B", 0, 0, 80, Priority.LOW)),
                    List.of(vehicle("V1", 100, 0, 0)));

            assertThat(result.unassignedOrders()).singleElement().satisfies(u -> {
                assertThat(u.order().getOrderId()).isEqualTo("B");
                assertThat(u.reason()).isEqualTo("Insufficient remaining capacity in all vehicles");
            });
        }
    }

    @Nested
    @DisplayName("Priority")
    class PriorityRules {

        @Test
        void highPriorityWinsScarceCapacityEvenWhenListedLast() {
            DeliveryOrder low = order("LOW1", 0, 0, 60, Priority.LOW);
            DeliveryOrder medium = order("MED1", 0, 0, 60, Priority.MEDIUM);
            DeliveryOrder high = order("HIGH1", 0, 0, 60, Priority.HIGH);

            DispatchResult result = strategy.plan(List.of(low, medium, high), List.of(vehicle("V1", 100, 0, 0)));

            assertThat(orderIds(routeOf(result, "V1"))).containsExactly("HIGH1");
            assertThat(result.unassignedOrders()).extracting(u -> u.order().getOrderId())
                    .containsExactlyInAnyOrder("LOW1", "MED1");
        }

        @Test
        void mediumBeatsLowForTheLastSlot() {
            DispatchResult result = strategy.plan(
                    List.of(order("L", 0, 0, 50, Priority.LOW), order("M", 0, 0, 50, Priority.MEDIUM)),
                    List.of(vehicle("V1", 50, 0, 0)));
            assertThat(orderIds(routeOf(result, "V1"))).containsExactly("M");
        }
    }

    @Nested
    @DisplayName("Distance")
    class Distance {

        @Test
        void assignsEachOrderToTheNearestVehicle() {
            // V_DEL in Delhi, V_BLR in Bangalore; orders in each city
            Vehicle delhiTruck = vehicle("V_DEL", 100, 28.6139, 77.2090);
            Vehicle blrTruck = vehicle("V_BLR", 100, 12.9716, 77.5946);
            DeliveryOrder karolBagh = order("DEL1", 28.7041, 77.1025, 10, Priority.MEDIUM);
            DeliveryOrder indiranagar = order("BLR1", 12.9716, 77.6413, 10, Priority.MEDIUM);

            DispatchResult result = strategy.plan(List.of(karolBagh, indiranagar), List.of(delhiTruck, blrTruck));

            assertThat(orderIds(routeOf(result, "V_DEL"))).containsExactly("DEL1");
            assertThat(orderIds(routeOf(result, "V_BLR"))).containsExactly("BLR1");
        }

        @Test
        void totalDistanceIsStartPlusEveryHop() {
            Vehicle v = vehicle("V1", 100, 0, 0);
            DeliveryOrder a = order("A", 0, 1, 10, Priority.HIGH);
            DeliveryOrder b = order("B", 0, 2, 10, Priority.HIGH);

            DispatchResult result = strategy.plan(List.of(b, a), List.of(v));

            VehicleRoute route = routeOf(result, "V1");
            assertThat(route.stops()).extracting(DeliveryOrder::getOrderId).containsExactly("A", "B");
            double expected = haversine.distanceKm(new GeoPoint(0, 0), new GeoPoint(0, 2));
            assertThat(route.totalDistanceKm()).isCloseTo(expected, within(1e-6));
        }

        @Test
        void whenNearestVehicleIsFullTheNextNearestIsUsed() {
            Vehicle near = vehicle("NEAR", 10, 0, 0);
            Vehicle far = vehicle("FAR", 100, 0, 1);
            DispatchResult result = strategy.plan(
                    List.of(order("A", 0, 0, 10, Priority.HIGH), order("B", 0, 0, 10, Priority.LOW)),
                    List.of(near, far));

            assertThat(orderIds(routeOf(result, "NEAR"))).containsExactly("A");
            assertThat(orderIds(routeOf(result, "FAR"))).containsExactly("B");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        void noOrdersGivesEmptyRoutesForEveryVehicle() {
            DispatchResult result = strategy.plan(List.of(), List.of(vehicle("V1", 10, 0, 0)));
            assertThat(result.vehicleRoutes()).singleElement().satisfies(r -> {
                assertThat(r.stops()).isEmpty();
                assertThat(r.totalLoad()).isZero();
                assertThat(r.totalDistanceKm()).isZero();
            });
            assertThat(result.unassignedOrders()).isEmpty();
        }

        @Test
        void noVehiclesMeansEveryOrderIsUnassigned() {
            DispatchResult result = strategy.plan(List.of(order("A", 0, 0, 1, Priority.HIGH)), List.of());
            assertThat(result.vehicleRoutes()).isEmpty();
            assertThat(result.unassignedOrders()).extracting(UnassignedOrder::reason)
                    .containsExactly("No vehicles available");
        }

        @Test
        void resultIsDeterministic() {
            List<DeliveryOrder> orders = List.of(order("A", 0, 0, 5, Priority.HIGH), order("B", 0, 0, 5, Priority.HIGH));
            List<Vehicle> vehicles = List.of(vehicle("V2", 10, 0, 0), vehicle("V1", 10, 0, 0));
            assertThat(strategy.plan(orders, vehicles)).isEqualTo(strategy.plan(orders, vehicles));
        }
    }

    @Nested
    @DisplayName("Assignment sample data (30 Delhi NCR orders, 5 vehicles)")
    class SampleData {

        private final ObjectMapper mapper = new ObjectMapper();

        private <T> T read(String file, Class<T> type) throws IOException {
            try (InputStream in = getClass().getResourceAsStream("/" + file)) {
                return mapper.readValue(in, type);
            }
        }

        @Test
        void sampleDataRespectsAllConstraints() throws IOException {
            List<DeliveryOrder> orders = read("sample-orders.json", OrdersRequest.class).orders().stream()
                    .map(DispatchMapper::toEntity).toList();
            List<Vehicle> vehicles = read("sample-vehicles.json", VehiclesRequest.class).vehicles().stream()
                    .map(DispatchMapper::toEntity).toList();

            DispatchResult result = strategy.plan(orders, vehicles);

            int assigned = result.vehicleRoutes().stream().mapToInt(r -> r.stops().size()).sum();
            assertThat(assigned + result.unassignedOrders().size()).isEqualTo(30);

            for (VehicleRoute r : result.vehicleRoutes()) {
                assertThat(r.totalLoad()).isLessThanOrEqualTo(r.vehicle().getCapacity());
                double sum = r.stops().stream().mapToDouble(DeliveryOrder::getPackageWeight).sum();
                assertThat(r.totalLoad()).isEqualTo(sum);
            }

            // 645kg of orders vs 500kg capacity: HIGH (275kg) and MEDIUM (145kg) all fit, only LOW is left over
            assertThat(result.unassignedOrders()).isNotEmpty()
                    .allSatisfy(u -> assertThat(u.order().getPriority()).isEqualTo(Priority.LOW));
        }
    }
}
