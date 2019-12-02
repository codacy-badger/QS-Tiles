package diorid.tiles.screenshot

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.util.DisplayMetrics
import android.view.Surface
import android.widget.Toast
import diorid.tiles.R
import java.lang.ref.WeakReference

class TakeScreenshotActivity : Activity(), ScreenshotPermissionListener {

    companion object {
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400
        const val NOTIFICATION_BIG_PICTURE_MAX_HEIGHT = 1024
        const val THREAD_START = 1
        const val THREAD_FINISHED = 2

        fun start(context: Context) {
            context.startActivity(newIntent(context))
        }

        private fun newIntent(context: Context): Intent {
            return Intent(context, TakeScreenshotActivity::class.java)
        }

    }

    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenSharing: Boolean = false
    private var virtualDisplay: VirtualDisplay? = null
    private var surface: Surface? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var handler = SaveImageHandler(this)
    private var thread: Thread? = null
    private var saveImageResult: SaveImageResult? = null

    private var askedForPermission = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        with(DisplayMetrics()) {
            windowManager.defaultDisplay.getRealMetrics(this)
            screenDensity = densityDpi
            screenWidth = widthPixels
            screenHeight = heightPixels
        }

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
        surface = imageReader?.surface

        if (packageManager.checkPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        packageName
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            App.requestStoragePermission(this)
            return
        }

        if (!askedForPermission) {
            askedForPermission = true
            App.acquireScreenshotPermission(this, this)
        } else {
        }
    }

    override fun onAcquireScreenshotPermission() {
        ScreenshotTileService.instance?.onAcquireScreenshotPermission()
        prepareForScreenSharing()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mediaProjection != null) {
            mediaProjection?.stop()
            mediaProjection = null
        }
    }

    private fun prepareForScreenSharing() {
        saveImageResult = null
        screenSharing = true
        mediaProjection = App.createMediaProjection()
        if (surface == null) {
            screenShotFailedToast("Failed to create ImageReader surface")
            finish()
            return
        }
        if (mediaProjection == null) {
            if (!askedForPermission) {
                askedForPermission = true
                App.acquireScreenshotPermission(this, this)
            }
            mediaProjection = App.createMediaProjection()
            if (mediaProjection == null) {
                screenShotFailedToast("Failed to create MediaProjection")
                finish()
                return
            }
        }
        startVirtualDisplay()
    }

    private fun startVirtualDisplay() {
        virtualDisplay = createVirtualDisplay()
        imageReader?.setOnImageAvailableListener({
            it.setOnImageAvailableListener(null, null)
            saveImage()
        }, null)
    }

    private fun saveImage() {
        if (imageReader == null) {
            stopScreenSharing()
            screenShotFailedToast("Could not start screen capture")
            finish()
            return
        }
        val image = try {
            imageReader?.acquireNextImage()
        } catch (e: UnsupportedOperationException) {
            stopScreenSharing()
            screenShotFailedToast("Could not acquire image.\nUnsupportedOperationException\nThis device is not supported.")
            finish()
            return
        }
        stopScreenSharing()
        if (image == null) {
            screenShotFailedToast("Could not acquire image")
            finish()
            return
        }

        if (image.width == 0 || image.height == 0) {
            screenShotFailedToast("Incorrect image dimensions: ${image.width}x${image.width}")
            finish()
            return
        }

        val compressionOptions = compressionPreference(applicationContext)

        thread = Thread(Runnable {
            saveImageResult = saveImageToFile(applicationContext, image, "Screenshot_", compressionOptions)
            image.close()

            handler.sendEmptyMessage(THREAD_FINISHED)
        })
        handler.sendEmptyMessage(THREAD_START)
    }

    class SaveImageHandler(takeScreenshotActivity: TakeScreenshotActivity) : Handler() {
        private var activity: WeakReference<TakeScreenshotActivity> = WeakReference(takeScreenshotActivity)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == THREAD_START) {
                activity.get()!!.thread!!.start()
            } else if (msg.what == THREAD_FINISHED) {
                activity.get()?.onFileSaved()
            }
        }
    }

    private fun onFileSaved() {
        if (saveImageResult == null) {
            screenShotFailedToast("saveImageResult is null")
            finish()
            return
        }
        if (saveImageResult?.success != true) {
            screenShotFailedToast(saveImageResult?.errorMessage)
            finish()
            return
        }

        val result = saveImageResult as? SaveImageResultSuccess?

        result?.let {
            Toast.makeText(
                    this,
                    getString(R.string.screenshot_file_saved, it.file.canonicalFile), Toast.LENGTH_LONG
            ).show()
            createNotification(
                    this,
                    Uri.fromFile(it.file),
                    it.bitmap,
                    screenDensity
            )
            if (!it.bitmap.isRecycled) {
                it.bitmap.recycle()
            }
        } ?: screenShotFailedToast("Failed to cast SaveImageResult")

        saveImageResult = null
        finish()
    }

    private fun stopScreenSharing() {
        screenSharing = false
        virtualDisplay?.release()
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
                "ScreenshotTaker",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
        )
    }

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}



