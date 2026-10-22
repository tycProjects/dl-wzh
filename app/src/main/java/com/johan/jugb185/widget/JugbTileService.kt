package com.johan.jugb185.widget

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.johan.jugb185.core.ArmedStore
import com.johan.jugb185.core.VendorStub
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class JugbTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        scope.launch {
            val vendor = VendorStub().apply { init() }
            // Toggle 185 vs disarm for foreground app (simplified)
            tile.state = if (tile.state == Tile.STATE_ACTIVE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            tile.label = if (tile.state == Tile.STATE_ACTIVE) "JUGB 185 ON" else "JUGB OFF"
            tile.updateTile()
        }
    }
}
