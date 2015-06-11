package com.udmspell.furadio.services;

import com.udmspell.furadio.models.Station;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;

public interface StationsService {
    @GET("/assets/files/stations.json")
    void getStations(Callback<List<Station>> callback);
}
