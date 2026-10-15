package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DriverInfoDto {
    @SerializedName("userId")
    private String userId;

    @SerializedName("driverName")
    private String driverName;

    @SerializedName("cnh")
    private String cnh;

    @SerializedName("phone")
    private String phone;

    @SerializedName("profileCreatedAt")
    private String profileCreatedAt;

    public String getUserId() {
        return userId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getCnh() {
        return cnh;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileCreatedAt() {
        return profileCreatedAt;
    }
}
