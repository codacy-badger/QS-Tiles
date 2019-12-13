package diorid.tiles.screenshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.net.Uri
import android.widget.Toast
import diorid.tiles.R

const val NOTIFICATION_ACTION_SHARE = "NOTIFICATION_ACTION_SHARE"
const val NOTIFICATION_ACTION_DELETE = "NOTIFICATION_ACTION_DELETE"
const val NOTIFICATION_ACTION_EDIT = "NOTIFICATION_ACTION_EDIT"
const val NOTIFICATION_ACTION_STOP = "NOTIFICATION_ACTION_STOP"
const val NOTIFICATION_ACTION_DATA_URI = "NOTIFICATION_ACTION_DATA_URI"
const val NOTIFICATION_ACTION_DATA_MIME_TYPE = "NOTIFICATION_ACTION_DATA_MIME_TYPE"
const val NOTIFICATION_ACTION_ID = "NOTIFICATION_ACTION_ID"

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationActionRcver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.apply {
            when (intent?.action) {
                NOTIFICATION_ACTION_SHARE -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val shareIntent = shareImageChooserIntent(this, path, mimeType)
                    shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                        if (ScreenshotTileService.instance != null) {
                            ScreenshotTileService.instance?.startActivityAndCollapse(shareIntent)
                        } else {
                            startActivity(shareIntent)
                        }
                    }
                }
                NOTIFICATION_ACTION_DELETE -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))

                    if (path != null && deleteImage(this, path)) {
                        Toast.makeText(
                            this,
                            context.getString(R.string.screenshot_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            context.getString(R.string.screenshot_delete_failed),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
                NOTIFICATION_ACTION_EDIT -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val shareIntent = editImageChooserIntent(this, path, mimeType)
                    shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                        if (ScreenshotTileService.instance != null) {
                            ScreenshotTileService.instance?.startActivityAndCollapse(shareIntent)
                        } else {
                            startActivity(shareIntent)
                        }
                    }
                }
                NOTIFICATION_ACTION_STOP -> {
                    ScreenshotTileService.instance?.background()
                    hideNotification(this, ScreenshotTileService.FOREGROUND_NOTIFICATION_ID)
                }

            }
        }
    }

    fun registerReceiver(context: Screenshot) {
        var intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_SHARE)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_DELETE)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_EDIT)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_STOP)
        context.registerReceiver(this, intentFilter)
    }
}
