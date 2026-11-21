package com.freightfox.dispatch.mapper;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrderDto;
import com.freightfox.dispatch.dto.UnassignedOrderDto;
import com.freightfox.dispatch.dto.VehicleDto;
import com.freightfox.dispatch.dto.VehiclePlanDto;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.optimizer.DispatchResult;

import java.util.Locale;

/** Converts between API DTOs, JPA entities and optimiser results. */
public final class DispatchMapper {

    private DispatchMapper() {
    }

    public static DeliveryOrder toEntity(OrderDto dto) {
        return new DeliveryOrder(dto.orderId().trim(), dto.latitude(), dto.longitude(), dto.address().trim(),
                dto.packageWeight(), dto.priority());
    }

    public static void updateEntity(DeliveryOrder entity, OrderDto dto) {
        entity.updateDetails(dto.latitude(), dto.longitude(), dto.address().trim(), dto.packageWeight(), dto.priority());
    }

    public static Vehicle toEntity(VehicleDto dto) {
        return new Vehicle(dto.vehicleId().trim(), dto.capacity(), dto.currentLatitude(), dto.currentLongitude(),
                dto.currentAddress().trim());
    }

    public static void updateEntity(Vehicle entity, VehicleDto dto) {
        entity.updateDetails(dto.capacity(), dto.currentLatitude(), dto.currentLongitude(), dto.currentAddress().trim());
    }

    public static OrderDto toDto(DeliveryOrder o) {
        return new OrderDto(o.getOrderId(), o.getLatitude(), o.getLongitude(), o.getAddress(),
                o.getPackageWeight(), o.getPriority());
    }

    public static VehicleDto toDto(Vehicle v) {
        return new VehicleDto(v.getVehicleId(), v.getCapacity(), v.getCurrentLatitude(), v.getCurrentLongitude(),
                v.getCurrentAddress());
    }

    public static DispatchPlanResponse toResponse(DispatchResult result) {
        return new DispatchPlanResponse(
                result.vehicleRoutes().stream().map(DispatchMapper::toPlan).toList(),
                result.unassignedOrders().stream().map(DispatchMapper::toUnassigned).toList());
    }

    public static String formatDistance(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }

    private static VehiclePlanDto toPlan(DispatchResult.VehicleRoute route) {
        return new VehiclePlanDto(
                route.vehicle().getVehicleId(),
                round2(route.totalLoad()),
                formatDistance(route.totalDistanceKm()),
                route.stops().stream().map(DispatchMapper::toDto).toList());
    }

    private static UnassignedOrderDto toUnassigned(DispatchResult.UnassignedOrder u) {
        DeliveryOrder o = u.order();
        return new UnassignedOrderDto(o.getOrderId(), o.getPackageWeight(), o.getPriority(), u.reason());
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
