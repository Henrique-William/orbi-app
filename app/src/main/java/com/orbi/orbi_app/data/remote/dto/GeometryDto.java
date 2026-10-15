package com.orbi.orbi_app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeometryDto {
    @SerializedName("type")
    private String type;

    @SerializedName("coordinates")
    private List<List<Double>> coordinates;

    public String getType() {
        return type;
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }
}
