package diorid.tiles.screenshot

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min

fun imageToBitmap(image: Image, rect: Rect? = null): Bitmap {
    val offset =
        (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return if (rect == null) {
        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    } else {
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }
}

fun resizeToNotificationIcon(bitmap: Bitmap, screenDensity: Int): Bitmap {
    val maxSize = (min(
        max(screenDensity / 2, TakeScreenshotActivity.NOTIFICATION_PREVIEW_MIN_SIZE),
        TakeScreenshotActivity.NOTIFICATION_PREVIEW_MAX_SIZE
    )).toDouble()

    val ratioX = maxSize / bitmap.width
    val ratioY = maxSize / bitmap.height
    val ratio = min(ratioX, ratioY)
    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
}

fun resizeToBigPicture(bitmap: Bitmap): Bitmap {
    return if (bitmap.height > TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) {
        val offsetY =
            (bitmap.height - TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) / 2
        Bitmap.createBitmap(
            bitmap, 0, offsetY, bitmap.width,
            TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT
        )
    } else {
        bitmap
    }
}

fun addImageToGallery(
    context: Context,
    filepath: String,
    title: String,
    description: String,
    mimeType: String = "image/png",
    date: Date? = null
): Uri? {
    val dateSeconds = (date?.time ?: System.currentTimeMillis()) / 1000
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, title)
        put(MediaStore.Images.Media.DESCRIPTION, description)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        @Suppress("DEPRECATION")
        put(MediaStore.MediaColumns.DATA, filepath)
    }
    return context.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

fun deleteImage(context: Context, uri: Uri?): Boolean {
    if (uri == null) {
        return false
    }

    uri.normalizeScheme()
    when {
        uri.scheme == "content" -> {
            val deletedRows = context.contentResolver.delete(uri, null, null)
            return deletedRows > 0
        }

        uri.scheme == "file" -> {
            val path = uri.path ?: return false

            val file = File(path)

            if (!file.exists()) {
                return false
            }

            if (!file.canWrite()) {
                return false
            }

            if (file.delete()) {
                val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Images.Media._ID)
                @Suppress("DEPRECATION")
                val selection = MediaStore.Images.Media.DATA + " = ?"
                val queryArgs = arrayOf(file.absolutePath)
                context.contentResolver.query(
                    externalContentUri,
                    projection,
                    selection,
                    queryArgs,
                    null
                )?.apply {
                    if (moveToFirst()) {
                        val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        context.contentResolver.delete(contentUri, null, null)
                    }
                    close()
                }
            }
        }


    }

    return true
}
