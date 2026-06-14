package lucns.tracker.activities.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import lucns.tracker.R;
import lucns.tracker.utils.Notify;
import lucns.tracker.utils.Utils;
import lucns.tracker.views.FragmentView;

public class FragmentBleScan extends FragmentView {

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothLeScanner;
    private final List<BluetoothDeviceData> scanResults;
    private boolean scanning;
    private final Handler handler;
    private ListView listView;
    private TextView textTitle, textStatus;
    private ProgressBar progressBar;
    private OnDeviceSelectedListener listener;

    public FragmentBleScan(Activity activity, OnDeviceSelectedListener listener) {
        super(activity);
        this.listener = listener;
        handler = new Handler(Looper.getMainLooper());
        scanResults = new LinkedList<>();
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    public void onCreate() {
        setContentView(R.layout.fragment_ble_scan);

        listView = findViewById(R.id.listView);
        textTitle = findViewById(R.id.textTitle);
        textStatus = findViewById(R.id.textStatus);
        progressBar = findViewById(R.id.progressBar);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                stopScan();
                BluetoothDeviceData data = (BluetoothDeviceData) listView.getAdapter().getItem(position);
                listener.onDeviceSelected(data.device);
            }
        });
        findViewById(R.id.buttonRefresh).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.vibrate();
                if (scanning) {
                    Notify.showToast(R.string.already_scanning);
                    return;
                }
                startScan();
            }
        });
    }

    private void updateList() {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        listView.setAdapter(new ArrayAdapter<BluetoothDeviceData>(getActivity(), R.layout.list_item_ble, scanResults.toArray(new BluetoothDeviceData[0])) {

            @Override
            public int getCount() {
                return scanResults.size();
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                BluetoothDeviceData data = scanResults.get(position);
                View view = layoutInflater.inflate(R.layout.list_item_ble, null, false);
                if (position == 0) {
                    if (getCount() == 1) {
                        view.setBackgroundResource(R.drawable.item_background_single);
                    } else {
                        view.setBackgroundResource(R.drawable.item_background_first);
                    }
                } else if (position == getCount() - 1) {
                    view.setBackgroundResource(R.drawable.item_background_last);
                } else {
                    view.setBackgroundResource(R.drawable.item_background_square);
                }
                TextView textTopStart = view.findViewById(R.id.textTopStart);
                TextView textTopEnd = view.findViewById(R.id.textTopEnd);
                TextView textBottomStart = view.findViewById(R.id.textBottomStart);
                try {
                    textTopStart.setText(data.device.getName() == null ? getActivity().getString(R.string.not_specified) : data.device.getName());
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                textBottomStart.setText(data.device.getAddress());
                textTopEnd.setText(String.valueOf(data.rssi));
                return view;
            }
        });
    }

    public void startScan() {
        if (scanning) return;
        textStatus.setVisibility(View.INVISIBLE);
        progressBar.setIndeterminate(true);
        scanning = true;
        scanResults.clear();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        if (bluetoothAdapter.isEnabled()) {
            try {
                bluetoothLeScanner.startScan(null, builder.build(), scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            /*
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, 10000);
            */
        }
    }

    public void stopScan() {
        if (scanning) {
            textStatus.setVisibility(scanResults.isEmpty() ? View.VISIBLE : View.INVISIBLE);
            textTitle.setText(R.string.scanned_devices);
            progressBar.setIndeterminate(false);
            progressBar.setMax(100);
            progressBar.setProgress(100);
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        scanning = false;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                if (result.getDevice().getName() != null) Log.d("lucas", "name: " + result.getDevice().getName());
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            boolean founded = false;
            for (int i = 0; i < scanResults.size(); i++) {
                if (scanResults.get(i).device.getAddress().equals(result.getDevice().getAddress())) {
                    founded = true;
                    try {
                        if (scanResults.get(i).device.getName() == null && result.getDevice().getName() != null) {
                            scanResults.set(i, new BluetoothDeviceData(result.getDevice(), result.getRssi()));
                            break;
                        } else {
                            return;
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
            Utils.vibrate();
            if (!founded) scanResults.add(new BluetoothDeviceData(result.getDevice(), result.getRssi()));
            scanResults.sort((a, b) -> Integer.compare(b.rssi, a.rssi));
            updateList();
        }
    };

    @Override
    public void onResume() {
        updateList();
        startScan();
    }

    @Override
    public void onPause() {
        stopScan();
    }

    @Override
    public void onDestroy() {
        stopScan();
    }

    private static class BluetoothDeviceData {
        public BluetoothDevice device;
        public int rssi;

        protected BluetoothDeviceData(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }
    }
}
