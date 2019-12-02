package diorid.tiles.screenshot

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class ScreenshotTileService : TileService(), ScreenshotPermissionListener {
    companion object {
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
            Log.e("ScreenshotTileService", "setState: IllegalStateException", e)
        } catch (e: NullPointerException) {
            Log.e("ScreenshotTileService", "setState: NullPointerException", e)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onTileAdded() {
        super.onTileAdded()
        App.acquireScreenshotPermission(this, this)
        Handler().postDelayed({
            val i = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            this.applicationContext.sendBroadcast(i)
        }, 5000)
        setState(Tile.STATE_INACTIVE)
    }

    override fun onAcquireScreenshotPermission() {
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStartListening() {
        super.onStopListening()
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStopListening() {
        super.onStopListening()
        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            App.getInstance().takeScreenshotFromTileService(this)
        }
        setState(Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        super.onClick()
        setState(Tile.STATE_ACTIVE)
        App.getInstance().screenshot(this)
    }

}