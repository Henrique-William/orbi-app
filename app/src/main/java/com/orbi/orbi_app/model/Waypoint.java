package com.orbi.orbi_app.model;

import java.io.Serializable;

public class Waypoint implements Serializable {
    private String id;
    private long deliveryId; // ID numérico na Orbi API
    private String routeId;
    private int sequenceOrder;
    private String recipientName;
    private String recipientPhone;
    private String addressStreet;
    private String addressNumber;
    private String neighborhood;
    private String city;
    private double latitude;
    private double longitude;
    private WaypointStatus status;
    private String justification;
    private Long completedAt;

    public Waypoint(String id, String routeId, int sequenceOrder, String recipientName,
                    String recipientPhone, String addressStreet, String addressNumber,
                    String neighborhood, String city, double latitude, double longitude,
                    WaypointStatus status) {
        this.id = id;
        try {
            this.deliveryId = Long.parseLong(id.replaceAll("\\D+", ""));
        } catch (Exception e) {
            this.deliveryId = sequenceOrder;
        }
        this.routeId = routeId;
        this.sequenceOrder = sequenceOrder;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.addressStreet = addressStreet;
        this.addressNumber = addressNumber;
        this.neighborhood = neighborhood;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status != null ? status : WaypointStatus.AT_PICKUP;
    }

    public Waypoint(long deliveryId, String routeId, int sequenceOrder, String fullAddress,
                    double latitude, double longitude, WaypointStatus status) {
        this.id = String.valueOf(deliveryId);
        this.deliveryId = deliveryId;
        this.routeId = routeId;
        this.sequenceOrder = sequenceOrder;
        this.recipientName = "Destinatário #" + sequenceOrder;
        this.recipientPhone = "(11) 9" + (9000 + sequenceOrder) + "-0000";
        this.addressStreet = fullAddress;
        this.addressNumber = "";
        this.neighborhood = "";
        this.city = "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status != null ? status : WaypointStatus.AT_PICKUP;
    }

    public String getId() {
        return id;
    }

    public long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getRouteId() {
        return routeId;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public void setAddressNumber(String addressNumber) {
        this.addressNumber = addressNumber;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public WaypointStatus getStatus() {
        return status;
    }

    public void setStatus(WaypointStatus status) {
        this.status = status;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    public boolean isAtPickup() {
        return status != null && status.isAtPickup();
    }

    public boolean isInTransit() {
        return status != null && status.isInTransit();
    }

    public boolean isDelivered() {
        return status == WaypointStatus.DELIVERED || status == WaypointStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == WaypointStatus.CANCELLED;
    }

    public String getFullAddress() {
        if (addressNumber.isEmpty() && neighborhood.isEmpty()) {
            return addressStreet;
        }
        return addressStreet + ", " + addressNumber + " - " + neighborhood + ", " + city;
    }

    public String getFormattedSequence() {
        return String.format("#%02d", sequenceOrder);
    }
}
