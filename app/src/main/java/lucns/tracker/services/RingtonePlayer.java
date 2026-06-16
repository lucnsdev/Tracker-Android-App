package lucns.tracker.services;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.IOException;

public class RingtonePlayer {

    private Context context;

    public RingtonePlayer(Context context) {
        this.context = context;
        this.mediaPlayer = new MediaPlayer();
    }

    private MediaPlayer mediaPlayer;

    public void start() {
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        try {
            mediaPlayer.setDataSource(context, ringtoneUri);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRingtone() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }
}
