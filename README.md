# Dispatch Load Balancer

A Spring Boot REST API that assigns delivery orders to a fleet of vehicles. It never exceeds a vehicle's capacity, assigns HIGH-priority orders first, and keeps total travel distance low using the Haversine formula.

- **Live URL:** `https://<your-app>.onrender.com` (the root redirects to Swagger UI)
- **Swagger UI:** `/swagger-ui/index.html` · **OpenAPI JSON:** `/v3/api-docs`
- **Postman collection:** [`postman/Dispatch-Load-Balancer.postman_collection.json`](postman/Dispatch-Load-Balancer.postman_collection.json)
- **Optional visualiser:** `/dashboard`, a map of the dispatch plan (see below)

---

## Dispatch Control Room (optional extra)

The assignment asks for REST APIs with no UI, and the APIs are complete without it: everything is testable in Swagger and Postman, and the root URL opens Swagger. On top of that I added a small, dependency-free page at **`/dashboard`** to make the algorithm's decisions *visible*. It is purely a client of the public endpoints and contains no business logic.

- **Map of the plan:** each truck's route is drawn from its start through its stops in delivery order. Stops are numbered and coloured by priority, and orders that couldn't be dispatched are shown as dashed markers with the reason.
- **Click the map to add an order or a truck:** it is POSTed to the API and the plan is recalculated immediately, so you can watch the optimiser react (for example, drop a heavy HIGH order and see a LOW order get bumped).
- **Scenarios:** load the assignment's Delhi sample, or generate a random scenario using real neighbourhoods in Bengaluru, Mumbai, Delhi NCR or Chennai.
- **Smart vs naive:** the comparison as a bar chart, with a toggle to draw the naive plan on the map, where its criss-crossing routes make the difference obvious.
- **Summary:** orders dispatched, total driving distance, fleet capacity used, and weight left at the depot. Click a truck card to highlight its route.
- **API errors are shown as they come back** (for example, a weight of 0 shows the server's validation message), demonstrating the error handling.

It is built with plain HTML, CSS and JavaScript plus Leaflet and OpenStreetMap tiles, and is served as static files from `src/main/resources/static/dashboard`.

---

## Tech stack

Java 17 · Spring Boot 3.5 · Spring Data JPA · H2 (in-memory) · Bean Validation · springdoc-openapi (Swagger) · JUnit 5 / Mockito / MockMvc · JaCoCo · Docker

## Run locally

```bash
mvn spring-boot:run                 # http://localhost:8080  ->  Swagger UI
# or
mvn clean package && java -jar target/dispatch-load-balancer.jar
# or
docker build -t dispatch . && docker run -p 8080:8080 dispatch
```

## Run tests

```bash
mvn clean verify
```
This runs 56 tests (unit and integration) and writes a coverage report to `target/site/jacoco/index.html` (about 95% line coverage).

---

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/dispatch/orders` | Submit delivery orders (upsert by `orderId`) |
| POST | `/api/dispatch/vehicles` | Submit fleet details (upsert by `vehicleId`) |
| GET | `/api/dispatch/plan` | Get the optimised dispatch plan (optional `?strategy=naive` for the baseline) |
| GET | `/api/dispatch/plan/comparison` | Compare the smart plan with a naive baseline *(extra)* |
| GET | `/api/dispatch/orders` | List stored orders *(extra)* |
| GET | `/api/dispatch/vehicles` | List stored vehicles *(extra)* |
| DELETE | `/api/dispatch/orders` | Delete all orders, for resetting between tests *(extra)* |
| DELETE | `/api/dispatch/vehicles` | Delete all vehicles *(extra)* |

The three required endpoints use exactly the request and response formats in the assignment. The plan response also includes an `unassignedOrders` array, so orders that cannot fit are reported explicitly instead of disappearing.

### Example: `GET /api/dispatch/plan`
```json
{
  "dispatchPlan": [
    {
      "vehicleId": "VEH001",
      "totalLoad": 10.0,
      "totalDistance": "5.06 km",
      "assignedOrders": [
        { "orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
          "address": "MG Road, Bangalore, Karnataka, India", "packageWeight": 10.0, "priority": "HIGH" }
      ]
    },
    {
      "vehicleId": "VEH002",
      "totalLoad": 20.0,
      "totalDistance": "3.97 km",
      "assignedOrders": [
        { "orderId": "ORD002", "latitude": 13.0827, "longitude": 80.2707,
          "address": "Anna Salai, Chennai, Tamil Nadu, India", "packageWeight": 20.0, "priority": "MEDIUM" }
      ]
    }
  ],
  "unassignedOrders": []
}
```

### Error format
Every error returns the same JSON shape with a suitable HTTP status (400, 404, 405, 415 or 500):
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": [
    "orders[0].latitude: latitude must be between -90 and 90",
    "orders[0].packageWeight: packageWeight must be greater than 0"
  ],
  "timestamp": "2026-11-20T08:15:00Z"
}
```
Handled cases: missing or blank fields; latitude and longitude out of range; weight or capacity ≤ 0; unknown priority (the error lists the allowed values); wrong data types; malformed JSON; an empty list; duplicate IDs within one request; wrong Content-Type; unsupported HTTP method; unknown path.

---

## The algorithm

Assigning orders to capacity-limited vehicles while minimising distance is the **Capacitated Vehicle Routing Problem**, which is NP-hard, so an exact solution doesn't scale. I used a **fast, deterministic, explainable greedy heuristic**:

1. **Sort orders** by priority (HIGH → MEDIUM → LOW), then by weight descending (heavy packages are harder to fit, so they are placed while space is still free), then by `orderId` so the output is deterministic.
2. **Assign each order to the nearest vehicle that still has room.** "Nearest" is measured from the vehicle's *last planned stop* (or its start position if it has none yet). This makes the choice route-aware: a vehicle already heading to Noida naturally picks up more Noida orders. Ties go to the vehicle with the least spare capacity (best fit), which keeps bigger vehicles free for bigger packages.
3. **Order each vehicle's stops** with the nearest-neighbour heuristic, then report `totalDistance` = start → stop 1 → stop 2 → … (Haversine).
4. **Report** any order that can't be placed in `unassignedOrders`, with a reason: *exceeds the largest vehicle capacity*, *insufficient remaining capacity in all vehicles*, or *no vehicles available*.

Because HIGH orders are placed first, a lower-priority order can never take capacity that a higher-priority order needed.

**Complexity:** O(n log n + n·v + Σk²) for n orders, v vehicles and k stops per vehicle. This easily handles thousands of orders per request.

**Result on the assignment's sample data** (30 orders, 645 kg in total; 5 vehicles, 500 kg capacity in total): every HIGH (275 kg) and MEDIUM (145 kg) order is assigned, 490 of the 500 kg of capacity is used, no vehicle is over capacity, and the remaining LOW orders are returned as unassigned.

### How much does the algorithm help? (`GET /api/dispatch/plan/comparison`)

To show the value of the heuristic rather than just claim it, the API also runs a **naive baseline**, the way dispatch is often done by hand: orders are taken first come, first served, trucks take turns, and priority and location are ignored. It still respects capacity, so the comparison is fair. Both run on the same data:

| On the assignment's sample data | Smart | Naive |
|---|---|---|
| Orders dispatched | 22 / 30 | 22 / 30 |
| HIGH-priority dispatched | **12 / 12** | 9 / 12 |
| Total driving | **73.4 km** | 380.7 km |
| km per delivered order | **3.34** | 17.30 |

**80.7% less driving per delivery, and no urgent order left behind.** km per delivered order is used because two plans can deliver a different number of orders. The baseline is a second implementation of the same `DispatchStrategy` interface. `DispatchService` receives every strategy through Spring and looks them up by type, so adding a third (for example OR-Tools) needs no controller changes. You can also view the naive plan with `GET /api/dispatch/plan?strategy=naive`.

### Assumptions
- `packageWeight` and `capacity` use the same unit (for example kg). Decimal values are allowed.
- Distance is straight-line (great-circle) distance using the Haversine formula, Earth radius 6371 km.
- Routes are open: the vehicle doesn't return to its start, matching "travel distance to drop points".
- Each order goes to one vehicle and is not split.
- Re-submitting an existing `orderId` or `vehicleId` updates it (upsert). A request that contains the same ID twice is rejected as ambiguous.
- The plan is computed when requested from the stored data, so it always reflects the latest orders and vehicles.

---

## Design

```
controller/   DispatchController (REST + Swagger docs), HomeController (/ → Swagger)
service/      OrderService, VehicleService (validated bulk upsert), DispatchService
optimizer/    DispatchStrategy ← GreedyPriorityDispatchStrategy (smart), NaiveRoundRobinDispatchStrategy (baseline)
              PlanMetrics (km per order, HIGH dispatched, capacity used)
              RouteOptimizer   ← NearestNeighbourRouteOptimizer
              DistanceCalculator ← HaversineDistanceCalculator
model/        DeliveryOrder, Vehicle (JPA entities), Priority
dto/          request and response records with Bean Validation
mapper/       DispatchMapper (entity ↔ DTO ↔ result)
exception/    GlobalExceptionHandler, InvalidRequestException
```

**Patterns used**
- **Strategy:** `DispatchStrategy` (two implementations, selectable at runtime), `RouteOptimizer` and `DistanceCalculator` are interfaces, so the algorithm, routing or distance source (for example a road-distance API) can be swapped without touching the API or service code.
- **Repository:** Spring Data JPA.
- **DTO + Mapper:** the API contract is kept separate from the persistence model.
- **Centralised error handling:** `@RestControllerAdvice` gives one consistent error format.
- **Dependency injection:** constructor injection throughout, which also makes classes easy to unit test.

**Efficient database operations:** an upsert uses *one* `SELECT … WHERE id IN (…)` to find existing rows. Existing rows are updated through JPA dirty checking, and new rows are inserted directly. The entities implement `Persistable`, which avoids Spring Data's per-row `merge()` lookups. Hibernate JDBC batching is enabled (`batch_size: 50`).

## Tests
| Test class | What it covers |
|---|---|
| `HaversineDistanceCalculatorTest` | known distances (Delhi–Mumbai, MG Road–Indiranagar), zero, symmetry, antipodal points |
| `NearestNeighbourRouteOptimizerTest` | stop order, total distance, empty route, stops at the same location |
| `GreedyPriorityDispatchStrategyTest` | capacity limits (including exact fit), priority wins scarce capacity, nearest vehicle chosen, fallback to the next vehicle, unassigned reasons, no orders or no vehicles, determinism, the full 30-order sample |
| `NaiveRoundRobinDispatchStrategyTest` | baseline behaviour (turn-taking, ignores priority, no reordering), and smart beats naive on the sample |
| `OrderServiceTest` | duplicate-ID rejection, upsert inserts new and updates existing rows with a single lookup |
| `DispatchControllerIntegrationTest` | every endpoint end to end, the spec example, upsert, case-insensitive priority, strategy comparison, and 12 error scenarios |

## Possible improvements
- 2-opt or OR-Tools local search to shorten routes further; a time-window or maximum-route-length constraint.
- Road distance from a maps API, implemented as another `DistanceCalculator`.
- PostgreSQL for persistent storage (only `application.yml` changes).
- Saved plan snapshots with IDs, and pagination for very large fleets.
