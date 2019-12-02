package diorid.tiles

import android.annotation.SuppressLint
import android.provider.Settings

const val excludeSystemUI = ",-com.android.systemui"

fun ImmersiveTileService.immersiveModeReset() {
    Settings.Global.putString(contentResolver, "policy_control", "immersive.none=*")
    setOverscan(0, 0, 0, 0, 0)
}

fun ImmersiveTileService.immersiveModeFull(exclude: Boolean, fullHide: Boolean) {
    if (fullHide) {
        val navHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        val navHeight = resources.getDimensionPixelSize(navHeightId)
        setOverscan(0, 0, 0, 0, -navHeight)
        Settings.Global.putString(contentResolver, "policy_control", "immersive.status=*${if (exclude) excludeSystemUI else ""}")
    } else {
        setOverscan(0, 0, 0, 0, 0)
        Settings.Global.putString(contentResolver, "policy_control", "immersive.full=*${if (exclude) excludeSystemUI else ""}")
    }

}

@SuppressLint("PrivateApi")
fun setOverscan(display: Int, left: Int, top: Int, right: Int, bottom: Int) {
    val service = Class.forName("android.view.WindowManagerGlobal")
            .getMethod("getWindowManagerService")
            .invoke(null)

    Class.forName("android.view.IWindowManager")
            .getMethod("setOverscan", Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
            .invoke(service, display, left, top, right, bottom)
}
