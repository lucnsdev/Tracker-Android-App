package lucns.tracker.services;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import lucns.tracker.R;
import lucns.tracker.activities.MapActivity;
import lucns.tracker.mqtt.MqttClient;
import lucns.tracker.notifications.NotificationProvider;
import lucns.tracker.remote.FloatAlert;
import lucns.tracker.utils.Utils;

public class MainService extends BaseService {

    public interface Callback {

        void onReceive(String data);

        void onConnected();

        void onSubscribed();
    }

    private Callback callback;
    private NotificationProvider notification;
    private MqttClient mqtt;
    private final String DEFAULT_TOPIC = "lucns/tracker/data";
    private boolean disconnectByUser;
    private boolean isMonitoring;
    private FloatAlert floatAlert;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setMonitorMode(boolean monitoring) {
        isMonitoring = monitoring;
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        floatAlert = new FloatAlert(this, this);
        notification = new NotificationProvider(this);
        notification.setActivityClass(MapActivity.class);

        mqtt = MqttClient.getInstance();
        mqtt.setClientId(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        mqtt.setCallback(new MqttClient.Callback() {
            @Override
            public void onBrokerConnectionChanged(boolean isConnected) {
                Log.d("Lucas", "onBrokerConnectionChanged " + isConnected);
                if (isConnected) mqtt.subscribe(DEFAULT_TOPIC);
                else if (Utils.hasInternetConnection() && !disconnectByUser) connect();
            }

            @Override
            public void onSubscribeChanged(boolean isSubscribed) {
                Log.d("Lucas", "onSubscribeChanged " + isSubscribed);
                if (isForeground()) {
                } else {
                    callback.onSubscribed();
                }
            }

            @Override
            public void onPublicationArrived() {
            }

            @Override
            public void onReceive(String topic, String publication) {
                Log.i("Lucas", "Received: " + publication);
                if (isForeground()) {
                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    if (keyguardManager.isKeyguardLocked()) {
                        Log.d("lucas", "isKeyguardLocked");
                        notification.showAlert(getString(R.string.possible_violation));
                    } else {
                        Log.d("lucas", "startActivity");
                        Intent intent = new Intent(MainService.this, MapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    }
                } else {
                    callback.onReceive(publication);
                }
            }

            @Override
            public void onPingCompleted() {
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStartCommand(Intent intent) {
        String action = intent == null || intent.getAction() == null ? "" : intent.getAction();
    }

    @Override
    public NotificationProvider onForegroundRequested() {
        if (!notification.isShowing()) {
            notification.showWithAction(R.string.monitor_enabled, R.string.monitor_close);
        }
        return notification;
    }

    @Override
    public void onForegroundStarted() {

    }

    @Override
    public void onForegroundStopped() {

    }

    public void startForeground() {
        startForeground(MainService.class);
    }

    public void stopForeground() {
        disconnectByUser = true;
        mqtt.disconnect();
        super.stopForeground();
    }

    public void send(String message) {
        mqtt.publish(DEFAULT_TOPIC, message);
    }

    public void connect() {
        disconnectByUser = false;
        if (mqtt.isConnected()) return;
        mqtt.connect("broker.emqx.io:1883");
    }

    public void disconnect() {
        disconnectByUser = true;
        mqtt.disconnect();
    }
}
