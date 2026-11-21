package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.PlanComparisonResponse;
import com.freightfox.dispatch.dto.StrategyMetricsDto;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.optimizer.DispatchResult;
import com.freightfox.dispatch.optimizer.DispatchStrategy;
import com.freightfox.dispatch.optimizer.PlanMetrics;
import com.freightfox.dispatch.optimizer.StrategyType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds dispatch plans from the stored orders and vehicles, and compares strategies. */
@Service
public class DispatchService {

    private final OrderService orderService;
    private final VehicleService vehicleService;
    private final Map<StrategyType, DispatchStrategy> strategies = new EnumMap<>(StrategyType.class);

    /** Spring injects every {@link DispatchStrategy} bean; each registers itself under its own type. */
    public DispatchService(OrderService orderService, VehicleService vehicleService, List<DispatchStrategy> strategies) {
        this.orderService = orderService;
        this.vehicleService = vehicleService;
        strategies.forEach(s -> this.strategies.put(s.type(), s));
    }

    public DispatchPlanResponse generatePlan(String strategyName) {
        return generatePlan(parse(strategyName));
    }

    public DispatchPlanResponse generatePlan(StrategyType type) {
        return DispatchMapper.toResponse(run(type, orderService.findAll(), vehicleService.findAll()));
    }

    /** Runs both strategies on the same snapshot of data and reports the difference. */
    public PlanComparisonResponse compare() {
        List<DeliveryOrder> orders = orderService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();
        PlanMetrics smart = PlanMetrics.of(run(StrategyType.SMART, orders, vehicles));
        PlanMetrics naive = PlanMetrics.of(run(StrategyType.NAIVE, orders, vehicles));

        double saved = naive.kmPerDispatchedOrder() == 0 ? 0
                : (naive.kmPerDispatchedOrder() - smart.kmPerDispatchedOrder()) * 100 / naive.kmPerDispatchedOrder();
        return new PlanComparisonResponse(toDto(StrategyType.SMART, smart), toDto(StrategyType.NAIVE, naive),
                round(saved), summary(smart, naive, saved));
    }

    private DispatchResult run(StrategyType type, List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        DispatchStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No strategy registered for " + type);
        }
        return strategy.plan(orders, vehicles);
    }

    private static StrategyType parse(String name) {
        return StrategyType.fromName(name).orElseThrow(() -> new InvalidRequestException(
                "Unknown strategy '" + name + "'",
                List.of("Allowed values: " + String.join(", ",
                        Arrays.stream(StrategyType.values()).map(StrategyType::key).toList()))));
    }

    private static String summary(PlanMetrics smart, PlanMetrics naive, double saved) {
        if (smart.ordersTotal() == 0) {
            return "Add orders and vehicles to compare strategies.";
        }
        String distance = saved >= 0
                ? String.format(Locale.ROOT, "drives %.1f%% less per delivered order", saved)
                : String.format(Locale.ROOT, "drives %.1f%% more per delivered order", -saved);
        return String.format(Locale.ROOT, "Smart dispatch %s and dispatches %d/%d HIGH-priority orders (naive: %d/%d).",
                distance, smart.highPriorityDispatched(), smart.highPriorityTotal(),
                naive.highPriorityDispatched(), naive.highPriorityTotal());
    }

    private static StrategyMetricsDto toDto(StrategyType type, PlanMetrics m) {
        return new StrategyMetricsDto(type.key(), type.getDescription(), m.ordersDispatched(), m.ordersTotal(),
                m.highPriorityDispatched(), m.highPriorityTotal(), round(m.totalDistanceKm()),
                round(m.kmPerDispatchedOrder()), round(m.capacityUsedPercent()));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
