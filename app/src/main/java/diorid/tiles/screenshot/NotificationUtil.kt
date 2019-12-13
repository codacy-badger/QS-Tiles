package diorid.tiles.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import diorid.tiles.R

fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_channel_description)
        val notificationTitle = context.getString(R.string.notification_title)
        val channelDescription =
            context.getString(R.string.notification_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = channelDescription
                        enableVibration(false)
                        enableLights(false)
                        setSound(null, null)
                    })
            }
        }
    }
    return TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
}

fun createNotificationForegroundServiceChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_foreground_channel_description)
        val notificationTitle = context.getString(R.string.notification_foreground_title)
        val channelDescription =
            context.getString(R.string.notification_foreground_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = channelDescription
                        enableVibration(false)
                        enableLights(false)
                        setSound(null, null)
                    })
            }
        }
    }
    return TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND
}

fun createNotification(
    context: Context,
    path: Uri,
    bitmap: Bitmap,
    screenDensity: Int,
    mimeType: String
) {
    val appContext = context.applicationContext

    val bigPicture = resizeToBigPicture(bitmap)

    val largeIcon = resizeToNotificationIcon(bitmap, screenDensity)

    val uniqueId =
        (System.currentTimeMillis() and 0xfffffff).toInt()

    val openImageIntent = openImageIntent(path, mimeType)
    val contentPendingIntent =
        PendingIntent.getActivity(appContext, uniqueId + 1, openImageIntent, 0)

    val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(appContext, createNotificationScreenshotTakenChannel(appContext))
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(appContext)
    }
    builder.apply {
        setWhen(Calendar.getInstance().timeInMillis)
        setShowWhen(true)
        setContentTitle(appContext.getString(R.string.notification_title))
        setContentText(appContext.getString(R.string.notification_body))
        setSmallIcon(R.drawable.ic_notify_image)
        setLargeIcon(largeIcon)
        setAutoCancel(true)
        style = Notification.BigPictureStyle().bigPicture(bigPicture).bigLargeIcon(null as? Icon?)
        if (openImageIntent.resolveActivity(context.applicationContext.packageManager) != null) {
            setContentIntent(contentPendingIntent)
        }
    }

    val icon = Icon.createWithResource(
        appContext,
        R.drawable.icon
    )

    val shareIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_SHARE)
    val pendingIntentShare = PendingIntent.getBroadcast(appContext, uniqueId + 3, shareIntent, 0)
    builder.addAction(
        Notification.Action.Builder(
            icon,
            appContext.getString(R.string.notification_share_screenshot),
            pendingIntentShare
        ).build()
    )

    if (editImageIntent(
            path,
            mimeType
        ).resolveActivity(context.applicationContext.packageManager) != null
    ) {
        val editIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_EDIT)
        val pendingIntentEdit = PendingIntent.getBroadcast(appContext, uniqueId + 4, editIntent, 0)
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getString(R.string.notification_edit_screenshot),
                pendingIntentEdit
            ).build()
        )
    }

    val deleteIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_DELETE)
    val pendingIntentDelete = PendingIntent.getBroadcast(appContext, uniqueId + 2, deleteIntent, 0)
    builder.addAction(
        Notification.Action.Builder(
            icon,
            appContext.getString(R.string.notification_delete_screenshot),
            pendingIntentDelete
        ).build()
    )

    Screenshot.registerNotificationReceiver()
    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        notify(uniqueId, builder.build())
    }

    largeIcon.recycle()
    bigPicture.recycle()
}

fun actionButtonIntent(
    path: Uri,
    mimeType: String,
    notificationId: Int,
    intentAction: String
): Intent {
    return Intent().apply {
        action = intentAction
        putExtra(NOTIFICATION_ACTION_DATA_URI, path.toString())
        putExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE, mimeType)
        putExtra(NOTIFICATION_ACTION_ID, notificationId)
    }
}

fun shareImageChooserIntent(context: Context, path: Uri, mimeType: String): Intent {
    Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, path)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(
            this,
            context.getString(R.string.notification_app_chooser_share)
        )
    }
}

fun editImageIntent(path: Uri, mimeType: String): Intent {
    return Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(path, mimeType)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}

fun editImageChooserIntent(context: Context, path: Uri, mimeType: String): Intent {
    editImageIntent(path, mimeType).apply {
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_edit))
    }
}

fun openImageIntent(path: Uri, mimeType: String): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(path, mimeType)
    }
}

fun hideNotification(context: Context, notificationId: Int) {
    (context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        cancel(notificationId)
    }
}

