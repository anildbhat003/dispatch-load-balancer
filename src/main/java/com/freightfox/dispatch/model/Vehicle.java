package com.freightfox.dispatch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @Id
    @Column(name = "vehicle_id", length = 64)
    private String vehicleId;

    @Column(nullable = false)
    private double capacity;

    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;

    @Column(name = "current_address", nullable = false, length = 500)
    private String currentAddress;

    protected Vehicle() {
        // for JPA
    }

    public Vehicle(String vehicleId, double capacity, double currentLatitude,
                   double currentLongitude, String currentAddress) {
        this.vehicleId = vehicleId;
        updateDetails(capacity, currentLatitude, currentLongitude, currentAddress);
    }

    public final void updateDetails(double capacity, double currentLatitude,
                                    double currentLongitude, String currentAddress) {
        this.capacity = capacity;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
    }

    @Override
    public String getId() {
        return vehicleId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    @Override
    public String toString() {
        return "Vehicle{" + vehicleId + ", capacity=" + capacity + "}";
    }
}
