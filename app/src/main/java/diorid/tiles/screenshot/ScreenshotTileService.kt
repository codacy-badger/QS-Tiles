package diorid.tiles.screenshot

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import diorid.tiles.R

class ScreenshotTileService : TileService(), onGrantedScreenshotPermission {
    companion object {
        private const val TAG = "ScreenshotTileService"
        const val FOREGROUND_NOTIFICATION_ID = 1234
        var instance: ScreenshotTileService? = null
    }

    var screenshotPermission: Intent? = null
    var takeScreenshotOnStopListening = false

    private fun setState(newState: Int) {
        try {
            qsTile?.run {
                state = newState
                updateTile()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "setState: IllegalStateException", e)
        } catch (e: NullPointerException) {
            Log.e(TAG, "setState: NullPointerException", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "setState: IllegalArgumentException", e)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onTileAdded() {
        super.onTileAdded()
        Log.v(TAG, "onTileAdded()")

        Screenshot.acquireScreenshotPermission(this, this)

        setState(Tile.STATE_INACTIVE)
    }

    override fun onAcquireScreenshotPermission() {
        Log.v(TAG, "onAcquireScreenshotPermission()")
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStartListening() {
        super.onStopListening()
        Log.v(TAG, "onStartListening()")
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.v(TAG, "onStopListening()")

        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            Screenshot.getInstance().takeScreenshotFromTileService(this)
        }
        setState(Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        super.onClick()
        Log.v(TAG, "onClick()")

        foreground()

        setState(Tile.STATE_ACTIVE)
        Screenshot.getInstance().screenshot(this)
    }

    fun foreground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        val context = this

        val builder = Notification.Builder(this, createNotificationForegroundServiceChannel(this))
        builder.apply {
            setShowWhen(false)
            setContentTitle(getString(R.string.notification_foreground_title))
            setContentText(getString(R.string.notification_foreground_body))
            setAutoCancel(true)
            setSmallIcon(R.drawable.icon)
            setContentIntent(PendingIntent.getBroadcast(context, 1, Intent().apply {
                action = NOTIFICATION_ACTION_STOP
                putExtra(NOTIFICATION_ACTION_ID, FOREGROUND_NOTIFICATION_ID)
            }, 0))
        }
        startForeground(
            TakeScreenshotActivity.FOREGROUND_SERVICE_ID,
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    fun background() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        stopForeground(true)
    }


}
