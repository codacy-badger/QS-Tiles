package diorid.tiles

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ImmersiveTileService : TileService() {

    override fun onClick() {
        super.onClick()

        if (hasSecureSettingsGranted()) {
            val check = Settings.Global.getString(contentResolver, "policy_control")
            if (check == "immersive.none=*") {
                this.immersiveModeFull(exclude = false, fullHide = false)
                qsTile.state = Tile.STATE_ACTIVE
            } else {
                this.immersiveModeReset()
                qsTile.state = Tile.STATE_INACTIVE
            }
            qsTile.updateTile()
        }

    }

    private fun hasSecureSettingsGranted(): Boolean {
        val checkVal = checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
        if (checkVal == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (checkVal == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(
                this,
                "WRITE_SECURE_SETTINGS has not been granted to the application! Check it again.",
                Toast.LENGTH_LONG
            ).show()
            val i = Intent(Intent.ACTION_VIEW)
            if (Build.VERSION.SDK_INT >= 28) {
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val url = "https://bit.ly/adbpermission"
            i.data = Uri.parse(url)
            startActivityAndCollapse(i)
            return false
        }
        return false
    }
}
