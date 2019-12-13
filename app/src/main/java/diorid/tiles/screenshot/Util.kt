package diorid.tiles.screenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore.Images
import diorid.tiles.R
import java.io.*
import java.util.*

fun screenshot(context: Context, partial: Boolean = false) {
    TakeScreenshotActivity.start(context, partial)
}

fun createImageFile(context: Context, filename: String): File {
    var storageDir: File?
    @Suppress("DEPRECATION")
    storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (storageDir == null) {
        storageDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

class CompressionOptions(var fileExtension: String = "png", val quality: Int = 100) {
    val format = when (fileExtension) {
        else -> {
            fileExtension = "png"
            Bitmap.CompressFormat.PNG
        }
    }
    val mimeType = "image/$fileExtension"
}

fun compressionPreference(): CompressionOptions {
    var prefFileFormat = "png"
    val parts = prefFileFormat.split("_")
    prefFileFormat = parts[0]
    val quality = 100
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
    val mimeType: String,
    val file: File?,
    val uri: Uri? = null,
    val fileTitle: String? = null
) : SaveImageResult("", true) {
    override fun toString(): String = "SaveImageResultSuccess($file)"
}

open class OutputStreamResult(
    val errorMessage: String = "",
    val success: Boolean = false
) : Serializable {

    override fun toString(): String = "OutputStreamResult($errorMessage)"
}

data class OutputStreamResultSuccess(
    val fileOutputStream: OutputStream,
    val imageFile: File?,
    val uri: Uri? = null,
    val contentValues: ContentValues? = null
) : OutputStreamResult("", true) {
    override fun toString(): String = "OutputStreamResultSuccess()"
}

fun createOutputStream(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date
): OutputStreamResult {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        createOutputStreamLegacy(context, fileTitle, compressionOptions)
    } else {
        createOutputStreamMediaStore(context, fileTitle, compressionOptions, date)
    }
}

fun createOutputStreamLegacy(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions
): OutputStreamResult {

    val filename = "$fileTitle.${compressionOptions.fileExtension}"

    var imageFile = createImageFile(context, filename)

    try {
        imageFile.parentFile?.mkdirs()
        imageFile.createNewFile()
    } catch (e: Exception) {
        val directory =
            context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File(directory, imageFile.name)
        try {
            directory?.mkdirs()
            imageFile.createNewFile()
        } catch (e: Exception) {
            return OutputStreamResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        return OutputStreamResult("Cannot write to file")
    }

    val outputStream: FileOutputStream
    try {
        outputStream = imageFile.outputStream()
    } catch (e: FileNotFoundException) {
        e.toString()
        return OutputStreamResult("Could not find output file")
    } catch (e: SecurityException) {
        e.toString()
        return OutputStreamResult("Could not open output file because of a security exception")
    } catch (e: IOException) {
        val error = e.toString()
        return if (error.contains("enospc", ignoreCase = true)) {
            OutputStreamResult("Could not open output file. No space left on internal device storage")
        } else {
            OutputStreamResult("Could not open output file. IOException")
        }
    } catch (e: NullPointerException) {
        val error = e.toString()
        return OutputStreamResult("Could not open output file. $error")
    }
    return OutputStreamResultSuccess(outputStream, imageFile)
}

fun createOutputStreamMediaStore(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date
): OutputStreamResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return OutputStreamResult("Dummy return")
    }

    val filename = "$fileTitle.${compressionOptions.fileExtension}"

    val resolver = context.contentResolver
    val dateMilliseconds = date.time
    val dateSeconds = dateMilliseconds / 1000
    val contentValues = ContentValues().apply {
        put(Images.ImageColumns.TITLE, fileTitle)
        put(Images.ImageColumns.DISPLAY_NAME, filename)
        put(
            Images.ImageColumns.DESCRIPTION, context.getString(
                R.string.file_description,
                SimpleDateFormat(
                    context.getString(R.string.file_description_simple_date_format),
                    Locale.getDefault()
                ).format(Date())
            )
        )
        put(Images.ImageColumns.DATE_TAKEN, dateMilliseconds)
        put(Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        put(Images.ImageColumns.MIME_TYPE, compressionOptions.mimeType)
        put(
            Images.ImageColumns.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
        )
        put(Images.ImageColumns.IS_PENDING, 1)
    }

    val uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return OutputStreamResult("MediaStore failed to provide a file")
    val outputStream =
        resolver.openOutputStream(uri)
            ?: return OutputStreamResult("Could not open output stream from MediaStore")

    return OutputStreamResultSuccess(outputStream, null, uri, contentValues)
}

fun saveImageToFile(
    context: Context,
    image: Image,
    prefix: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?
): SaveImageResult {
    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val filename = "$prefix$timeStamp"

    val outputStreamResult = createOutputStream(context, filename, compressionOptions, date)

    if (!outputStreamResult.success && outputStreamResult !is OutputStreamResultSuccess) {
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?)
            ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    val bitmap = imageToBitmap(image, cutOutRect)
    image.close()

    if (bitmap.width == 0 || bitmap.height == 0) {
        return SaveImageResult("Bitmap is empty")
    }

    val bytes = ByteArrayOutputStream()
    bitmap.compress(compressionOptions.format, compressionOptions.quality, bytes)

    var success = false
    var error = ""
    try {
        outputStream.write(bytes.toByteArray())
        success = true
    } catch (e: FileNotFoundException) {
        error = e.toString()
    } catch (e: SecurityException) {
        error = e.toString()
    } catch (e: IOException) {
        error = e.toString()
        if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
        }

    } catch (e: NullPointerException) {
        error = e.toString()
    } finally {
        outputStream.close()
    }

    if (!success) {
        return SaveImageResult("Could not save image file:\n$error")
    }

    return when {
        result.imageFile != null -> {
            addImageToGallery(
                context,
                result.imageFile.absolutePath,
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
                compressionOptions.mimeType,
                date
            )
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, result.imageFile)
        }
        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result.contentValues?.run {
                    this.clear()
                    this.put(Images.ImageColumns.IS_PENDING, 0)
                    context.contentResolver.update(result.uri, this, null, null)
                }
            }
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, null, result.uri, filename)
        }
        else -> SaveImageResult("Could not save image file, no URI")
    }
}
