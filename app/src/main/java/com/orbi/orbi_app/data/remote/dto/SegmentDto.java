package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SegmentDto {
    @SerializedName("distanceMeters")
    private Double distanceMeters;

    @SerializedName("durationSeconds")
    private Double durationSeconds;

    @SerializedName("steps")
    private List<StepDto> steps;

    public Double getDistanceMeters() {
        return distanceMeters != null ? distanceMeters : 0.0;
    }

    public Double getDurationSeconds() {
        return durationSeconds != null ? durationSeconds : 0.0;
    }

    public List<StepDto> getSteps() {
        return steps;
    }
}
