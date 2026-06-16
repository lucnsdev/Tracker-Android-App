package lucns.tracker.activities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;

import lucns.tracker.R;
import lucns.tracker.notifications.NotificationProvider;
import lucns.tracker.services.MainService;
import lucns.tracker.services.ServiceController;

public class IncomingCallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            keyguardManager.requestDismissKeyguard(this, null);
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceController.getInstance(IncomingCallActivity.this, new ServiceController.OnServiceAvailableListener() {
                    @Override
                    public void onAvailable(MainService mainService) {
                        mainService.stopForeground();
                    }
                });
                //startActivity(new Intent(IncomingCallActivity.this, MapActivity.class));
                finish();
            }
        });
    }
}
