package data.utilities

import com.fs.starfarer.api.Global
import org.apache.log4j.Level
import org.json.JSONException
import java.io.IOException

object niko_MPC_settings {

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        niko_MPC_debugUtils.log.debug("reloading settings")
        val configJson = Global.getSettings().loadJSON(niko_MPC_ids.niko_MPC_masterConfig)
        DEFENSE_SATELLITES_ENABLED = configJson.getBoolean("enableDefenseSatellites")
        SHOW_ERRORS_IN_GAME = configJson.getBoolean("showErrorsInGame")
        PREVENT_SATELLITE_TURN = configJson.getBoolean("preventSatelliteTurning")
        DISCOVER_SATELLITES_IN_BULK = configJson.getBoolean("discoverSatellitesInBulk")
        SATELLITE_INTERFERENCE_DISTANCE_BASE = configJson.getDouble("satelliteInterferenceDistanceBase").toFloat()
        SATELLITE_INTERFERENCE_DISTANCE_MULT = configJson.getDouble("satelliteInterferenceDistanceMult").toFloat()
        BATTLE_SATELLITES_BASE = configJson.getInt("maxBattleSatellitesBase")
        BATTLE_SATELLITES_MULT = configJson.getDouble("maxBattleSatellitesMult")
        BARRAGE_WEIGHT = configJson.getDouble("barrage_weight").toFloat()
        STANDARD_WEIGHT = configJson.getDouble("standard_weight").toFloat()
        SHIELDED_WEIGHT = configJson.getDouble("shielded_weight").toFloat()
        BEAMER_WEIGHT = configJson.getDouble("beamer_weight").toFloat()
        ORDNANCE_WEIGHT = configJson.getDouble("ordnance_weight").toFloat()
        SWARM_WEIGHT = configJson.getDouble("swarm_weight").toFloat()
    }

    fun isIdEnabled(enabledSetting: String?): Boolean {

    }

    var XENOLIFE_ENABLED = false

    var XENO_RANCHING_IND_ENABLED = false
    var XENO_HUNTING_IND_ENABLED = false
    @JvmField
    var PREVENT_SATELLITE_TURN = false
    @JvmField
    var DEFENSE_SATELLITES_ENABLED = false
    var SHOW_ERRORS_IN_GAME = false
    @JvmField
    var DISCOVER_SATELLITES_IN_BULK = false
    @JvmField
    var SATELLITE_INTERFERENCE_DISTANCE_BASE = 0f
    @JvmField
    var SATELLITE_INTERFERENCE_DISTANCE_MULT = 0f
    var BATTLE_SATELLITES_BASE = 0
    var BATTLE_SATELLITES_MULT = 0.0
    @JvmField
    var BARRAGE_WEIGHT: Float? = null
    @JvmField
    var STANDARD_WEIGHT: Float? = null
    @JvmField
    var SHIELDED_WEIGHT: Float? = null
    @JvmField
    var BEAMER_WEIGHT: Float? = null
    @JvmField
    var ORDNANCE_WEIGHT: Float? = null
    @JvmField
    var SWARM_WEIGHT: Float? = null
}