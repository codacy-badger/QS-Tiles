package diorid.tiles.screenshot

import android.os.Bundle

class LongClickTileActivity : TransparentContainerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ScreenshotTileService.instance?.let {
            Screenshot.acquireScreenshotPermission(this, it)
        }

    }

    override fun onStart() {
        super.onStart()

        Screenshot.getInstance().screenshotPartial(applicationContext)
        finish()
    }
}
