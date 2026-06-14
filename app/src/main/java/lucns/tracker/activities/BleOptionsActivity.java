package lucns.tracker.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import lucns.tracker.R;
import lucns.tracker.activities.fragment.FragmentBleConnecting;
import lucns.tracker.activities.fragment.FragmentBleScan;
import lucns.tracker.activities.fragment.FragmentBleSendData;
import lucns.tracker.views.SliderView;

public class BleOptionsActivity extends Activity {

    private SliderView sliderView;
    private FragmentBleConnecting fragmentBleConnecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_options);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        FragmentBleSendData fragmentBleSendData = new FragmentBleSendData(this);
        fragmentBleConnecting = new FragmentBleConnecting(this, new FragmentBleConnecting.OnCharacteristicsTxAvailableListener() {
            @Override
            public void onCharacteristicsAvailable(BluetoothGatt gatt, BluetoothGattCharacteristic tx) {
                sliderView.goToIndex(2);
                fragmentBleSendData.setGattAneCharacteristics(gatt, tx);
            }
        });
        FragmentBleScan fragmentBleScan = new FragmentBleScan(this, new FragmentBleScan.OnDeviceSelectedListener() {
            @Override
            public void onDeviceSelected(BluetoothDevice device) {
                sliderView.goToIndex(1);
                fragmentBleConnecting.connect(device);
            }
        });

        sliderView = findViewById(R.id.sliderView);
        sliderView.disableScroll(true);
        sliderView.addFragment(fragmentBleScan);
        sliderView.addFragment(fragmentBleConnecting);
        sliderView.addFragment(fragmentBleSendData);

        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackPressedListener);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    private final OnBackInvokedCallback onBackPressedListener = new OnBackInvokedCallback() {
        @Override
        public void onBackInvoked() {
            if (isFinishing()) return;
            if (!sliderView.onBackPressed()) return;
            startActivity(new Intent(BleOptionsActivity.this, MapActivity.class));
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackPressedListener);
        fragmentBleConnecting.disconnect();
        unregisterReceiver(bluetoothStateReceiver);
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        finish();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    };
}
