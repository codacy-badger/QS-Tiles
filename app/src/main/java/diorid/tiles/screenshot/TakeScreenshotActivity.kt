package diorid.tiles.screenshot

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import diorid.tiles.BuildConfig.APPLICATION_ID
import diorid.tiles.R
import java.lang.ref.WeakReference

class TakeScreenshotActivity : Activity(), onGrantedScreenshotPermission {

    companion object {
        private const val TAG = "TakeScreenshotActivity"
        const val FOREGROUND_SERVICE_ID = 1111
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val NOTIFICATION_CHANNEL_FOREGROUND = "notification_channel_foreground"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400
        const val NOTIFICATION_BIG_PICTURE_MAX_HEIGHT = 1024
        const val THREAD_START = 1
        const val THREAD_FINISHED = 2
        const val EXTRA_PARTIAL = "$APPLICATION_ID.TakeScreenshotActivity.EXTRA_PARTIAL"

        fun start(context: Context, partial: Boolean = false) {
            context.startActivity(newIntent(context, partial))
        }

        private fun newIntent(context: Context, partial: Boolean = false): Intent {
            return Intent(context, TakeScreenshotActivity::class.java).apply {
                putExtra(EXTRA_PARTIAL, partial)
            }
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
    private var cutOutRect: Rect? = null
    private var handler = SaveImageHandler(this)
    private var thread: Thread? = null
    private var saveImageResult: SaveImageResult? = null
    private var partial = false

    private var askedForPermission = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        partial = intent?.getBooleanExtra(EXTRA_PARTIAL, false) ?: false

        with(DisplayMetrics()) {
            windowManager.defaultDisplay.getRealMetrics(this)
            screenDensity = densityDpi
            screenWidth = widthPixels
            screenHeight = heightPixels
        }

        @SuppressLint("WrongConstant")
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)

        surface = imageReader?.surface

        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Screenshot.requestStoragePermission(this)
            return
        }

        if (!askedForPermission) {
            askedForPermission = true
            Screenshot.acquireScreenshotPermission(this, this)
        } else {
            Log.v(TAG, "onCreate() else")

        }

    }

    private fun partialScreenshot() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContentView(R.layout.partial_screenshot)
        val mScreenshotSelectorView =
            findViewById<PartialScreenshotActivity>(R.id.global_screenshot_selector)
        mScreenshotSelectorView.text = getString(R.string.take_screenshot)
        mScreenshotSelectorView.shutter = R.drawable.ic_screenshot
        mScreenshotSelectorView.onShutter = {
            cutOutRect = it
            prepareForScreenSharing()
        }

    }

    override fun onAcquireScreenshotPermission() {
        ScreenshotTileService.instance?.onAcquireScreenshotPermission()

        if (partial) {
            partialScreenshot()
        } else {
            prepareForScreenSharing()
        }
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
        mediaProjection = Screenshot.createMediaProjection()
        if (surface == null) {
            screenshotFailed("Failed to create ImageReader surface")
            finish()
            return
        }
        if (mediaProjection == null) {
            if (!askedForPermission) {
                askedForPermission = true
                Screenshot.acquireScreenshotPermission(this, this)
            }
            mediaProjection = Screenshot.createMediaProjection()
            if (mediaProjection == null) {
                screenshotFailed("Failed to create MediaProjection")
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
            screenshotFailed("Could not start screen capture")
            finish()
            return
        }

        val image = try {
            imageReader?.acquireNextImage()
        } catch (e: UnsupportedOperationException) {
            stopScreenSharing()
            screenshotFailed("Could not acquire image.\nUnsupportedOperationException\nThis device is not supported.")
            finish()
            return
        }
        stopScreenSharing()
        if (image == null) {
            screenshotFailed("Could not acquire image")
            finish()
            return
        }

        if (image.width == 0 || image.height == 0) {
            screenshotFailed("Incorrect image dimensions: ${image.width}x${image.width}")
            finish()
            return
        }

        val compressionOptions = compressionPreference()

        thread = Thread(Runnable {
            saveImageResult = saveImageToFile(
                applicationContext,
                image,
                "Screenshot_",
                compressionOptions,
                cutOutRect
            )
            image.close()

            handler.sendEmptyMessage(THREAD_FINISHED)
        })
        handler.sendEmptyMessage(THREAD_START)
    }

    class SaveImageHandler(takeScreenshotActivity: TakeScreenshotActivity) : Handler() {
        private var activity: WeakReference<TakeScreenshotActivity> =
            WeakReference(takeScreenshotActivity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == THREAD_START) {
                activity.get()?.thread?.start()
            } else if (msg.what == THREAD_FINISHED) {
                activity.get()?.onFileSaved()
            }
        }
    }

    private fun onFileSaved() {
        if (saveImageResult == null) {
            screenshotFailed("saveImageResult is null")
            finish()
            return
        }
        if (saveImageResult?.success != true) {
            screenshotFailed(saveImageResult?.errorMessage)
            finish()
            return
        }

        val result = saveImageResult as? SaveImageResultSuccess?

        when {
            result == null -> {
                screenshotFailed("Failed to cast SaveImageResult")
            }
            result.uri != null -> {
                val dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/$SCREENSHOT_DIRECTORY/${result.fileTitle}"
                Toast.makeText(
                    this,
                    getString(R.string.screenshot_file_saved, dummyPath), Toast.LENGTH_LONG
                ).show()

                createNotification(
                    this,
                    result.uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
            }
            result.file != null -> {
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                Toast.makeText(
                    this,
                    getString(R.string.screenshot_file_saved, path), Toast.LENGTH_LONG
                ).show()

                createNotification(
                    this,
                    uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
            }
            else -> {
                screenshotFailed("Failed to cast SaveImageResult path/uri")
            }
        }

        saveImageResult = null

        ScreenshotTileService.instance?.run {
            background()
        }

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

    private fun screenshotFailed(errorMessage: String? = null) {
        val message = getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}



