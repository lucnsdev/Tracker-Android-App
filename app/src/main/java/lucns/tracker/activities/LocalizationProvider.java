package lucns.tracker.activities;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

public class LocalizationProvider {

    public interface Callback {
        void onAvailable(double latitude, double longitude, double accuracy, double bearing);
    }

    private boolean isRunning;
    private final Callback callback;
    private final FusedLocationProviderClient fusedLocationClient;
    private final Compass compass;
    private float azimuth;

    public LocalizationProvider(Context context, Callback callback) {
        this.callback = callback;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        compass = new Compass(context, new Compass.Callback() {
            @Override
            public void onNewAzimuth(float azimuth) {
                LocalizationProvider.this.azimuth = azimuth;
            }
        });
    }

    public float getAzimuth() {
        return azimuth;
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                Location location = locationList.get(locationList.size() - 1);
                if (!isRunning) return;
                callback.onAvailable(location.getLatitude(), location.getLongitude(), resizeNumber(location.getAccuracy()), location.getBearing());
            }
        }

        private double resizeNumber(double d) {
            int i = (int) (d * 100.0d);
            return ((double) i) / 100.0d;
        }
    };

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(2500);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        compass.start();
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        compass.stop();
    }
}
