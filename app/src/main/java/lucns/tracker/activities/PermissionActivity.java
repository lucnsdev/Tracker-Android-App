package lucns.tracker.activities;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

import lucns.tracker.R;

public class PermissionActivity extends Activity {

    private String[] PERMISSIONS_RUNTIME = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.USE_FULL_SCREEN_INTENT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions();
    }

    private void requestPermissions() {
        String[] deniedPermissions = getDeniedPermissions();
        if (deniedPermissions.length > 0) {
            requestPermissions(deniedPermissions, 1234);
            return;
        } else if (!Settings.canDrawOverlays(this)) {
            Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            myIntent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(myIntent, 1234);
            return;
        } else if (!((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).canUseFullScreenIntent()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
            return;
        }

        startActivity(new Intent(this, MapActivity.class));
        finish();
    }

    private String[] getDeniedPermissions() {
        List<String> permissions = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        String packageName = getPackageName();
        for (String permission : PERMISSIONS_RUNTIME) {
            if (packageManager.checkPermission(permission, packageName) != PackageManager.PERMISSION_GRANTED)
                permissions.add(permission);
        }
        return permissions.toArray(new String[permissions.size()]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
    }
}