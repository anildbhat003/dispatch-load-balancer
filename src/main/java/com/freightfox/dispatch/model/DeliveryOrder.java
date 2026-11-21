package com.freightfox.dispatch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrder extends BaseEntity {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "package_weight", nullable = false)
    private double packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    protected DeliveryOrder() {
        // for JPA
    }

    public DeliveryOrder(String orderId, double latitude, double longitude, String address,
                         double packageWeight, Priority priority) {
        this.orderId = orderId;
        updateDetails(latitude, longitude, address, packageWeight, priority);
    }

    /** Updates mutable fields; used by the upsert flow so managed entities are dirty-checked. */
    public final void updateDetails(double latitude, double longitude, String address,
                                    double packageWeight, Priority priority) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.packageWeight = packageWeight;
        this.priority = priority;
    }

    @Override
    public String getId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public double getPackageWeight() {
        return packageWeight;
    }

    public Priority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "DeliveryOrder{" + orderId + ", " + packageWeight + "kg, " + priority + "}";
    }
}
