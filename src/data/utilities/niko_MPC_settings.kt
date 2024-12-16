package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import data.niko_MPC_modPlugin
import data.niko_MPC_modPlugin.Companion.modId
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_derelictSatelliteHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.randomizedSourceBudgets
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import dynamictariffs.util.SettingsUtil
import dynamictariffs.util.SettingsUtil.readSettings
import lunalib.lunaSettings.LunaSettings
import org.json.JSONException
import java.awt.Color
import java.io.IOException

object niko_MPC_settings {

    var MAGNETAR_DISABLED = false
    const val MAX_IAIIC_REP = -0.24f
    const val OMAN_BOMBARD_COST = 200
    const val DELAYED_REPAIR_TIME = 1f
    /** If true, fleets in the magnetar system can drop omega weapons. */
    var MAGNETAR_DROP_OMEGA_WEAPONS = false

    var DERELICT_ESCORT_SIMULATE_FLEETS = true

    var DERELICT_ESCORT_SPAWN_ON_PATROLS = true

    var nexLoaded: Boolean = false
    var MCTE_loaded: Boolean = false
    var indEvoEnabled = false
    var AOTD_vaultsEnabled = false
    var SOTF_enabled = false

    var CONDENSE_OVERGROWN_NANOFORGE_INTEL = false

    /** All spreading speed and countermeasures speed is multiplied against this. */
    var OVERGROWN_NANOFORGE_SPEED_MULT = 0.7f

    const val OVERGROWN_NANOFORGE_INDUSTRY_NAME = "Overgrown Nanoforge"
    const val OVERGROWN_NANOFORGE_JUNK_NAME = "Nanoforge Growth"

    const val OVERGROWN_NANOFORGE_NEGATIVE_EFFECT_BUDGET_MULT = 0.35f //being a structure is already a big punishment, isnt it?

    const val OVERGROWN_NANOFORGE_MIN_PREDEFINED_JUNK = 1
    const val OVERGROWN_NANOFORGE_MAX_PREDEFINED_JUNK = 4

    const val OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN = 1f
    const val OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX = 10f

    const val OVERGROWN_NANOFORGE_MIN_SCORE_ESTIMATION_VARIANCE = 0.2f
    const val OVERGROWN_NANOFORGE_MAX_SCORE_ESTIMATION_VARIANCE = 5f

    const val OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE = 5f
    const val OVERGROWN_NANOFORGE_SPREADING_PROGRESS = 50
    const val OVERGROWN_NANOFORGE_SPREADING_MAX_PROGRESS = 100

    const val OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION = 150f
    const val OVERGROWN_NANOFORGE_MINIMUM_GROWTH_MANIPULATION = -150f

    const val OVERGROWN_NANOFORGE_SUPPRESSION_DISCOUNT_THRESHOLD = 20f
    const val OVERGROWN_NANOFORGE_SUPPRESSION_DISCOUNT_MULT = 0.25f
    const val OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD = 100
    const val OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT = 30f
    const val OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT = 15f

    const val OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE = 630
    const val OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE = 800

    const val OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE = 150
    const val OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE = 250

    const val OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN = 4
    const val OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN = 10

    const val OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE_REGEN = 30
    const val OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE_REGEN = 30

    const val overgrownNanoforgeBaseJunkSpreadTargettingChance = 500f
    const val OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS = 500
    const val OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT = 0.0f
    const val OVERGROWN_NANOFORGE_INTEL_TOGGLE_VIEWMODE_ID = "toggleViewmode"

    val hyperMagFieldColors: MutableSet<Color> = hashSetOf(
        Color(144, 44, 152, 100),
        Color(50, 25, 100, 100),
        //Color(38, 155, 130, 100),
    )

    fun loadAllSettings() {
        loadSettings()
        loadNexSettings()
    }

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun loadSettings() {
        niko_MPC_debugUtils.log.info("MPC reloading settings")
        SHOW_ERRORS_IN_GAME = LunaSettings.getBoolean(modId,"MPC_showErrorsInGame")!!
        DEFENSE_SATELLITES_ENABLED = LunaSettings.getBoolean(modId, "MPC_enableDefenseSatellites")!!
        PREVENT_SATELLITE_TURN = LunaSettings.getBoolean(modId, "MPC_preventSatelliteTurning")!!
        DISCOVER_SATELLITES_IN_BULK = LunaSettings.getBoolean(modId, "MPC_discoverSatellitesInBulk")!!
        SATELLITE_INTERFERENCE_DISTANCE_BASE = LunaSettings.getFloat(modId, "MPC_satelliteInterferenceDistanceBase")!!
        SATELLITE_INTERFERENCE_DISTANCE_MULT = LunaSettings.getFloat(modId, "MPC_satelliteInterferenceDistanceMult")!!
        SATELLITE_FLEET_FP_BONUS_INCREMENT = LunaSettings.getInt(modId, "MPC_maxSatelliteFpBonus")!!
        SATELLITE_FLEET_FP_BONUS_MULT = LunaSettings.getFloat(modId, "MPC_maxSatelliteFpBonusMult")!!
        ATTACK_SAME_FACTION_IF_TOFF = LunaSettings.getBoolean(modId, "MPC_satellitesAttackFactionIfToff")!!

        MAX_STRUCTURES_ALLOWED = LunaSettings.getInt(modId, "MPC_maxStructuresForNanoforge")!!
        OVERGROWN_NANOFORGE_IS_INDUSTRY = LunaSettings.getBoolean(modId, "MPC_nanoforgeIsIndustry")!!
        OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN = LunaSettings.getFloat(modId, "MPC_nanoforgeBudgetMultMin")!!
        OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX = LunaSettings.getFloat(modId, "MPC_nanoforgeBudgetMultMax")!!
        OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS = LunaSettings.getInt(modId, "MPC_nanoforgeMinTimeBetweenSpreads")!!
        OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS = LunaSettings.getInt(modId, "MPC_nanoforgeMaxTimeBetweenSpreads")!!
        CONDENSE_OVERGROWN_NANOFORGE_INTEL = LunaSettings.getBoolean(modId, "MPC_condenseOvergrownNanoforgeIntel")!!
        OVERGROWN_NANOFORGE_SPEED_MULT = LunaSettings.getFloat(modId, "MPC_nanoforgeSpeedMult")!!

        DERELICT_ESCORT_SIMULATE_FLEETS = LunaSettings.getBoolean(modId, "MPC_derelictEscortSimulateFleets")!!
        DERELICT_ESCORT_SPAWN_ON_PATROLS = LunaSettings.getBoolean(modId, "MPC_derelictEscortSpawnOnPatrols")!!

        MAGNETAR_DROP_OMEGA_WEAPONS = LunaSettings.getBoolean(modId, "MPC_magnetarDropOmegaWeapons")!!
        MAGNETAR_DISABLED = LunaSettings.getBoolean(modId, "MPC_disableMagnetar")!!

        overgrownNanoforgeCommodityDataStore.reload()
    }

    fun loadNexSettings() {
        SATELLITE_NEX_ABILITY_BASE_TOTAL_DAMAGE = LunaSettings.getFloat(modId, "MPC_satelliteBarrageDamage")!!
        SATELLITE_NEX_ABILITY_DURATION = LunaSettings.getInt(modId, "MPC_satelliteBarrageDuration")!!
        SATELLITE_NEX_ABILITY_BASE_DISRUPTION_TIME = LunaSettings.getInt(modId, "MPC_satelliteBarrageDisruptTime")!!
    }

    @JvmStatic
    @Throws(JSONException::class, IOException::class)
    fun generatePredefinedSatellites() {
        niko_MPC_debugUtils.log.info("generating pre-defined satellites")
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

    var ATTACK_SAME_FACTION_IF_TOFF = false
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
    var SATELLITE_FLEET_FP_BONUS_MULT = 0f

    var USE_SATELLITE_INTERACTION_PLUGIN: Boolean = true

    var SATELLITE_NEX_ABILITY_BASE_TOTAL_DAMAGE = 2f // pretty low
    var SATELLITE_NEX_ABILITY_BASE_DISRUPTION_TIME = 9 // in days
    var SATELLITE_NEX_ABILITY_DURATION = 3 // 3 turns

    var OVERGROWN_NANOFORGE_IS_INDUSTRY = false

    var OVERGROWN_NANOFORGE_USE_JUNK_STRUCTURES = true

    var HARD_LIMIT_FOR_DEFENSE = 500f
    var ANCHOR_POINT_FOR_DEFENSE = 1200f

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
    var OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT = 1.4f

    var OVERGROWN_NANOFORGE_BASE_SCORE_MIN = randomizedSourceBudgets.HIGH.value
    var OVERGROWN_NANOFORGE_BASE_SCORE_MAX = randomizedSourceBudgets.EXTREMELY_HIGH.value

    var OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED: Boolean = false

    var MAX_STRUCTURES_ALLOWED: Int = 12
    /// MUST BE A CONST. NEEDS TO MATCH THE AMOUNT OF JUNK STRUCTURES WE HAVE DEFINED IN INDUSTRIES OR ELSE.
    const val MAX_JUNK_STRUCTURES_ALLOWED: Int = 12
}