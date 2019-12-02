package diorid.tiles

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class RearFlashTileService : TileService() {

    private var isTorchOn: Boolean = false

    override fun onTileAdded() {
        super.onTileAdded()

        val isFlashAvailable = applicationContext.packageManager
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (!isFlashAvailable) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            Toast.makeText(this, "Your device don't support rear flash", Toast.LENGTH_SHORT).show()
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        qsTile.updateTile()

        if (isTorchOn) {
            turnOffFlashLight()
        } else {
            turnOnFlashLight()
        }

    }

    override fun onStartListening() {
        super.onStartListening()

        qsTile.state = Tile.STATE_INACTIVE

    }

    private fun turnOnFlashLight() {
        val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId: String
        qsTile.state = Tile.STATE_ACTIVE
        isTorchOn = true
        try {
            cameraId = camManager.cameraIdList[0]
            camManager.setTorchMode(cameraId, true)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        qsTile.updateTile()
    }


    private fun turnOffFlashLight() {
        val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId: String
        qsTile.state = Tile.STATE_INACTIVE
        isTorchOn = false
        try {
            cameraId = camManager.cameraIdList[0]
            camManager.setTorchMode(cameraId, false)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        qsTile.updateTile()
    }
}
