package com.freightfox.dispatch.optimizer;

/**
 * Strategy for computing the distance between two points, in kilometres.
 * Haversine is the default; a road-distance implementation (e.g. a maps API) could be swapped in.
 */
public interface DistanceCalculator {

    double distanceKm(GeoPoint from, GeoPoint to);
}
