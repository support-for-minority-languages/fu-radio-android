package com.udmspell.furadio;

/**
 * Created by ggrigoryev on 21.05.15.
 */
public class Consts {
    public static final String PLAYER_COMMAND = "com.udmspell.furadio.command";
    public static final String STATION_URL = "com.udmspell.furadio.station_url";
    public static final String RECEIVER_ACTION = "com.udmspell.furadio.receiver_action";
    public static final String STATION_TITLE = "station_title";
    public static final String SAVED_URL = "saved_url";
    public static final int NOTIFICATION_ID = 1;
    public static final String LOG_TAG = "FU_RADIO";
    public static final String STOP_SERVICE = "STOP_SERVICE";
    public static final String ENDPOINT = "http://udmspell.ru";
    public static final double CLOUD_WIDTH_SCALE = 0.75;
    public static final float DEFAULT_ROUND_SCALE = 1;
    public static final float LOAD_ROUND_SCALE = 3;

    public class PlayerCommands {
        public static final String IS_PLAYING = "COMMAND_PLAY";
        public static final String PAUSE = "COMMAND_PAUSE";
        public static final String PREPARE = "COMMAND_PREPARE";
    }
}
