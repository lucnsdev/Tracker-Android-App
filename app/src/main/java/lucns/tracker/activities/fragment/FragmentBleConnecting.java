package lucns.tracker.activities.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import lucns.tracker.R;
import lucns.tracker.activities.MapActivity;
import lucns.tracker.services.BleController;
import lucns.tracker.utils.Notify;
import lucns.tracker.utils.Utils;
import lucns.tracker.views.FragmentView;

public class FragmentBleConnecting extends FragmentView {

    public interface OnCharacteristicsTxAvailableListener {
        void onCharacteristicsAvailable(BluetoothGatt gatt, BluetoothGattCharacteristic tx);
    }


    private final String SERVICE_UUID = "6ffcebec-3d20-11ed-b878-0242ac120002";

    private final String CHARACTERISTIC_TX_UUID = "6ffcebec-3d20-11ed-b878-0242ac120004";
    private final String CHARACTERISTIC_RX_UUID = "6ffcebec-3d20-11ed-b878-0242ac120003";

    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;
    private TextView textTitle;
    private OnCharacteristicsTxAvailableListener listener;
    private boolean connected, disconnectedByUser;

    public FragmentBleConnecting(Activity activity, OnCharacteristicsTxAvailableListener listener) {
        super(activity);
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_connecting);

        textTitle = findViewById(R.id.textTitle);
    }

    private void closeActivity(int resString) {
        Utils.vibrate();
        Notify.showToast(resString);
        startActivity(new Intent(getActivity(), MapActivity.class));
        finish();
    }

    public void disconnect() {
        disconnectedByUser = true;
        if (!connected) return;
        try {
            bluetoothGatt.disconnect();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void connect(BluetoothDevice device) {
        this.bluetoothDevice = device;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothGatt = device.connectGatt(getActivity(), false, bluetoothGattCallback);
                    return;
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!gatt.getDevice().getAddress().equals(bluetoothDevice.getAddress())) return;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    connected = true;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            textTitle.setText(R.string.discovering_services);
                        }
                    });
                    try {
                        bluetoothGatt.discoverServices();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    connected = false;
                    if (disconnectedByUser) break;
                    closeActivity(R.string.disconnected);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService bluetoothGattService = gatt.getService(UUID.fromString(SERVICE_UUID));
                BluetoothGattCharacteristic characteristicTx = bluetoothGattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_TX_UUID));
                BluetoothGattCharacteristic characteristicRx = bluetoothGattService.getCharacteristic(UUID.fromString(CHARACTERISTIC_RX_UUID));
                try {
                    gatt.setCharacteristicNotification(characteristicRx, true);
                    List<BluetoothGattDescriptor> list = characteristicRx.getDescriptors();
                    int properties = characteristicRx.getProperties();
                    for (BluetoothGattDescriptor descriptor : list) {
                        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        }
                    }

                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCharacteristicsAvailable(gatt, characteristicTx);
                    }
                });
            } else {
                closeActivity(R.string.discovering_services_failure);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            Log.d("lucas", "Received: " + new String(value));
            Notify.showToast("Received: " + new String(value));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("lucas", "onCharacteristicWrite");
        }
    };

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
