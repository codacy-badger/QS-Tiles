package diorid.tiles.screenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import diorid.tiles.R
import diorid.tiles.screenshot.TakeScreenshotActivity.Companion.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT
import diorid.tiles.screenshot.TakeScreenshotActivity.Companion.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
import diorid.tiles.screenshot.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MAX_SIZE
import diorid.tiles.screenshot.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MIN_SIZE
import java.io.*
import java.util.*
import kotlin.math.max
import kotlin.math.min


fun screenshot(context: Context) {
    TakeScreenshotActivity.start(context)
}

fun imageToBitmap(image: Image): Bitmap {
    val offset = (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
}

fun addImageToGallery(
        context: Context,
        filepath: String,
        title: String,
        description: String,
        mimeType: String = "image/jpeg"
): Uri? {
    val values = ContentValues()
    values.put(Images.Media.TITLE, title)
    values.put(Images.Media.DESCRIPTION, description)
    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
    values.put(Images.Media.MIME_TYPE, mimeType)
    values.put(MediaStore.MediaColumns.DATA, filepath)
    return context.contentResolver?.insert(Images.Media.EXTERNAL_CONTENT_URI, values)
}

fun createImageFile(context: Context, filename: String): File {
    var storageDir: File?
    storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (storageDir == null) {
        Log.e("Utils.kt:createImageFile()", "Fallback to getExternalFilesDir(Environment.DIRECTORY_PICTURES)")
        storageDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

class CompressionOptions(var fileExtension: String = "png", val quality: Int = 100) {
    val format = run {
        fileExtension = "png"
        Bitmap.CompressFormat.PNG
    }
    val mimeType = "image/$fileExtension"
}

fun compressionPreference(context: Context): CompressionOptions {
    var prefFileFormat = context.getString(R.string.setting_file_format_value_default)
    val parts = prefFileFormat.split("_")
    prefFileFormat = parts[0]
    val quality = if (parts.size > 1) {
        parts[1].toInt()
    } else 100
    return CompressionOptions(prefFileFormat, quality)
}

open class SaveImageResult(
        val errorMessage: String = "",
        val success: Boolean = false
) : Serializable {
    override fun toString(): String = "SaveImageResult($errorMessage)"
}

data class SaveImageResultSuccess(
        val bitmap: Bitmap,
        val file: File
) : SaveImageResult("", true) {
    override fun toString(): String = "SaveImageResultSuccess($file)"
}

fun saveImageToFile(
        context: Context,
        image: Image,
        prefix: String,
        compressionOptions: CompressionOptions = CompressionOptions()
): SaveImageResult {
    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val filename = "$prefix$timeStamp"

    var imageFile = createImageFile(context, "$filename.${compressionOptions.fileExtension}")

    try {
        imageFile.createNewFile()
    } catch (e: Exception) {
        Log.e("Utils.kt:saveImageToFile()", "Could not createNewFile() ${imageFile.absolutePath} $e")
        imageFile = File(context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFile.name)
        try {
            imageFile.createNewFile()
        } catch (e: Exception) {
            Log.e(
                    "Utils.kt:saveImageToFile()",
                    "Could not createNewFile() for fallback file ${imageFile.absolutePath} $e"
            )
            return SaveImageResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e("Utils.kt:saveImageToFile()", "File ${imageFile.absolutePath} does not exist or is not writable")
        return SaveImageResult("Cannot write to file")
    }

    val bitmap = imageToBitmap(image)
    image.close()

    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e("Utils.kt:saveImageToFile()", "Bitmap width or height is 0")
        return SaveImageResult("Bitmap is empty")
    }

    val bytes = ByteArrayOutputStream()
    bitmap.compress(compressionOptions.format, compressionOptions.quality, bytes)

    var outputStream: FileOutputStream? = null
    var success = false
    var error = ""
    try {
        outputStream = imageFile.outputStream()
        outputStream.write(bytes.toByteArray())
        success = true
    } catch (e: FileNotFoundException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
    } catch (e: SecurityException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
    } catch (e: IOException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
        if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
        }

    } catch (e: NullPointerException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
    } finally {
        outputStream?.close()
    }

    if (!success) {
        return SaveImageResult("Could not save image file:\n$error")
    }

    addImageToGallery(
            context,
            imageFile.absolutePath,
            context.getString(R.string.file_title),
            context.getString(
                    R.string.file_description,
                    SimpleDateFormat(
                            context.getString(R.string.file_description_simple_date_format),
                            Locale.getDefault()
                    ).format(
                            date
                    )
            ),
            compressionOptions.mimeType
    )

    return SaveImageResultSuccess(bitmap, imageFile)
}

fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_channel_description)
        val notificationTitle = context.getString(R.string.notification_title)
        val channelDescription = context.getString(R.string.notification_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN) == null) {
                createNotificationChannel(NotificationChannel(
                        NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN,
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
    return NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
}

fun resizeToNotificationIcon(bitmap: Bitmap, screenDensity: Int): Bitmap {
    val maxSize = (min(max(screenDensity / 2, NOTIFICATION_PREVIEW_MIN_SIZE), NOTIFICATION_PREVIEW_MAX_SIZE)).toDouble()

    val ratioX = maxSize / bitmap.width
    val ratioY = maxSize / bitmap.height
    val ratio = min(ratioX, ratioY)
    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
}

fun resizeToBigPicture(bitmap: Bitmap): Bitmap {
    return if (bitmap.height > NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) {
        val offsetY = (bitmap.height - NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) / 2
        Bitmap.createBitmap(bitmap, 0, offsetY, bitmap.width, NOTIFICATION_BIG_PICTURE_MAX_HEIGHT)
    } else {
        bitmap
    }
}

fun createNotification(context: Context, path: Uri, bitmap: Bitmap, screenDensity: Int) {
    val appContext = context.applicationContext

    val bigPicture = resizeToBigPicture(bitmap)

    val largeIcon = resizeToNotificationIcon(bitmap, screenDensity)

    val uniqueId =
            (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification

    val openImageIntent = openImageIntent(path)
    val contentPendingIntent = PendingIntent.getActivity(appContext, uniqueId + 1, openImageIntent, 0)

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
        setSmallIcon(R.drawable.icon)
        setLargeIcon(largeIcon)
        setAutoCancel(true)
        style = Notification.BigPictureStyle().bigPicture(bigPicture).bigLargeIcon(null as Icon?)
        if (openImageIntent.resolveActivity(context.applicationContext.packageManager) != null) {
            setContentIntent(contentPendingIntent)
        }
    }

    val icon = Icon.createWithResource(
            appContext,
            R.drawable.icon
    )

    val shareIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_SHARE)
    val pendingIntentShare = PendingIntent.getBroadcast(appContext, uniqueId + 3, shareIntent, 0)
    builder.addAction(
            Notification.Action.Builder(
                    icon,
                    appContext.getString(R.string.notification_share_screenshot),
                    pendingIntentShare
            ).build()
    )

    if (editImageIntent(path).resolveActivity(context.applicationContext.packageManager) != null) {
        val editIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_EDIT)
        val pendingIntentEdit = PendingIntent.getBroadcast(appContext, uniqueId + 4, editIntent, 0)
        builder.addAction(
                Notification.Action.Builder(
                        icon,
                        appContext.getString(R.string.notification_edit_screenshot),
                        pendingIntentEdit
                ).build()
        )
    }

    val deleteIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_DELETE)
    val pendingIntentDelete = PendingIntent.getBroadcast(appContext, uniqueId + 2, deleteIntent, 0)
    builder.addAction(
            Notification.Action.Builder(
                    icon,
                    appContext.getString(R.string.notification_delete_screenshot),
                    pendingIntentDelete
            ).build()
    )

    App.registerNotificationReceiver()

    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        notify(uniqueId, builder.build())
    }

    largeIcon.recycle()
    bigPicture.recycle()
}

fun actionButtonIntent(path: Uri, notificationId: Int, intentAction: String): Intent {
    return Intent().apply {
        action = intentAction
        putExtra(NOTIFICATION_ACTION_DATA_URI, path.toString())
        putExtra(NOTIFICATION_ACTION_ID, notificationId)
    }
}

fun shareImageChooserIntent(context: Context, path: Uri): Intent {
    Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, path)
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_share))
    }
}

fun editImageIntent(path: Uri): Intent {
    return Intent(Intent.ACTION_EDIT).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        setDataAndType(path, "image/png")
    }
}

fun editImageChooserIntent(context: Context, path: Uri): Intent {
    editImageIntent(path).apply {
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_edit))
    }
}

fun openImageIntent(path: Uri): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        setDataAndType(path, "image/png")
    }
}

fun hideNotification(context: Context, notificationId: Int) {
    (context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        cancel(notificationId)
    }
}

fun deleteImage(context: Context, file: File): Boolean {
    if (!file.exists()) {
        Log.w("Screenshot", "File does not exist: ${file.absoluteFile}")
        return false
    }

    if (!file.canWrite()) {
        Log.w("Screenshot", "File is not writable: ${file.absoluteFile}")
        return false
    }

    if (file.delete()) {
        val uri = Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(Images.Media._ID)
        val selection = Images.Media.DATA + " = ?"
        val queryArgs = arrayOf(file.absolutePath)
        context.contentResolver.query(uri, projection, selection, queryArgs, null)?.apply {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(Images.Media._ID))
                val contentUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id)
                context.contentResolver.delete(contentUri, null, null)
            }
            close()
        }
    } else {
        Log.w("Screenshot", "Could not delete file: ${file.absoluteFile}")
        return false
    }

    return true
}
