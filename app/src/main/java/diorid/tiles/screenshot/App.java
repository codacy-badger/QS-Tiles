package diorid.tiles.screenshot;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class App extends Application {
    private static MediaProjectionManager mediaProjectionManager = null;
    private static App instance;
    private static Intent screenshotPermission = null;
    private static ScreenshotPermissionListener screenshotPermissionListener = null;
    private static MediaProjection mediaProjection = null;
    private static volatile boolean receiverRegistered = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable screenshotRunnable;

    public static App getInstance() {
        return instance;
    }

    public static void setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        App.mediaProjectionManager = mediaProjectionManager;
    }

    protected static void registerNotificationReceiver() {
        if (receiverRegistered) {
            return;
        }

        NotificationActionReceiver notificationActionReceiver = new NotificationActionReceiver();
        notificationActionReceiver.registerReceiver(App.getInstance());

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

    protected static void acquireScreenshotPermission(Context context, ScreenshotPermissionListener myScreenshotPermissionListener) {
        screenshotPermissionListener = myScreenshotPermissionListener;

        if (screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
            screenshotPermission = ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
        }

        if (screenshotPermission != null) {
            if (null != mediaProjection) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            if (screenshotPermissionListener != null) {
                screenshotPermissionListener.onAcquireScreenshotPermission();
            }

        } else {
            openScreenshotPermissionRequester(context);
        }
    }

    private static void openScreenshotPermissionRequester(Context context) {
        final Intent intent = new Intent(context, CheckScreenshotPermission.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(CheckScreenshotPermission.EXTRA_REQUEST_PERMISSION_SCREENSHOT, true);
        context.startActivity(intent);
    }

    protected static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
        if (ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService.Companion.getInstance().setScreenshotPermission(screenshotPermission);
            if (screenshotPermissionListener != null) {
                screenshotPermissionListener.onAcquireScreenshotPermission();
                screenshotPermissionListener = null;
            }
        }
    }

    public static void requestStoragePermission(Context context) {
        final Intent intent = new Intent(context, CheckScreenshotPermission.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(CheckScreenshotPermission.EXTRA_REQUEST_PERMISSION_STORAGE, true);
        context.startActivity(intent);
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    public void screenshot(Context context) {
        screenshotHiddenCountdown(context, false);
    }

    private void screenshotHiddenCountdown(Context context, Boolean now) {
        int delay = 1;
        if (now) {
            delay = 0;
        }
        if (delay > 0) {
            handler.removeCallbacks(screenshotRunnable);
            screenshotRunnable = new CountDownRunnable(this, delay);
            handler.post(screenshotRunnable);
        } else {
            if (context instanceof ScreenshotTileService || ScreenshotTileService.Companion.getInstance() != null) {
                ScreenshotTileService tileService = (context instanceof ScreenshotTileService) ? (ScreenshotTileService) context : ScreenshotTileService.Companion.getInstance();
                tileService.setTakeScreenshotOnStopListening(true);
                Intent intent = NoDisplayActivity.newIntent(context, false);
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                }
                tileService.startActivityAndCollapse(intent);
            } else {
                Intent intent = NoDisplayActivity.newIntent(context, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                if (!(context instanceof Activity)) {
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                }
                context.startActivity(intent);
            }
        }
    }

    protected void takeScreenshotFromTileService(TileService context) {
        Intent i = NoDisplayActivity.newIntent(context, true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            i.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    private class CountDownRunnable implements Runnable {
        private final Context ctx;
        private int count;

        CountDownRunnable(Context context, int count) {
            this.count = count;
            ctx = context;
        }

        @Override
        public void run() {

            count--;
            if (count < 0) {
                screenshotHiddenCountdown(ctx, true);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }

}
