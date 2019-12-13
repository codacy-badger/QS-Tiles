package diorid.tiles.screenshot;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.service.quicksettings.TileService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Screenshot extends Application {
    private static MediaProjectionManager mediaProjectionManager = null;
    private static Screenshot instance;
    private static Intent screenshotPermission = null;
    private static onGrantedScreenshotPermission onGrantedScreenshotPermission = null;
    private static MediaProjection mediaProjection = null;
    private static volatile boolean receiverRegistered = false;

    public static Screenshot getInstance() {
        return instance;
    }

    public static void setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        Screenshot.mediaProjectionManager = mediaProjectionManager;
    }

    protected static void registerNotificationReceiver() {
        if (receiverRegistered) {
            return;
        }

        NotificationActionReceiver notificationActionReceiver = new NotificationActionReceiver();
        notificationActionReceiver.registerReceiver(Screenshot.getInstance());

        receiverRegistered = true;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected static MediaProjection createMediaProjection() {
        if (mediaProjection == null) {
            if (screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
                screenshotPermission = ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
            }
            if (screenshotPermission == null) {
                return null;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
        }
        return mediaProjection;
    }

    protected static void acquireScreenshotPermission(Context context, onGrantedScreenshotPermission myOnGrantedScreenshotPermission) {
        onGrantedScreenshotPermission = myOnGrantedScreenshotPermission;
        ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();

        if (screenshotPermission == null && screenshotTileService != null) {
            screenshotPermission = screenshotTileService.getScreenshotPermission();
        }

        if (screenshotPermission != null) {
            if (null != mediaProjection) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            if (screenshotTileService != null) {
                screenshotTileService.foreground();
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            if (onGrantedScreenshotPermission != null) {
                onGrantedScreenshotPermission.onAcquireScreenshotPermission();
            }

        } else {
            openScreenshotPermissionRequester(context);
        }
    }

    private static void openScreenshotPermissionRequester(Context context) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION_SCREENSHOT, true);
        context.startActivity(intent);
    }

    protected static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
        if (ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService.Companion.getInstance().setScreenshotPermission(screenshotPermission);
            if (onGrantedScreenshotPermission != null) {
                onGrantedScreenshotPermission.onAcquireScreenshotPermission();
                onGrantedScreenshotPermission = null;
            }
        }
    }

    public static void requestStoragePermission(Context context) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION_STORAGE, true);
        context.startActivity(intent);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    public void screenshot(Context context) {
        screenshotHiddenCountdown(context);
    }

    public void screenshotPartial(Context context) {
        Intent intent = NonDisplayActivity.newPartialIntent(context);
        if (!(context instanceof Activity)) {
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private void screenshotHiddenCountdown(Context context) {
        if (context instanceof ScreenshotTileService || ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService tileService = (context instanceof ScreenshotTileService) ? (ScreenshotTileService) context : ScreenshotTileService.Companion.getInstance();
            tileService.setTakeScreenshotOnStopListening(true);
            Intent intent = NonDisplayActivity.newIntent(context, false);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            tileService.startActivityAndCollapse(intent);
        } else {
            Intent intent = NonDisplayActivity.newIntent(context, true);
            if (!(context instanceof Activity)) {
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        }
    }

    protected void takeScreenshotFromTileService(TileService context) {
        Intent intent = NonDisplayActivity.newIntent(context, true);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}
