package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.ApiResponse;
import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.ErrorResponse;
import com.freightfox.dispatch.dto.OrderDto;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.PlanComparisonResponse;
import com.freightfox.dispatch.dto.VehicleDto;
import com.freightfox.dispatch.dto.VehiclesRequest;
import com.freightfox.dispatch.mapper.DispatchMapper;
import com.freightfox.dispatch.service.DispatchService;
import com.freightfox.dispatch.service.OrderService;
import com.freightfox.dispatch.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping(path = "/api/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dispatch", description = "Submit orders and vehicles, then retrieve the optimised dispatch plan")
public class DispatchController {

    private static final String ORDERS_EXAMPLE = """
            {
              "orders": [
                {"orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
                 "address": "MG Road, Bangalore, Karnataka, India", "packageWeight": 10, "priority": "HIGH"},
                {"orderId": "ORD002", "latitude": 13.0827, "longitude": 80.2707,
                 "address": "Anna Salai, Chennai, Tamil Nadu, India", "packageWeight": 20, "priority": "MEDIUM"}
              ]
            }""";

    private static final String VEHICLES_EXAMPLE = """
            {
              "vehicles": [
                {"vehicleId": "VEH001", "capacity": 100, "currentLatitude": 12.9716, "currentLongitude": 77.6413,
                 "currentAddress": "Indiranagar, Bangalore, Karnataka, India"},
                {"vehicleId": "VEH002", "capacity": 150, "currentLatitude": 13.0674, "currentLongitude": 80.2376,
                 "currentAddress": "T Nagar, Chennai, Tamil Nadu, India"}
              ]
            }""";

    private final OrderService orderService;
    private final VehicleService vehicleService;
    private final DispatchService dispatchService;

    public DispatchController(OrderService orderService, VehicleService vehicleService,
                              DispatchService dispatchService) {
        this.orderService = orderService;
        this.vehicleService = vehicleService;
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "Submit delivery orders",
            description = "Adds orders, or updates them if the orderId already exists (upsert). "
                    + "Every field is validated; duplicate orderIds within one request are rejected.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(name = "Sample", value = ORDERS_EXAMPLE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse submitOrders(@Valid @RequestBody OrdersRequest request) {
        orderService.upsertOrders(request.orders());
        return ApiResponse.success("Delivery orders accepted.");
    }

    @Operation(summary = "Submit fleet details",
            description = "Adds vehicles, or updates them if the vehicleId already exists (upsert).")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(name = "Sample", value = VEHICLES_EXAMPLE)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicles accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(path = "/vehicles", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse submitVehicles(@Valid @RequestBody VehiclesRequest request) {
        vehicleService.upsertVehicles(request.vehicles());
        return ApiResponse.success("Vehicle details accepted.");
    }

    @Operation(summary = "Get the optimised dispatch plan",
            description = "Assigns stored orders to stored vehicles: HIGH priority first, never exceeding "
                    + "capacity, choosing the nearest vehicle (Haversine) to keep travel distance low. "
                    + "Each vehicle's orders are listed in delivery sequence. Orders that cannot fit are "
                    + "returned in unassignedOrders with a reason.")
    @GetMapping("/plan")
    public DispatchPlanResponse getDispatchPlan(
            @Parameter(description = "Optional. 'smart' (default) is the optimised plan; 'naive' is a first-come, "
                    + "round-robin baseline, useful for comparison.", example = "smart")
            @RequestParam(defaultValue = "smart") String strategy) {
        return dispatchService.generatePlan(strategy);
    }

    @Operation(summary = "Compare the smart plan with a naive baseline",
            description = "Runs both strategies on the same stored data. The naive baseline takes orders first come, "
                    + "first served, gives them to vehicles in turn, and ignores priority and location. Reports orders "
                    + "and HIGH-priority orders dispatched, total km and km per delivered order for each, plus the "
                    + "percentage of driving saved.")
    @GetMapping("/plan/comparison")
    public PlanComparisonResponse comparePlans() {
        return dispatchService.compare();
    }

    @Operation(summary = "List stored orders")
    @GetMapping("/orders")
    public List<OrderDto> listOrders() {
        return orderService.findAll().stream()
                .map(DispatchMapper::toDto)
                .sorted(Comparator.comparing(OrderDto::orderId))
                .toList();
    }

    @Operation(summary = "List stored vehicles")
    @GetMapping("/vehicles")
    public List<VehicleDto> listVehicles() {
        return vehicleService.findAll().stream()
                .map(DispatchMapper::toDto)
                .sorted(Comparator.comparing(VehicleDto::vehicleId))
                .toList();
    }

    @Operation(summary = "Delete all stored orders", description = "Handy for resetting between test runs")
    @DeleteMapping("/orders")
    public ApiResponse deleteOrders() {
        orderService.deleteAll();
        return ApiResponse.success("All delivery orders deleted.");
    }

    @Operation(summary = "Delete all stored vehicles", description = "Handy for resetting between test runs")
    @DeleteMapping("/vehicles")
    public ApiResponse deleteVehicles() {
        vehicleService.deleteAll();
        return ApiResponse.success("All vehicles deleted.");
    }
}
