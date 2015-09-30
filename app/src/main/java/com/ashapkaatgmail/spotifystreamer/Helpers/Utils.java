package com.ashapkaatgmail.spotifystreamer.Helpers;

import java.util.concurrent.TimeUnit;

public final class Utils {

    public static String formatMillis(int ms) {
        String formattedMs = String.format("%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)));

        return formattedMs;
    }
}
