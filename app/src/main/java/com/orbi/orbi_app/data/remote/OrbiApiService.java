package com.orbi.orbi_app.data.remote;

import com.orbi.orbi_app.data.remote.dto.DirectionsResponseDto;
import com.orbi.orbi_app.data.remote.dto.LoginRequestDto;
import com.orbi.orbi_app.data.remote.dto.LoginResponseDto;
import com.orbi.orbi_app.data.remote.dto.RouteResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OrbiApiService {

    @POST("api/auth/login")
    Call<LoginResponseDto> login(@Body LoginRequestDto request);

    @GET("api/auth/validate")
    Call<Void> validateToken();

    @GET("api/route")
    Call<List<RouteResponseDto>> getRoutes();

    @GET("api/route/{id}")
    Call<RouteResponseDto> getRouteById(@Path("id") long id);

    @GET("api/route/{id}/directions")
    Call<DirectionsResponseDto> getRouteDirections(@Path("id") long id);

    @PATCH("api/deliveries/{id}/next-status")
    Call<Void> advanceDeliveryStatus(@Path("id") long id);

    @PATCH("api/deliveries/{id}/cancel")
    Call<Void> cancelDelivery(@Path("id") long id);
}
