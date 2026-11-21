package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchControllerIntegrationTest {

    private static final String ORDERS = """
            {"orders": [
              {"orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
               "address": "MG Road, Bangalore, Karnataka, India", "packageWeight": 10, "priority": "HIGH"},
              {"orderId": "ORD002", "latitude": 13.0827, "longitude": 80.2707,
               "address": "Anna Salai, Chennai, Tamil Nadu, India", "packageWeight": 20, "priority": "MEDIUM"}
            ]}""";

    private static final String VEHICLES = """
            {"vehicles": [
              {"vehicleId": "VEH001", "capacity": 100, "currentLatitude": 12.9716, "currentLongitude": 77.6413,
               "currentAddress": "Indiranagar, Bangalore, Karnataka, India"},
              {"vehicleId": "VEH002", "capacity": 150, "currentLatitude": 13.0674, "currentLongitude": 80.2376,
               "currentAddress": "T Nagar, Chennai, Tamil Nadu, India"}
            ]}""";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DeliveryOrderRepository orderRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String resource(String name) throws IOException {
        return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        void submitOrdersReturnsSpecResponse() throws Exception {
            postJson("/api/dispatch/orders", ORDERS)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Delivery orders accepted.")))
                    .andExpect(jsonPath("$.status", is("success")));
        }

        @Test
        void submitVehiclesReturnsSpecResponse() throws Exception {
            postJson("/api/dispatch/vehicles", VEHICLES)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", is("Vehicle details accepted.")))
                    .andExpect(jsonPath("$.status", is("success")));
        }

        @Test
        void planMatchesTheSpecExample() throws Exception {
            postJson("/api/dispatch/vehicles", VEHICLES).andExpect(status().isOk());
            postJson("/api/dispatch/orders", ORDERS).andExpect(status().isOk());

            mvc.perform(get("/api/dispatch/plan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dispatchPlan", hasSize(2)))
                    .andExpect(jsonPath("$.dispatchPlan[0].vehicleId", is("VEH001")))
                    .andExpect(jsonPath("$.dispatchPlan[0].totalLoad", is(10.0)))
                    .andExpect(jsonPath("$.dispatchPlan[0].totalDistance", is("5.06 km")))
                    .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].orderId", is("ORD001")))
                    .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].address",
                            is("MG Road, Bangalore, Karnataka, India")))
                    .andExpect(jsonPath("$.dispatchPlan[1].vehicleId", is("VEH002")))
                    .andExpect(jsonPath("$.dispatchPlan[1].totalLoad", is(20.0)))
                    .andExpect(jsonPath("$.dispatchPlan[1].assignedOrders[0].orderId", is("ORD002")))
                    .andExpect(jsonPath("$.unassignedOrders", hasSize(0)));
        }

        @Test
        void fullSampleDatasetIsDispatched() throws Exception {
            postJson("/api/dispatch/vehicles", resource("sample-vehicles.json")).andExpect(status().isOk());
            postJson("/api/dispatch/orders", resource("sample-orders.json")).andExpect(status().isOk());

            mvc.perform(get("/api/dispatch/orders")).andExpect(jsonPath("$", hasSize(30)));
            mvc.perform(get("/api/dispatch/plan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dispatchPlan", hasSize(5)))
                    .andExpect(jsonPath("$.unassignedOrders[*].priority", hasItem("LOW")));
        }

        @Test
        void resubmittingAnOrderUpdatesItInsteadOfDuplicating() throws Exception {
            postJson("/api/dispatch/orders", ORDERS).andExpect(status().isOk());
            postJson("/api/dispatch/orders", """
                    {"orders": [{"orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
                      "address": "MG Road, Bangalore", "packageWeight": 99, "priority": "LOW"}]}""")
                    .andExpect(status().isOk());

            mvc.perform(get("/api/dispatch/orders"))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].packageWeight", is(99.0)))
                    .andExpect(jsonPath("$[0].priority", is("LOW")));
        }

        @Test
        void priorityIsCaseInsensitive() throws Exception {
            postJson("/api/dispatch/orders", ORDERS.replace("\"HIGH\"", "\"high\"")).andExpect(status().isOk());
        }

        @Test
        void planWithNoDataIsEmpty() throws Exception {
            mvc.perform(get("/api/dispatch/plan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dispatchPlan", hasSize(0)))
                    .andExpect(jsonPath("$.unassignedOrders", hasSize(0)));
        }

        @Test
        void ordersWithoutVehiclesAreReportedUnassigned() throws Exception {
            postJson("/api/dispatch/orders", ORDERS).andExpect(status().isOk());
            mvc.perform(get("/api/dispatch/plan"))
                    .andExpect(jsonPath("$.unassignedOrders", hasSize(2)))
                    .andExpect(jsonPath("$.unassignedOrders[0].reason", is("No vehicles available")));
        }

        @Test
        void deleteEndpointsResetData() throws Exception {
            postJson("/api/dispatch/orders", ORDERS);
            postJson("/api/dispatch/vehicles", VEHICLES);
            mvc.perform(delete("/api/dispatch/orders")).andExpect(status().isOk());
            mvc.perform(delete("/api/dispatch/vehicles")).andExpect(status().isOk());
            mvc.perform(get("/api/dispatch/orders")).andExpect(jsonPath("$", hasSize(0)));
            mvc.perform(get("/api/dispatch/vehicles")).andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void rootRedirectsToSwagger() throws Exception {
            mvc.perform(get("/")).andExpect(redirectedUrl("/swagger-ui/index.html"));
        }

        @Test
        void naivePlanIsAvailableAsAnOption() throws Exception {
            postJson("/api/dispatch/vehicles", resource("sample-vehicles.json")).andExpect(status().isOk());
            postJson("/api/dispatch/orders", resource("sample-orders.json")).andExpect(status().isOk());

            mvc.perform(get("/api/dispatch/plan").param("strategy", "naive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dispatchPlan", hasSize(5)));
        }

        @Test
        void comparisonShowsSmartSavesDistance() throws Exception {
            postJson("/api/dispatch/vehicles", resource("sample-vehicles.json")).andExpect(status().isOk());
            postJson("/api/dispatch/orders", resource("sample-orders.json")).andExpect(status().isOk());

            mvc.perform(get("/api/dispatch/plan/comparison"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.smart.strategy", is("smart")))
                    .andExpect(jsonPath("$.naive.strategy", is("naive")))
                    .andExpect(jsonPath("$.smart.highPriorityDispatched", is(12)))
                    .andExpect(jsonPath("$.distanceSavedPercent", org.hamcrest.Matchers.greaterThan(0.0)))
                    .andExpect(jsonPath("$.summary", containsString("Smart dispatch drives")));
        }

        @Test
        void comparisonWithNoDataIsHelpful() throws Exception {
            mvc.perform(get("/api/dispatch/plan/comparison"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary", is("Add orders and vehicles to compare strategies.")));
        }

        @Test
        void dashboardIsServed() throws Exception {
            mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/dashboard/index.html"));
            mvc.perform(get("/dashboard/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(containsString("Dispatch Control Room")));
        }

        @Test
        void openApiDocsAreServed() throws Exception {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paths['/api/dispatch/plan']").exists());
        }
    }

    @Nested
    @DisplayName("Validation and errors")
    class Errors {

        @Test
        void invalidCoordinatesAndWeightAreRejectedWithFieldErrors() throws Exception {
            postJson("/api/dispatch/orders", """
                    {"orders": [{"orderId": "ORD1", "latitude": 95, "longitude": 200,
                      "address": "x", "packageWeight": -5, "priority": "HIGH"}]}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is("error")))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.errors", hasSize(3)))
                    .andExpect(jsonPath("$.errors", hasItem("orders[0].latitude: latitude must be between -90 and 90")))
                    .andExpect(jsonPath("$.errors", hasItem("orders[0].packageWeight: packageWeight must be greater than 0")));
        }

        @Test
        void missingFieldsAreRejected() throws Exception {
            postJson("/api/dispatch/orders", "{\"orders\": [{\"orderId\": \"\"}]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("orders[0].orderId: orderId is required")))
                    .andExpect(jsonPath("$.errors", hasItem("orders[0].priority: priority is required (HIGH, MEDIUM or LOW)")));
        }

        @Test
        void emptyOrderListIsRejected() throws Exception {
            postJson("/api/dispatch/orders", "{\"orders\": []}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]", is("orders: orders must contain at least one order")));
        }

        @Test
        void unknownPriorityGivesHelpfulMessage() throws Exception {
            postJson("/api/dispatch/orders", ORDERS.replace("\"HIGH\"", "\"URGENT\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message",
                            is("Invalid value 'URGENT' for field 'orders[0].priority'. Allowed values: HIGH, MEDIUM, LOW")));
        }

        @Test
        void wrongTypeIsRejected() throws Exception {
            postJson("/api/dispatch/vehicles", VEHICLES.replace("\"capacity\": 100", "\"capacity\": \"lots\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("vehicles[0].capacity")));
        }

        @Test
        void malformedJsonIsRejected() throws Exception {
            postJson("/api/dispatch/orders", "{\"orders\": [")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Malformed JSON request body")));
        }

        @Test
        void duplicateVehicleIdsAreRejected() throws Exception {
            postJson("/api/dispatch/vehicles", VEHICLES.replace("VEH002", "VEH001"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Duplicate vehicleId values in request")))
                    .andExpect(jsonPath("$.errors[0]", is("vehicleId 'VEH001' appears more than once")));
        }

        @Test
        void invalidVehicleCapacityIsRejected() throws Exception {
            postJson("/api/dispatch/vehicles", VEHICLES.replace("\"capacity\": 100", "\"capacity\": 0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem("vehicles[0].capacity: capacity must be greater than 0")));
        }

        @Test
        void wrongContentTypeIsRejected() throws Exception {
            mvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content(ORDERS))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void unsupportedMethodIsRejected() throws Exception {
            mvc.perform(put("/api/dispatch/plan")).andExpect(status().isMethodNotAllowed());
        }

        @Test
        void unknownStrategyIsRejected() throws Exception {
            mvc.perform(get("/api/dispatch/plan").param("strategy", "magic"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Unknown strategy 'magic'")))
                    .andExpect(jsonPath("$.errors[0]", is("Allowed values: smart, naive")));
        }

        @Test
        void unknownPathReturns404Json() throws Exception {
            mvc.perform(get("/api/dispatch/unknown"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is("error")));
        }
    }
}
