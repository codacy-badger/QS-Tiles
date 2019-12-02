package diorid.tiles

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class NotificationHistoryTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val intent = Intent()
        intent.action = "android.intent.action.MAIN"
        if (Build.VERSION.SDK_INT >= 28) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        intent.addCategory("android.intent.category.LAUNCHER")
        intent.setClassName(
            "com.android.settings",
            "com.android.settings.Settings\$NotificationStationActivity"
        )


        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "Cannot open notification log.", Toast.LENGTH_SHORT).show()
        } else {
            startActivityAndCollapse(intent)
        }

    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
