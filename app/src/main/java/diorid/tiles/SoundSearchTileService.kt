package diorid.tiles

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class SoundSearchTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val url =
            "https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox"
        val action = "com.google.android.googlequicksearchbox.MUSIC_SEARCH"

        try {
            val i = Intent()
            i.action = action
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivityAndCollapse(i)
        } catch (e: ActivityNotFoundException) {
            qsTile.state = Tile.STATE_INACTIVE
            Toast.makeText(this, "Google App is not installed on your device.", Toast.LENGTH_SHORT)
                .show()
            val i = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= 28) {
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            i.data = Uri.parse(url)
            startActivityAndCollapse(i)

        }

    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
