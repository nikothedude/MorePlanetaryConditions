package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.thoughtworks.xstream.XStream
import data.campaign.ids.SKR_ids.THEME_PLAGUEBEARER
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners.overgrownNanoforgeDiscoveryListener
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.specialItems.overgrownNanoforgeItemEffect
import data.scripts.campaign.listeners.niko_MPC_interationDialogShownListener
import data.scripts.campaign.listeners.niko_MPC_pickFleetAIListener
import data.scripts.campaign.listeners.niko_MPC_satelliteDiscoveredListener
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.overgrownNanoforgeConditionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeItemId
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeIndustryId
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeJunkStructureId
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.purgeOvergrownNanoforgeBuildings
import data.utilities.niko_MPC_marketUtils.removeOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_memoryUtils.createNewSatelliteTracker
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.generatePredefinedSatellites
import data.utilities.niko_MPC_settings.loadSettings

class niko_MPC_modPlugin : BaseModPlugin() {
    @Throws(RuntimeException::class)
    override fun onApplicationLoad() {
        val isLazyLibEnabled = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        if (!isLazyLibEnabled) {
            throw RuntimeException("LazyLib is required for more planetary conditions!")
        }
        try {
            loadSettings()
        } catch (ex: Exception) {
            throw RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during application load! Exception: " + ex)
        }

        addSpecialItemsToItemRepo()
    }

    val overgrownNanoforgeItemInstance = overgrownNanoforgeItemEffect(niko_MPC_ids.overgrownNanoforgeItemId, 0, 0)

    private fun addSpecialItemsToItemRepo() {

        //add special items
        ItemEffectsRepo.ITEM_EFFECTS[overgrownNanoforgeItemId] = overgrownNanoforgeItemInstance
        val spec = Global.getSettings().getSpecialItemSpec(overgrownNanoforgeItemId) ?: return
        val strictBlacklist = setOf(overgrownNanoforgeIndustryId)
        val looseBlacklist = setOf(overgrownNanoforgeJunkStructureId)
        for (industry in Global.getSettings().allIndustrySpecs) {
            val id = industry.id
            if (id in spec.params) continue

            if (strictBlacklist.contains(id)) continue
            if (looseBlacklist.any { id.contains(it) }) continue

            if (spec.params.isNotEmpty()) spec.params += ", "
            spec.params += id
        }
    }

    /* @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.getHullSpec().hasTag("niko_MPC_isSatelliteHullId")) {

            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
    } */

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        Global.getSector().addTransientListener(niko_MPC_pickFleetAIListener())
        Global.getSector().addTransientListener(niko_MPC_interationDialogShownListener())
        Global.getSector().listenerManager.addListener(overgrownNanoforgeOptionsProvider(), true)
        Global.getSector().addTransientListener(niko_MPC_satelliteEventListener(false))
        Global.getSector().listenerManager.addListener(overgrownNanoforgeDiscoveryListener(), true)

        if (niko_MPC_settings.DISCOVER_SATELLITES_IN_BULK) {
            Global.getSector().listenerManager.addListener(niko_MPC_satelliteDiscoveredListener(), true)
        }

        Global.getSector().registerPlugin(niko_MPC_campaignPlugin())

        val globalMemory = Global.getSector().memory
        if (!globalMemory.contains(niko_MPC_ids.satelliteBattleTrackerId)) {
            createNewSatelliteTracker()
        }

        val nanoforgeFaction = Global.getSector().getFaction(overgrownNanoforgeFleetFactionId)
        if (nanoforgeFaction == null) {
            displayError("null nanoforge faction SOMEHTING IS VERY VERY WRONG")
        } else {
            for (faction in Global.getSector().allFactions) {
                val id = faction.id
                if (id == Factions.DERELICT || id == overgrownNanoforgeFleetFactionId) continue
                nanoforgeFaction.setRelationship(id, RepLevel.HOSTILE)
            }

            val knownShips = nanoforgeFaction.knownShips
            knownShips -= "guardian" //no super special ship
            knownShips -= "station_derelict_survey_mothership"
            nanoforgeFaction.clearShipRoleCache()
        }

        /*val list = Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as? HashSet<overgrownNanoforgeIndustryHandler>
        if (list != null) {
            for (entry in list.toMutableSet()) {
                entry.delete()
            }
        }

        for (system in Global.getSector().starSystems) {
            for (planet in system.planets) {
                val market = planet.market ?: continue
                market.purgeOvergrownNanoforgeBuildings()
                market.removeOvergrownNanoforgeIndustryHandler()
                for (i in 0..12) {
                    market.memoryWithoutUpdate.unset(overgrownNanoforgeJunkStructureId + i)
                }
            }
        }*/
    }

    override fun onEnabled(wasEnabledBefore: Boolean) {
        //MPC_conditionManager.generateConditions(wasEnabledBefore)

        super.onEnabled(wasEnabledBefore)
    }

    override fun configureXStream(x: XStream?) {
        if (x == null) return

        // TODO: aliases

        super.configureXStream(x)
    }

    override fun onNewGameAfterEconomyLoad() {
        super.onNewGameAfterEconomyLoad()
        if (!niko_MPC_settings.DEFENSE_SATELLITES_ENABLED) {
            niko_MPC_satelliteUtils.obliterateSatellites()
        } else {
            clearSatellitesFromCoreWorlds()

            generatePredefinedSatellites()
        }
        clearNanoforgesFromCoreWorlds()
        clearInappropiateOvergrownFleetSpawners()
    }

    private fun clearInappropiateOvergrownFleetSpawners() {
        val scripts = (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.overgrownNanoforgeFleetScriptListMemoryId] as? MutableSet<overgrownNanoforgeSpawnFleetScript>)?.toMutableSet() ?: return
        for (script in scripts) {
            val system = script.getSystem() ?: continue
            for (tag in overgrownNanoforgeEffectPrototypes.blacklistedFleetSpawnerSystemTags) {
                if (system.hasTag(tag)) {
                    val market = script.getMarket()
                    val handler = script.effect.handler
                    val coreHandler = handler.getCoreHandler()
                    handler.delete()
                    if (coreHandler.deleted) continue
                    val newHandler = overgrownNanoforgeJunkHandler(
                        market,
                        coreHandler,
                        market.getNextOvergrownJunkDesignation(),
                        false
                    )
                    newHandler.init()
                    continue
                }
            }
        }
    }

    private fun clearNanoforgesFromCoreWorlds() {
        val systems = Global.getSector().starSystems
        for (system in systems) {
            if (!system.hasTag(Tags.THEME_CORE)) continue
            for (planet in system.planets) {
                val foundMarket = planet.market ?: continue
                if (foundMarket.hasCondition(overgrownNanoforgeConditionId)) {
                    foundMarket.removeCondition(overgrownNanoforgeConditionId)
                }
            }
        }
    }

    fun clearSatellitesFromCoreWorlds() {
        for (handler: niko_MPC_satelliteHandlerCore in ArrayList(niko_MPC_satelliteUtils.getAllSatelliteHandlers())) {
            if (handler.allowedInLocationWithTag(Tags.THEME_CORE)) continue
            val location: LocationAPI = handler.getLocation() ?: continue
            if (location.hasTag(Tags.THEME_CORE)) handler.delete()
        }
    }
}