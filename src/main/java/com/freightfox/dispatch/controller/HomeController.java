package com.freightfox.dispatch.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Root URL goes to the Swagger UI (the API is the product). {@code /dashboard} serves an optional
 * map-based visualiser that is a pure client of the public REST endpoints.
 */
@Hidden
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/dashboard/index.html";
    }
}
