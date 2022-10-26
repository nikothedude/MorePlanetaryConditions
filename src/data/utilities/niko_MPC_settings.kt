package data.utilities

import com.fs.starfarer.api.Global
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
        SATELLITE_FLEET_FP_BONUS_INCREMENT = configJson.getInt("maxSatelliteFpBonus")
        SATELLITE_FLEET_FP_BONUS_MULT = configJson.getDouble("maxSatelliteFpBonusMult")

    }
    @JvmField
    var PREVENT_SATELLITE_TURN = false
    @JvmField
    var DEFENSE_SATELLITES_ENABLED = false
    @JvmField
    var SHOW_ERRORS_IN_GAME = false
    @JvmField
    var DISCOVER_SATELLITES_IN_BULK = false
    @JvmField
    var SATELLITE_INTERFERENCE_DISTANCE_BASE = 0f
    @JvmField
    var SATELLITE_INTERFERENCE_DISTANCE_MULT = 0f
    @JvmField
    var SATELLITE_FLEET_FP_BONUS_INCREMENT = 0
    @JvmField
    var SATELLITE_FLEET_FP_BONUS_MULT = 0.0
}