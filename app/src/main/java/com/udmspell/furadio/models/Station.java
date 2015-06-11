package com.udmspell.furadio.models;

import com.orm.SugarRecord;

public class Station extends SugarRecord<Station> {

    int stationId;
    String text;
    String url;
    int popularity;

    public Station() {

    }

    public int getStationId() {
        return stationId;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public int getPopularity() {
        return popularity;
    }

    public Station(int stationId, String text, int popularity, String url) {
        this.stationId = stationId;
        this.text = text;
        this.popularity = popularity;
        this.url = url;
    }


}
