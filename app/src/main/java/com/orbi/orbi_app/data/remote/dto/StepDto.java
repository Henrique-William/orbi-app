package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class StepDto {
    @SerializedName("name")
    private String name;

    @SerializedName("instruction")
    private String instruction;

    @SerializedName("distanceMeters")
    private Double distanceMeters;

    @SerializedName("durationSeconds")
    private Double durationSeconds;

    public String getName() {
        return name;
    }

    public String getInstruction() {
        return instruction;
    }

    public Double getDistanceMeters() {
        return distanceMeters != null ? distanceMeters : 0.0;
    }

    public Double getDurationSeconds() {
        return durationSeconds != null ? durationSeconds : 0.0;
    }
}
