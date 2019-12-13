package diorid.tiles.screenshot

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import diorid.tiles.R
import diorid.tiles.screenshot.Screenshot.setScreenshotPermission

class AcquireScreenshotPermission : Activity() {
    companion object {
        const val EXTRA_REQUEST_PERMISSION_SCREENSHOT = "extra_request_permission_screenshot"
        const val EXTRA_REQUEST_PERMISSION_STORAGE = "extra_request_permission_storage"
        private const val SCREENSHOT_REQUEST_CODE = 4455
        private const val WRITE_REQUEST_CODE = 12345
    }

    private var askedForStoragePermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra(EXTRA_REQUEST_PERMISSION_STORAGE, false)) {
            askedForStoragePermission = true
        }

        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permissions, WRITE_REQUEST_CODE)
        }

        if (intent.getBooleanExtra(EXTRA_REQUEST_PERMISSION_SCREENSHOT, false)) {
            (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager)?.apply {
                Screenshot.setMediaProjectionManager(this)
                startActivityForResult(createScreenCaptureIntent(), SCREENSHOT_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SCREENSHOT_REQUEST_CODE == requestCode) {
            if (RESULT_OK == resultCode) {
                data?.run {
                    (data.clone() as? Intent)?.apply {
                        setScreenshotPermission(this)
                    }
                }
            } else {
                setScreenshotPermission(null)
                Toast.makeText(
                    this,
                    getString(R.string.permission_missing_screen_capture), Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (WRITE_REQUEST_CODE == requestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (askedForStoragePermission) {
                    Screenshot.getInstance().screenshot(this)
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_missing_external_storage), Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }
}
