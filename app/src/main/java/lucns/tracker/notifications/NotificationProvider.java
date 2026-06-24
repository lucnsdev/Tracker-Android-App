package lucns.tracker.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.widget.RemoteViews;

import lucns.tracker.R;
import lucns.tracker.activities.IncomingCallActivity;
import lucns.tracker.activities.MapActivity;

public class NotificationProvider {

    public static final int NOTIFICATION_CODE = 1234;
    private final String silent = "Silent";
    private final String alert = "Alert";
    public static final String ACTION_BUTTON_CLICK = "button_click";
    public static final String ACTION_DELETE = "delete";
    private final Context context;
    private final NotificationManager notificationManager;
    private Notification notification;
    private boolean isShowing;
    private Class<?> activityClass;

    public NotificationProvider(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        NotificationChannel builderChannel = new NotificationChannel(silent, context.getString(R.string.notification_silent), NotificationManager.IMPORTANCE_DEFAULT);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.enableLights(false);
        builderChannel.enableVibration(false);
        builderChannel.setSound(null, null);
        notificationManager.createNotificationChannel(builderChannel);
        /*
        AudioAttributes.Builder audioAttributes = new AudioAttributes.Builder();
        audioAttributes.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
        audioAttributes.setLegacyStreamType(AudioManager.STREAM_RING);
        audioAttributes.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
         */
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build();
        builderChannel = new NotificationChannel(alert, context.getString(R.string.notification_alert), NotificationManager.IMPORTANCE_HIGH);
        builderChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);
        builderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        builderChannel.setLightColor(Color.RED);
        builderChannel.enableLights(true);
        builderChannel.setBypassDnd(true);
        builderChannel.enableVibration(true);
        builderChannel.setVibrationPattern(new long[]{250, 250, 250, 250});
        notificationManager.createNotificationChannel(builderChannel);
    }

    public int getNotificationCode() {
        return NOTIFICATION_CODE;
    }

    public Notification getNotification() {
        return notification;
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void setActivityClass(Class<?> activityToOpen) {
        activityClass = activityToOpen;
    }

    public void showAlert(String title) {
        isShowing = true;
        Intent intent = new Intent(context, MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentFullScreen = new Intent(context, IncomingCallActivity.class);
        PendingIntent pendingIntentFullScreen = PendingIntent.getActivity(context, 0, intentFullScreen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent intentButton = new Intent(context, IncomingCallReceiver.class);
        intentButton.setAction(ACTION_BUTTON_CLICK);
        PendingIntent pendingIntentButton = PendingIntent.getBroadcast(context, 1, intentButton, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews collapsedView = new RemoteViews(context.getPackageName(), R.layout.notification_call_collapsed);
        RemoteViews expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_call_expanded);
        expandedView.setOnClickPendingIntent(R.id.buttonIgnore, pendingIntentButton);
        expandedView.setOnClickPendingIntent(R.id.buttonOpenApp, pendingIntent);

        Notification.Builder builder = new Notification.Builder(context, alert);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setColorized(true);
        builder.setColor(context.getColor(R.color.item_red));
        builder.setContentIntent(pendingIntent);
        builder.setFullScreenIntent(pendingIntentFullScreen, true);
        builder.setSmallIcon(Icon.createWithResource(context, R.drawable.icon_dangerous));
        builder.setStyle(new Notification.DecoratedCustomViewStyle());
        builder.setCustomContentView(collapsedView);
        builder.setCustomHeadsUpContentView(collapsedView);
        //builder.setCustomBigContentView(expandedView);
        notificationManager.notify(NOTIFICATION_CODE, builder.build());
    }

    public void showWithAction(int title, int action) {
        show(context.getString(title), null, null, context.getString(action));
    }

    public void show(int title, int text, int sub) {
        show(context.getString(title), context.getString(text), context.getString(sub), null);
    }

    public void show(int title, int text, int sub, int action) {
        show(context.getString(title), context.getString(text), context.getString(sub), context.getString(action));
    }

    public void show(String title, String text, String sub, String action) { // action button only works if app is running
        isShowing = true;
        PendingIntent pendingIntent = null;
        if (activityClass != null) {
            Intent resultIntent = new Intent(context, activityClass);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        Notification.Builder builder = new Notification.Builder(context, silent);
        builder.setAutoCancel(true);
        builder.setOngoing(false);
        builder.setShowWhen(true);
        builder.setColorized(false);
        //builder.setColor(context.getColor(R.color.accent));
        builder.setTicker(title);
        builder.setContentTitle(title);
        if (text != null) builder.setContentText(text);
        if (sub != null) builder.setSubText(sub);
        if (pendingIntent != null) builder.setContentIntent(pendingIntent);
        if (action != null) {
            Intent intentButton = new Intent(context, IncomingCallReceiver.class);
            intentButton.setAction(ACTION_BUTTON_CLICK);
            PendingIntent pendingIntentButton = PendingIntent.getBroadcast(context, 1, intentButton, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(new Notification.Action.Builder(Icon.createWithResource(context, R.drawable.icon_close_18), action, pendingIntentButton).build());
        }
        builder.setSmallIcon(Icon.createWithResource(context, R.drawable.icon_notification_18));
        builder.setCategory(Notification.CATEGORY_SERVICE);
        builder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, new Intent(ACTION_DELETE), PendingIntent.FLAG_IMMUTABLE));
        notification = builder.build();
        notificationManager.notify(NOTIFICATION_CODE, notification);
    }

    public void hide() {
        isShowing = false;
        notificationManager.cancelAll();
    }
}
