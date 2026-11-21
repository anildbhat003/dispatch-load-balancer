package com.freightfox.dispatch.optimizer;

import org.springframework.stereotype.Component;

/**
 * Great-circle distance between two coordinates using the Haversine formula.
 *
 * <pre>
 * a = sin²(Δφ/2) + cos φ1 · cos φ2 · sin²(Δλ/2)
 * d = 2R · asin(√a)
 * </pre>
 */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    /** Mean radius of the Earth in kilometres. */
    static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.latitude());
        double lat2 = Math.toRadians(to.latitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to.longitude() - from.longitude());

        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        // min() guards against floating-point values marginally above 1.0
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
