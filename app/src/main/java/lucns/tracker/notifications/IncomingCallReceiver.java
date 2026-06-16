package lucns.tracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import lucns.tracker.services.MainService;
import lucns.tracker.services.ServiceController;

public class IncomingCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case NotificationProvider.ACTION_BUTTON_CLICK:
                ServiceController.getInstance(context, new ServiceController.OnServiceAvailableListener() {
                    @Override
                    public void onAvailable(MainService mainService) {
                        mainService.stopForeground();
                    }
                });
                break;
        }
    }
}
