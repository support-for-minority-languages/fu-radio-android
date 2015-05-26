package com.udmspell.furadio;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;

public interface StationsService {
    @GET("/assets/files/stations.json")
    void getStations(Callback<List<Tag>> callback);
}
