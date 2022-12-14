package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_derelictSatelliteHandler
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import org.json.JSONException
import java.io.IOException

object niko_MPC_settings {

    const val overgrownNanoforgeBaseJunkSpreadTargettingChance = 500f


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

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun generatePredefinedSatellites() {
        niko_MPC_debugUtils.log.debug("generating pre-defined satellites")
        val configJson = Global.getSettings().loadJSON(niko_MPC_ids.niko_MPC_masterConfig)
        val objectOfEntityToHandler = configJson.getJSONObject("entitiesToAddSatellitesTo")
        val iterator = objectOfEntityToHandler.keys()
        while (iterator.hasNext()) {
            val key = iterator.next() as String
            val data = objectOfEntityToHandler.getString(key)

            val entity = Global.getSector().getEntityById(key)
            if (entity == null) {
                niko_MPC_debugUtils.log.warn("could not find $key, continuing")
                continue
            }
            when (data) {
                "DERELICT" -> {
                    if (entity.market != null) {
                        entity.market.addCondition("niko_MPC_antiAsteroidSatellites_derelict")
                    } else {
                        niko_MPC_derelictSatelliteHandler.createNewHandlerInstance(entity)
                    }
                } else -> {
                    niko_MPC_debugUtils.log.warn("invalid handler type in settings for $key, skipping")
                    return
                }
            }
            val market = (entity.market)
            if (market != null) {
                if (!market.isHidden && !market.isPlanetConditionMarketOnly) {
                    val handlers = entity.getSatelliteHandlers()
                    for (handler in handlers) {
                        for (satellite in handler.cosmeticSatellites) {
                            satellite.setSensorProfile(null)
                            satellite.isDiscoverable = false
                        }
                    }
                }
            }
        }
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