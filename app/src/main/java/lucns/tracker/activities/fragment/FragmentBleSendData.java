package lucns.tracker.activities.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.nio.charset.StandardCharsets;

import lucns.tracker.R;
import lucns.tracker.activities.LocationData;
import lucns.tracker.activities.LocalizationProvider;
import lucns.tracker.utils.Utils;
import lucns.tracker.views.FragmentView;

public class FragmentBleSendData extends FragmentView {

    public FragmentBleSendData(Activity activity) {
        super(activity);
    }

    private MapView mapView;
    private GoogleMap googleMap;
    private Marker mapMarker;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private LocationData locationData;
    private LocalizationProvider localizationProvider;

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_send_data);

        TextView textView = findViewById(R.id.textTitle);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setEnabled(false);
                String lat = String.valueOf(locationData.latitude);
                String lon = String.valueOf(locationData.longitude);
                if (lat.substring(lat.indexOf(".") + 1).length() > 6) {
                    int indexOf = lat.indexOf(".") + 1;
                    lat = lat.substring(0, indexOf) + lat.substring(indexOf, indexOf + 6);
                }
                if (lon.substring(lon.indexOf(".") + 1).length() > 6) {
                    int indexOf = lon.indexOf(".") + 1;
                    lon = lon.substring(0, indexOf) + lon.substring(indexOf, indexOf + 6);
                }
                String data = lat + "," + lon;
                try {
                    bluetoothGatt.writeCharacteristic(characteristic, data.getBytes(StandardCharsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    textView.setText(R.string.sent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        });

        Bitmap bitmapMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_red), 110, 110, false);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                FragmentBleSendData.this.googleMap = googleMap;
                LatLng ll = new LatLng(-5.0d, -39.5d);
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(ll));
                //googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 3f));
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng ll) {
                        Utils.vibrate();
                        textView.setText(R.string.send_data);
                        button.setEnabled(true);
                        locationData = new LocationData(ll.latitude, ll.longitude);
                        if (mapMarker != null) mapMarker.remove();
                        MarkerOptions options = new MarkerOptions();
                        options.icon((BitmapDescriptorFactory.fromBitmap(bitmapMarker)));
                        options.position(ll);
                        mapMarker = googleMap.addMarker(options);
                    }
                });
            }
        });

        localizationProvider = new LocalizationProvider(getActivity(), new LocalizationProvider.Callback() {

            boolean zoomed;

            @Override
            public void onAvailable(double latitude, double longitude, double accuracy, double bearing) {
                localizationProvider.stop();
                if (!zoomed) {
                    zoomed = true;
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 20f));
                }
            }
        });
    }

    public void setGattAneCharacteristics(BluetoothGatt gatt, BluetoothGattCharacteristic tx) {
        bluetoothGatt = gatt;
        characteristic = tx;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        localizationProvider.start();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        localizationProvider.stop();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
    }
}
