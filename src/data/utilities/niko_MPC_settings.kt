package data.utilities

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_derelictSatelliteHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.randomizedSourceBudgets
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import org.json.JSONException
import java.io.IOException

object niko_MPC_settings {

    const val OVERGROWN_NANOFORGE_INDUSTRY_NAME = "Overgrown Nanoforge"
    const val OVERGROWN_NANOFORGE_JUNK_NAME = "Nanoforge Growth"

    const val OVERGROWN_NANOFORGE_NEGATIVE_EFFECT_BUDGET_MULT = 0.35f //being a structure is already a big punishment, isnt it?

    const val OVERGROWN_NANOFORGE_MIN_PREDEFINED_JUNK = 1
    const val OVERGROWN_NANOFORGE_MAX_PREDEFINED_JUNK = 1

    const val OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN = 1f
    const val OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX = 10f

    const val OVERGROWN_NANOFORGE_MIN_SCORE_ESTIMATION_VARIANCE = 0.2f
    const val OVERGROWN_NANOFORGE_MAX_SCORE_ESTIMATION_VARIANCE = 5f

    const val OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE = 20f
    const val OVERGROWN_NANOFORGE_SPREADING_PROGRESS = 50

    const val OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION = 150f
    const val OVERGROWN_NANOFORGE_MINIMUM_GROWTH_MANIPULATION = -150f

    const val OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD = 100
    const val OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT = 30f
    const val OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT = 15f

    const val OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE = 400
    const val OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE = 600

    const val OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE = 100
    const val OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE = 200

    const val OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN = 1
    const val OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN = 5

    const val OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE_REGEN = 10
    const val OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE_REGEN = 10

    const val overgrownNanoforgeBaseJunkSpreadTargettingChance = 500f
    const val OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS = 500
    const val OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT = 0.1f
    const val OVERGROWN_NANOFORGE_INTEL_TOGGLE_VIEWMODE_ID = "toggleViewmode"

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

    var USE_SATELLITE_INTERACTION_PLUGIN: Boolean = true

    var OVERGROWN_NANOFORGE_IS_INDUSTRY = false

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
    const val multToConvertFloatToDays = 100
    var OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS: Int = 30
    var OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS: Int = 90

    var OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT = 9f
    var VOLATILE_EFFECT_INDUSTRIES_TO_DISRUPT = 5f
    var OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT = 0.0f
    var OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT = 1f

    var OVERGROWN_NANOFORGE_BASE_SCORE_MIN = randomizedSourceBudgets.HIGH.value*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT
    var OVERGROWN_NANOFORGE_BASE_SCORE_MAX = randomizedSourceBudgets.EXTREMELY_HIGH.value*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT

    var OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED: Boolean = false
}