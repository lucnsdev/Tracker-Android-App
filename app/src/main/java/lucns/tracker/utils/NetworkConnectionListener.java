package lucns.tracker.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

public class NetworkConnectionListener {

    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final ConnectionCallback connectionCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectionCallback {
        void onAvailable();
        void onLost();
    }

    public NetworkConnectionListener(Context context, ConnectionCallback callback) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.connectionCallback = callback;
    }

    public void startListening() {
        if (networkCallback != null) return;
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                mainHandler.post(connectionCallback::onAvailable);
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                mainHandler.post(connectionCallback::onLost);
            }
        };
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void stopListening() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }
}

