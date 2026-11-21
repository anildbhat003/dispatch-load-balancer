package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;

import java.util.List;

/**
 * Strategy for assigning orders to vehicles. The service layer depends only on this interface, so
 * a more advanced optimiser (e.g. OR-Tools / metaheuristics) can replace the default without any
 * change to the API.
 */
public interface DispatchStrategy {

    /** Identifies the strategy so callers can choose one by name (e.g. {@code ?strategy=naive}). */
    StrategyType type();

    DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles);
}
