package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponseDto {
    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("expiresIn")
    private long expiresIn;

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
