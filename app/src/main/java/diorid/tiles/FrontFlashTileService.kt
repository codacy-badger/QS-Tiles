package diorid.tiles

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class FrontFlashTileService : TileService() {

    private lateinit var characteristics: CameraCharacteristics
    private var isTorchOn: Boolean = false

    override fun onClick() {
        super.onClick()

        qsTile.updateTile()

        if (isTorchOn) {
            turnOffFlashLight()
        } else {
            turnOnFlashLight()
        }

    }

    private fun turnOnFlashLight() {
        if (checkFrontFlash()) {
            val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId: String
            qsTile.state = Tile.STATE_ACTIVE
            isTorchOn = true
            try {
                cameraId = camManager.cameraIdList[1]
                camManager.setTorchMode(cameraId, true)
            } catch (e: CameraAccessException) {

                e.printStackTrace()
            }

            qsTile.updateTile()
        } else {
            qsTile.state = Tile.STATE_UNAVAILABLE
            isTorchOn = false
            Toast.makeText(this, "Your device doesn't support front flash.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun turnOffFlashLight() {
        val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId: String
        qsTile.state = Tile.STATE_INACTIVE
        isTorchOn = false
        try {
            cameraId = camManager.cameraIdList[1]
            camManager.setTorchMode(cameraId, false)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        qsTile.updateTile()
    }

    private fun checkFrontFlash(): Boolean {
        val camManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            characteristics = camManager.getCameraCharacteristics("1")
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        return characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!
    }
}
