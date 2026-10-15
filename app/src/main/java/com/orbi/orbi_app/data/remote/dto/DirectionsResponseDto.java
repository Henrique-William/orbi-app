package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponseDto {
    @SerializedName("routeId")
    private Long routeId;

    @SerializedName("distanceMeters")
    private Double distanceMeters;

    @SerializedName("durationSeconds")
    private Double durationSeconds;

    @SerializedName("geometry")
    private GeometryDto geometry;

    @SerializedName("segments")
    private List<SegmentDto> segments;

    public Long getRouteId() {
        return routeId;
    }

    public Double getDistanceMeters() {
        return distanceMeters != null ? distanceMeters : 0.0;
    }

    public Double getDurationSeconds() {
        return durationSeconds != null ? durationSeconds : 0.0;
    }

    public GeometryDto getGeometry() {
        return geometry;
    }

    public List<SegmentDto> getSegments() {
        return segments;
    }
}
