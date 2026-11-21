package com.freightfox.dispatch.optimizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    @DisplayName("Distance from a point to itself is zero")
    void samePointIsZero() {
        GeoPoint p = new GeoPoint(28.6139, 77.2090);
        assertThat(calculator.distanceKm(p, p)).isZero();
    }

    @Test
    @DisplayName("Delhi to Mumbai is about 1148 km")
    void delhiToMumbai() {
        GeoPoint delhi = new GeoPoint(28.6139, 77.2090);
        GeoPoint mumbai = new GeoPoint(19.0760, 72.8777);
        assertThat(calculator.distanceKm(delhi, mumbai)).isCloseTo(1148.09, within(0.5));
    }

    @Test
    @DisplayName("Short city distance: MG Road to Indiranagar is about 5.06 km")
    void shortDistance() {
        GeoPoint mgRoad = new GeoPoint(12.9716, 77.5946);
        GeoPoint indiranagar = new GeoPoint(12.9716, 77.6413);
        assertThat(calculator.distanceKm(mgRoad, indiranagar)).isCloseTo(5.06, within(0.01));
    }

    @Test
    @DisplayName("Distance is symmetric")
    void symmetric() {
        GeoPoint a = new GeoPoint(28.7041, 77.1025);
        GeoPoint b = new GeoPoint(28.4595, 77.0266);
        assertThat(calculator.distanceKm(a, b)).isEqualTo(calculator.distanceKm(b, a));
    }

    @Test
    @DisplayName("Antipodal points are half the Earth's circumference apart")
    void antipodal() {
        double expected = Math.PI * HaversineDistanceCalculator.EARTH_RADIUS_KM;
        assertThat(calculator.distanceKm(new GeoPoint(0, 0), new GeoPoint(0, 180))).isCloseTo(expected, within(0.001));
    }
}
