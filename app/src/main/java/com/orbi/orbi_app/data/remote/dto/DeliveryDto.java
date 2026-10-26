package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DeliveryDto {
    @SerializedName("id")
    private Long id;

    @SerializedName("order")
    private Integer order;

    @SerializedName("status")
    private String status;

    @SerializedName("address")
    private String address;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("recipientEmail")
    private String recipientEmail;

    @SerializedName("pickedUpAt")
    private String pickedUpAt;

    @SerializedName("deliveredAt")
    private String deliveredAt;

    public Long getId() {
        return id;
    }

    public Integer getOrder() {
        return order != null ? order : 1;
    }

    public String getStatus() {
        return status;
    }

    public String getAddress() {
        return address != null ? address : "Endereço não informado";
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getPickedUpAt() {
        return pickedUpAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }
}
