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

    public String getDeliveredAt() {
        return deliveredAt;
    }
}
