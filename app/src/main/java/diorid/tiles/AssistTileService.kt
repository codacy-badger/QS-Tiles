package diorid.tiles

import android.app.Activity
import android.app.SearchManager
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

import java.lang.reflect.InvocationTargetException

class AssistTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val searchManager = baseContext.getSystemService(Activity.SEARCH_SERVICE) as SearchManager
        try {
            SearchManager::class.java.getMethod("launchAssist", Bundle::class.java)
                .invoke(searchManager, Bundle())
        } catch (e: IllegalAccessException) {
            Toast.makeText(this, "You haven't got any assist app!", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            Toast.makeText(this, "You haven't got any assist app!", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            Toast.makeText(this, "You haven't got any assist app!", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}
