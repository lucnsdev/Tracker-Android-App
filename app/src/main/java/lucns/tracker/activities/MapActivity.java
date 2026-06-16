package lucns.tracker.activities;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;

import lucns.tracker.R;
import lucns.tracker.services.MainService;
import lucns.tracker.services.ServiceController;
import lucns.tracker.utils.Annotator;
import lucns.tracker.utils.Notify;
import lucns.tracker.utils.TimerCounter;
import lucns.tracker.utils.Utils;

public class MapActivity extends Activity {

    private MainService mainService;
    private GoogleMap googleMap;
    private MapView mapView;
    private boolean followMyMarker;
    private PopupMenu popupMenu;
    private Bitmap positionIconWhite, positionIconBlack, positionIconGreen, positionIconRed;
    private LocalizationProvider localizationProvider;
    private boolean zoomed;
    private Marker mainMarker;
    private LocationData myLocation;
    private Information information;
    private boolean isBlackMarker;
    private Dialog dialog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = new Runnable() {

        private void change(boolean powerOn) {
            if (mainMarker != null) {
                long time = 0;
                if (information.hasLocation()) {
                    if (powerOn) {
                        time = (information.getPowerTime() + information.getActiveTime() + (timerActiveTime.getCurrentTime() / 1000)) - information.getLocationData().capturedAt;
                    } else {
                        time = (Instant.now().getEpochSecond() - information.getLocationData().capturedAt);
                    }
                }
                if (time > 0) {
                    String text = information.getPassedTime(time);
                    if (isBlackMarker) {
                        if (powerOn) {
                            if (information.getLocationData().isOlder) {
                                mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(generateBitmap(positionIconRed, text)));
                            } else {
                                mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(generateBitmap(positionIconGreen, text)));
                            }
                        } else {
                            mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(generateBitmap(positionIconWhite, text)));
                        }
                    } else {
                        mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(generateBitmap(positionIconBlack, text)));
                    }
                } else {
                    if (isBlackMarker) {
                        if (powerOn) {
                            if (information.getLocationData().isOlder) {
                                mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(positionIconRed));
                            } else {
                                mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(positionIconGreen));
                            }
                        } else {
                            mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(positionIconWhite));
                        }
                    } else {
                        mainMarker.setIcon(BitmapDescriptorFactory.fromBitmap(positionIconBlack));
                    }
                }
                isBlackMarker = !isBlackMarker;
            }
        }

        @Override
        public void run() {
            if (information != null) {
                switch (information.status) {
                    case Information.POWER_UP:
                        change(true);
                        break;
                    case Information.EMPTY_DATA:
                    case Information.INTERNAL_READ_FAILURE:
                        break;
                    default:
                        change(false);
                        break;
                }
            } else {
                change(true);
            }
            handler.postDelayed(this, 250);
        }
    };

    private TextView textStatus, textAccuracy, textPowerAt, textPowerAtValue, textActiveTime, textVelocity, textSatellitesHdop;
    private TimerCounter timerCapturedAt, timerActiveTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        /*
        Rect rectangle = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight= contentViewTop - statusBarHeight;
         */

        int size = 56;
        int size2 = 36;
        positionIconWhite = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.my_location_icon_white), size, size, false);
        positionIconBlack = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.my_location_icon_black), size, size, false);
        positionIconGreen = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.my_location_icon_green), size, size, false);
        positionIconRed = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.my_location_icon_red), size, size, false);
        //positionPointCircle = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.map_point_circle), size2, size2, false);

        /*
        // cNRjvw9fSLG0VgkKtKwo8a:APA91bEqOoiadB32B0LtSXV8Z8SaDKosr6NTRVTA1ph3guNono2RnKRmQOMaUjeSJbJg0WBF3ZTfoMrIF_5Ct8wFycbMD92159ZidzZ-t1Nz5XkQ1gz4VxE
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        String token = task.getResult();
                        Log.d("lucas", "token:" + token);
                        AppPreferences.setString("token", token);
                    }
                });
         */

        textStatus = findViewById(R.id.textStatusValue);
        textAccuracy = findViewById(R.id.textAccuracyValue);
        textPowerAt = findViewById(R.id.textPowerAt);
        textPowerAtValue = findViewById(R.id.textPowerAtValue);
        textActiveTime = findViewById(R.id.textActiveTimeValue);
        textVelocity = findViewById(R.id.textVelocityValue);
        textSatellitesHdop = findViewById(R.id.textSatellitesHdopValue);

        timerCapturedAt = new TimerCounter(new TimerCounter.Callback() {
            @Override
            public void onTimeChanged(long milliseconds) {
                if (information != null && information.getLocationData().capturedAt > 0) {
                    //textCapturedAt.setText(information.getPassedTime((information.getPowerTime() + information.getActiveTime() + (milliseconds / 1000)) - information.getLocationData().capturedAt));
                }
            }

            @Override
            public void onGate() {

            }
        });

        timerActiveTime = new TimerCounter(new TimerCounter.Callback() {
            @Override
            public void onTimeChanged(long milliseconds) {
                if (information != null && information.getActiveTime() > 0) {
                    textActiveTime.setText(information.getPassedTime(information.getActiveTime() + (milliseconds / 1000)));
                }
            }

            @Override
            public void onGate() {
            }
        });

        ImageButton fab = findViewById(R.id.fab);
        fab.setImageResource(followMyMarker ? R.drawable.location_enabled : R.drawable.location_disabled);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                followMyMarker = !followMyMarker;
                fab.setImageResource(followMyMarker ? R.drawable.location_enabled : R.drawable.location_disabled);
                if (followMyMarker) {
                    if (information != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(information.getLocationData().latitude, information.getLocationData().longitude), 18f));
                    } else if (myLocation != null) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(myLocation.latitude, myLocation.longitude), 18f));
                    }
                } else {
                    //googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(googleMap.getCameraPosition().target, 14f));
                }
            }
        });
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                fab.setEnabled(true);
                MapActivity.this.googleMap = googleMap;
                LatLng ll = new LatLng(-5.0d, -39.5d);
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
                //googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 3f));
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        if (mainMarker != null)
                            mainMarker.setRotation((float) information.getLocationData().azimuth - googleMap.getCameraPosition().bearing);
                    }
                });
                updateMarker();
            }
        });

        localizationProvider = new LocalizationProvider(this, new LocalizationProvider.Callback() {
            @Override
            public void onAvailable(double latitude, double longitude, double accuracy, double bearing) {
                myLocation = new LocationData(latitude, longitude);
                if (accuracy < 20) {
                    if (!zoomed) {
                        zoomed = true;
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15f));
                    }
                }
            }
        });

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonMenu) {
                    popupMenu.getMenu().getItem(1).setTitle(mainService.isMonitoring() ? R.string.disable_monitor : R.string.enable_monitor);
                    popupMenu.show();
                }
            }
        };
        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setVisibility(View.VISIBLE);
        buttonMenu.setOnClickListener(onClickListener);
        popupMenu = new PopupMenu(MapActivity.this, buttonMenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_send_test) {
                    Log.d("lucas", "Sending a message...");
                    JSONObject jsonLocation = new JSONObject();
                    try {
                        jsonLocation.put("satellites", 0);
                        jsonLocation.put("gnss_fix", 0);
                        jsonLocation.put("azimuth", 0);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("status", "power_down");
                        jsonObject.put("active_time", 10);
                        jsonObject.put("location", jsonLocation);
                        mainService.send(jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (itemId == R.id.menu_change_monitor) {
                    if (mainService.isMonitoring()) {
                        mainService.setMonitorMode(false);
                        Notify.showToast(R.string.monitor_disabled);
                    } else {
                        mainService.setMonitorMode(true);
                        Notify.showToast(R.string.monitor_enabled);
                        finish();
                    }
                } else if (itemId == R.id.menu_ble_options) {
                    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                    if (bluetoothAdapter.isEnabled()) {
                        startActivity(new Intent(MapActivity.this, BleOptionsActivity.class));
                        finish();
                    } else {
                        try {
                            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1234);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
        });
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popupMenu.getMenu());

        ServiceController.getInstance(this, new ServiceController.OnServiceAvailableListener() {
            @Override
            public void onAvailable(MainService mainService) {
                MapActivity.this.mainService = mainService;
                mainService.stopForeground();
                mainService.setCallback(new MainService.Callback() {
                    @Override
                    public void onReceive(String data) {
                        try {
                            retrieveData(data);
                            saveLastInformation();
                            updateUi();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConnected() {
                        Notify.showToast(R.string.connected);
                    }

                    @Override
                    public void onSubscribed() {
                        Notify.showToast(R.string.subscribed);
                    }
                });
                if (Utils.hasInternetConnection()) mainService.connect();
                else Notify.showToast(R.string.error_no_internet_connection);
            }
        });
        loadLastInformation();
        if (information != null) {
            LocationData location = information.getLocationData();
            if (location != null) location.isOlder = true;
        }

        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackPressedListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (resultCode == RESULT_OK) {
                if (isFinishing()) return;
                startActivity(new Intent(MapActivity.this, BleOptionsActivity.class));
                finish();
                return;
            }
            Notify.showToast(R.string.canceled);
        }
    }

    public void updateUi() {
        if (information.status == Information.POWER_UP) {
            textPowerAt.setText(R.string.power_on_at);
            textPowerAtValue.setText(information.getWhenTime(information.getPowerOnAt()));
        } else {
            textPowerAt.setText(R.string.power_off_at);
            if (information.getPowerOffAt() > 0) textPowerAtValue.setText(information.getWhenTime(information.getPowerOffAt()));
        }
        if (information.hasLocation()) {
            boolean fix = information.getLocationData().gnssFix;
            textAccuracy.setText(fix ? R.string.fix : R.string.no_fix);
            textAccuracy.setTextColor(getColor(fix ? R.color.green : R.color.red));
        } else {
            textAccuracy.setText(R.string.no_fix);
            textAccuracy.setTextColor(getColor(R.color.red));
        }
        textSatellitesHdop.setText(String.valueOf(information.getLocationData().satellites));
        textActiveTime.setText(information.getPassedTime(information.getActiveTime()));
        textVelocity.setText(information.velocity + "km/h");
        switch (information.status) {
            case Information.EMPTY_DATA:
                textStatus.setTextColor(getColor(R.color.red));
                textStatus.setText(R.string.status_empty_data);
                break;
            case Information.INTERNAL_READ_FAILURE:
                textStatus.setTextColor(getColor(R.color.red));
                textStatus.setText(R.string.status_internal_read_failure);
                break;
            case Information.POWER_UP:
                timerActiveTime.start();
                textStatus.setTextColor(getColor(R.color.green));
                textStatus.setText(R.string.status_power_up);
                break;
            case Information.POWER_DOWN:
                textStatus.setTextColor(getColor(R.color.gray_8));
                textStatus.setText(R.string.status_power_down);
                break;
            case Information.POWER_DOWN_HOME:
                textStatus.setTextColor(getColor(R.color.gray_8));
                textStatus.setText(R.string.status_power_down_home);
                break;
            case Information.POWER_DOWN_WAIT_ACCURACY:
                textStatus.setTextColor(getColor(R.color.main));
                textStatus.setText(R.string.status_power_down_waiting_accuracy);
                break;
            case Information.POWER_DOWN_GPS_NO_ACCURACY:
                textStatus.setTextColor(getColor(R.color.orange));
                textStatus.setText(R.string.status_power_down_no_accuracy);
                break;
            case Information.POWER_DOWN_GPS_NO_CONNECTION:
                textStatus.setTextColor(getColor(R.color.red));
                textStatus.setText(R.string.status_power_down_no_connection);
                break;
        }
        updateMarker();
    }

    public void retrieveData(String data) throws JSONException {
        Utils.vibrate(30);
        JSONObject jsonObject = new JSONObject(data);
        if (jsonObject.has("location")) {
            JSONObject jsonLocation = jsonObject.getJSONObject("location");
            LocationData locationData;
            if (jsonLocation.has("latitude") && jsonLocation.has("longitude")) {
                locationData = new LocationData(jsonLocation.getBoolean("gnss_fix"), jsonLocation.getDouble("latitude"), jsonLocation.getDouble("longitude"), jsonLocation.optInt("accuracy", 0), jsonLocation.getInt("azimuth"), jsonLocation.getInt("satellites"), jsonLocation.getLong("captured_at"));
            } else {
                if (information == null) {
                    locationData = new LocationData(jsonLocation.getInt("satellites"));
                } else {
                    locationData = LocationData.from(information.getLocationData());
                    locationData.satellites = jsonLocation.getInt("satellites");
                    locationData.gnssFix = jsonLocation.getBoolean("gnss_fix");
                }
            }
            if (information != null) {
                int azimuth = calculateDegrees(locationData, information.getLocationData());
                information = new Information(locationData, getKmh(locationData, information.getLocationData()));
                if (information.status == Information.POWER_UP) information.getLocationData().azimuth = azimuth;
            } else {
                information = new Information(locationData);
            }
        } else if (information == null) {
            information = new Information(jsonObject.getString("status"));
        } else {
            information.setStatus(jsonObject.getString("status"));
        }
        information.setActiveTime(jsonObject.getLong("active_time"));

        Object object = jsonObject.get("status");
        if (object instanceof Integer) information.status = (int) object;
        else information.setStatus((String) object);
        if (!checkIsValidData()) return;

        if (information.status == Information.POWER_UP) {
            information.setPowerOnAt(jsonObject.getLong("power_on_at"));
            timerCapturedAt.stop();
            if (information.status == Information.POWER_UP) timerCapturedAt.start();
        } else if (jsonObject.has("power_off_at")) {
            information.setPowerOffAt(jsonObject.getLong("power_off_at"));
        }
        timerActiveTime.stop();
    }

    private void saveLastInformation() {
        Annotator annotator = new Annotator("LastInformation.json");
        LocationData location = information.getLocationData();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", information.status);
            jsonObject.put("velocity", information.velocity);
            jsonObject.put("active_time", information.getActiveTime());
            if (information.status == Information.POWER_UP) jsonObject.put("power_on_at", information.getPowerOnAt());
            else jsonObject.put("power_off_at", information.getPowerOffAt());
            if (location != null) {
                JSONObject jsonLocation = new JSONObject();
                if (location.latitude != 0) jsonLocation.put("latitude", location.latitude);
                if (location.longitude != 0) jsonLocation.put("longitude", location.longitude);
                if (location.accuracy != 0) jsonLocation.put("accuracy", location.accuracy);
                jsonLocation.put("gnss_fix", location.gnssFix);
                jsonLocation.put("azimuth", location.azimuth);
                jsonLocation.put("satellites", location.satellites);
                jsonLocation.put("captured_at", location.capturedAt);
                jsonObject.put("location", jsonLocation);
            }
            annotator.setContent(jsonObject.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadLastInformation() {
        Annotator annotator = new Annotator("LastInformation.json");
        if (!annotator.exists()) return;
        try {
            retrieveData(annotator.getContent());
            updateUi();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private final OnBackInvokedCallback onBackPressedListener = new OnBackInvokedCallback() {
        @Override
        public void onBackInvoked() {
            finish();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        popupMenu.dismiss();
        localizationProvider.stop();
        mapView.onPause();
        handler.removeCallbacks(runnable);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mainService != null) {
            if (mainService.isMonitoring()) {
                mainService.startForeground();
            } else {
                mainService.disconnect();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        localizationProvider.start();
        handler.postDelayed(runnable, 500);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mainService != null) {
            mainService.stopForeground();
            if (Utils.hasInternetConnection()) mainService.connect();
            else Notify.showToast(R.string.error_no_internet_connection);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerActiveTime.stop();
        timerCapturedAt.stop();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackPressedListener);
        if (mapView != null) mapView.onDestroy();
    }

    private void showDialogInfo(String title) {
        dialog = new Dialog(MapActivity.this, R.style.DialogTheme);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_info);
        TextView textTitle = dialog.findViewById(R.id.textTitle);
        textTitle.setText(title);
        dialog.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private boolean checkIsValidData() {
        if (information.status == Information.EMPTY_DATA || information.status == Information.INTERNAL_READ_FAILURE) {
            if (isDestroyed()) return false;
            showDialogInfo(getString(information.status == Information.EMPTY_DATA ? R.string.status_empty_data : R.string.status_internal_read_failure));
        }
        return true;
    }

    private void updateMarker() {
        if (googleMap == null) return;
        Utils.vibrate();
        if (information == null || information.getLocationData() == null) {
            return;
        }
        LocationData locationData = information.getLocationData();
        long time = (information.getPowerTime() + information.getActiveTime()) - locationData.capturedAt;
        if (mainMarker != null) mainMarker.remove();
        MarkerOptions options = new MarkerOptions();
        if (time > 0) {
            options.icon(BitmapDescriptorFactory.fromBitmap(generateBitmap(positionIconBlack, information.getPassedTime((information.getPowerTime() + information.getActiveTime()) - locationData.capturedAt))));
        } else {
            options.icon(BitmapDescriptorFactory.fromBitmap(positionIconBlack));
        }
        LatLng ll = new LatLng(locationData.latitude, locationData.longitude);
        options.position(ll);
        options.rotation((float) locationData.azimuth - googleMap.getCameraPosition().bearing);
        mainMarker = googleMap.addMarker(options);
        if (followMyMarker || !zoomed) {
            zoomed = true;
            cameraConfigPosition();
        }
    }

    private Bitmap generateBitmap(Bitmap markerBitmap, String text) {
        View markerLayout = getLayoutInflater().inflate(R.layout.map_marker, null);
        ImageView markerImage = markerLayout.findViewById(R.id.marker_image);
        TextView markerRating = markerLayout.findViewById(R.id.marker_text);
        markerImage.setImageBitmap(markerBitmap);
        markerRating.setText(text);

        markerLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        final Bitmap bitmap = Bitmap.createBitmap(markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerLayout.draw(canvas);
        return bitmap;
    }

    private void cameraConfigPosition() {
        CameraPosition currentPosition = googleMap.getCameraPosition();
        CameraPosition newPosition = new CameraPosition.Builder(currentPosition)
                .bearing((float) information.getLocationData().azimuth)
                .target(new LatLng(information.getLocationData().latitude, information.getLocationData().longitude))
                .zoom(18f)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition));
    }

    private int calculateDegrees(LocationData c, LocationData c2) {
        double angle = Math.toDegrees(Math.atan2(c2.longitude - c.longitude, c2.latitude - c.latitude));
        return (int) (angle + 360) % 360; // Returns 0-359.9...
    }

    private int getKmh(LocationData i, LocationData i2) {
        if (i2.capturedAt <= i.capturedAt) return 0;
        double distance = calculateDistance(i.latitude, i.longitude, i2.latitude, i2.longitude);
        long time = i2.capturedAt - i.capturedAt;
        long partitions = 3600 / time;
        return (int) (distance * partitions);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c; // EARTH_RADIUS_KM = 6371;
    }
}
