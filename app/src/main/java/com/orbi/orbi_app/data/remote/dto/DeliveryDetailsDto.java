package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DeliveryDetailsDto {

    @SerializedName("details")
    private String details;

    public DeliveryDetailsDto() {
    }

    public DeliveryDetailsDto(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
