package lucns.tracker.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Information {

    public static final int INTERNAL_READ_FAILURE = 0;
    public static final int EMPTY_DATA = 1;
    public static final int POWER_UP = 2;
    public static final int POWER_DOWN = 3;
    public static final int POWER_DOWN_HOME = 4;
    public static final int POWER_DOWN_WAIT_ACCURACY = 5;
    public static final int POWER_DOWN_GPS_NO_CONNECTION = 6;
    public static final int POWER_DOWN_GPS_NO_ACCURACY = 7;

    public int status, velocity;
    private long powerOnAt, powerOffAt, activeTime;

    private LocationData locationData;

    public Information(LocationData locationData, int velocity) {
        this.locationData = locationData;
        this.velocity = velocity;
    }

    public Information(LocationData locationData) {
        this.locationData = locationData;
    }

    public Information(String status) {
        setStatus(status);
    }

    public boolean hasValidLocation() {
        return locationData != null && locationData.latitude != 0 && locationData.longitude != 0;
    }

    public void setLocationData(LocationData locationData) {
        this.locationData = locationData;
    }

    public LocationData getLocationData() {
        return locationData;
    }

    public boolean hasLocation() {
        return locationData != null;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public long getActiveTime() {
        return activeTime;
    }

    public void setPowerOnAt(long powerOnAt) {
        this.powerOnAt = powerOnAt;
    }

    public long getPowerOnAt() {
        return powerOnAt;
    }

    public void setPowerOffAt(long powerOffAt) {
        this.powerOffAt = powerOffAt;
    }

    public long getPowerOffAt() {
        return powerOffAt;
    }

    public long getPowerTime() {
        return powerOnAt == 0 ? powerOffAt : powerOnAt;
    }

    public void setStatus(String name) {
        switch (name) {
            case "internal_read_failure":
                status = INTERNAL_READ_FAILURE;
                break;
            case "empty_data":
                status = EMPTY_DATA;
                break;
            case "power_up":
                status = POWER_UP;
                break;
            case "power_down":
                status = POWER_DOWN;
                break;
            case "power_down_home":
                status = POWER_DOWN_HOME;
                break;
            case "power_down_waiting_accuracy":
                status = POWER_DOWN_WAIT_ACCURACY;
                break;
            case "power_down_no_connection":
                status = POWER_DOWN_GPS_NO_CONNECTION;
                break;
            case "power_down_no_accuracy":
                status = POWER_DOWN_GPS_NO_ACCURACY;
                break;
        }
    }

    public String getWhenTime(long timestamp) {
        Date date = new Date(timestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public String getPassedTime(long timestamp) { // in seconds
        long m = 0;
        long h = 0;
        long d = 0;
        long s = timestamp;
        while (s >= 86400) {
            d++;
            s -= 86400;
        }
        while (s >= 3600) {
            h++;
            s -= 3600;
        }
        while (s >= 60) {
            m++;
            s -= 60;
        }
        if (d > 0 && h > 0) return  d + "d " + h + "h";
        else if (d > 0) return d + "d";
        else if (h > 0 && m > 0) return h + "h " + m + "m";
        else if (h > 0) return h + "h";
        else if (m > 0 && s > 0) return m + "m " + s + "s";
        else if (m > 0) return m + "m";
        else return s + "s";
    }
}
