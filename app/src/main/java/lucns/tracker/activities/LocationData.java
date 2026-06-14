package lucns.tracker.activities;

public class LocationData {

    public static final int ACCURACY_COEFFICIENT = 10;

    public double latitude, longitude;
    public int accuracy, azimuth;
    public long capturedAt;
    public int satellites;
    public boolean gnssFix;
    public boolean isOlder;

    public LocationData(int satellites) {
        this.satellites = satellites;
    }

    public LocationData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocationData(boolean gnssFix, double latitude, double longitude, int accuracy, int azimuth, int satellites, long capturedAt) {
        this.gnssFix = gnssFix;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.azimuth = azimuth;
        this.capturedAt = capturedAt;
        this.satellites = satellites;
    }

    public static LocationData from(LocationData locationData) {
        LocationData data =  new LocationData(locationData.gnssFix, locationData.latitude, locationData.longitude, locationData.accuracy, locationData.azimuth, locationData.satellites, locationData.capturedAt);
        data.isOlder = true;
        return data;
    }

    public double getHdop() {
        return accuracy / ACCURACY_COEFFICIENT;
    }
}
