package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RouteResponseDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("driverId")
    private DriverInfoDto driverInfo;

    @SerializedName("driverName")
    private String driverName;

    @SerializedName("status")
    private String status;

    @SerializedName("totalDistance")
    private Double totalDistance;

    @SerializedName("totalCost")
    private Double totalCost;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("deliveries")
    private List<DeliveryDto> deliveries;

    public Long getId() {
        return id;
    }

    public DriverInfoDto getDriverInfo() {
        return driverInfo;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getStatus() {
        return status;
    }

    public Double getTotalDistance() {
        return totalDistance != null ? totalDistance : 0.0;
    }

    public Double getTotalCost() {
        return totalCost != null ? totalCost : 0.0;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<DeliveryDto> getDeliveries() {
        return deliveries;
    }
}
