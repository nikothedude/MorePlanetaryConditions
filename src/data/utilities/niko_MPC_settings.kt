package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_derelictSatelliteHandler
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.randomizedSourceBudgets
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

    var OVERGROWN_NANOFORGE_IS_INDUSTRY = true

    var OVERGROWN_NANOFORGE_CARES_ABOUT_PLAYER_PROXIMITY_FOR_DECON = true
    var OVERGROWN_NANOFORGE_INTERACTION_DISTANCE = 1000f //todo: adjust
    var OVERGROWN_NANOFORGE_USE_JUNK_STRUCTURES = true

    var OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS = 60f
    var OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS = 90f

    var HARD_LIMIT_FOR_DEFENSE = 500f
    var ANCHOR_POINT_FOR_DEFENSE = 2000f

    var OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES = 1

    var OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN = 0.8f
    var OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX = 1.2f

    // this is so completely inaccurate because the api methods are useless
    const val multToConvertFloatToDays = 100f
    var OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS = 120f * multToConvertFloatToDays
    var OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS = 200f * multToConvertFloatToDays

    var OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT = 3f
    var VOLATILE_EFFECT_INDUSTRIES_TO_DISRUPT = 5f
    var OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT = 0.0f
    var OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT = 1f

    var OVERGROWN_NANOFORGE_BASE_SCORE_MIN = randomizedSourceBudgets.HIGH.value*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT
    var OVERGROWN_NANOFORGE_BASE_SCORE_MAX = randomizedSourceBudgets.EXTREMELY_HIGH.value*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT
}