package com.freightfox.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchOpenApi() {
        return new OpenAPI()
                // relative URL so "Try it out" works on localhost and on the deployed host alike
                .servers(List.of(new Server().url("/").description("Current host")))
                .info(new Info()
                        .title("Dispatch Load Balancer API")
                        .version("1.0.0")
                        .description("""
                                Assigns delivery orders to a fleet of vehicles.

                                **Rules:** vehicle capacity is never exceeded, HIGH priority orders are \
                                assigned first, and total travel distance (Haversine) is kept low.

                                **Try it:** 1) POST /api/dispatch/vehicles 2) POST /api/dispatch/orders \
                                3) GET /api/dispatch/plan

                                **Optional visualiser:** [/dashboard](/dashboard) draws the plan on a map. \
                                It is only a client of these endpoints; every API remains fully testable here \
                                and in Postman.""")
                        .contact(new Contact().name("Anil Bhat")));
    }
}
