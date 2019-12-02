package diorid.tiles;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class SoundProfileTileService extends TileService {
    private AudioManager audioManager;
    private NotificationManager notificationManager;

    @Override
    public void onStopListening() {
        super.onStopListening();
        audioManager = null;
        notificationManager = null;
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        initTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (notificationManager == null) {
            initTile();
        } else {
            setTileAction();
        }
    }

    public void grantPermission() {
        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        closeSystemDialogs();
        Toast.makeText(this, "To control sound modes QS-Tiles needs DND permission!", Toast.LENGTH_LONG).show();
        startActivity(intent);
    }

    public void closeSystemDialogs() {
        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(closeIntent);
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    private void initTile() {

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager.isNotificationPolicyAccessGranted()) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                        setMusicOffMode();
                    } else {
                        setTileSound();
                    }
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    setTileVibrate();
                    break;
                case AudioManager.RINGER_MODE_SILENT:
                    setTileSilent();
                    break;
            }
        } else {
            getQsTile().setState(Tile.STATE_INACTIVE);
            getQsTile().updateTile();
        }
    }

    private void setTileAction() {
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            getQsTile().setState(Tile.STATE_INACTIVE);
            getQsTile().updateTile();
            grantPermission();
            return;
        }

        if (getQsTile().getLabel().equals("Sound")) {
            setVibrateMode();
            return;
        }

        if (getQsTile().getLabel().equals("Vibrate")) {
            setSilentMode();
            return;
        }

        if (getQsTile().getLabel().equals("Silent")) {
            setMusicOffMode();
            return;
        }

        if (getQsTile().getLabel().equals("Media Off")) {
            setSoundMode();
        }
    }

    public void setSoundMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.MODE_NORMAL), AudioManager.FLAG_ALLOW_RINGER_MODES);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.MODE_NORMAL), AudioManager.FLAG_PLAY_SOUND);
        setTileSound();
    }

    public void setVibrateMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_VIBRATE);
        setTileVibrate();
    }

    public void setSilentMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        setTileSilent();
    }

    public void setMusicOffMode() {
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        setTileMusicOff();
    }

    private void setTileSilent() {
        getQsTile().setIcon(Icon.createWithResource(this,
                R.drawable.ic_volume_mute));
        getQsTile().setLabel("Silent");
        getQsTile().updateTile();
    }

    private void setTileVibrate() {
        getQsTile().setIcon(Icon.createWithResource(this,
                R.drawable.ic_vibration));
        getQsTile().setLabel("Vibrate");
        getQsTile().updateTile();
    }

    private void setTileSound() {
        getQsTile().setIcon(Icon.createWithResource(this,
                R.drawable.ic_volume_up_white_24dp));
        getQsTile().setLabel("Sound");
        getQsTile().updateTile();
    }

    private void setTileMusicOff() {
        getQsTile().setIcon(Icon.createWithResource(this,
                R.drawable.ic_music_off));
        getQsTile().setLabel("Media Off");
        getQsTile().updateTile();
    }
}