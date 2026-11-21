package com.freightfox.dispatch.optimizer;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;

/** Immutable latitude/longitude pair in decimal degrees. */
public record GeoPoint(double latitude, double longitude) {

    public static GeoPoint of(DeliveryOrder order) {
        return new GeoPoint(order.getLatitude(), order.getLongitude());
    }

    public static GeoPoint of(Vehicle vehicle) {
        return new GeoPoint(vehicle.getCurrentLatitude(), vehicle.getCurrentLongitude());
    }
}
