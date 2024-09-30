package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.thoughtworks.xstream.XStream
import data.compatability.MPC_compatabilityUtils
import data.scripts.campaign.MPC_People
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners.overgrownNanoforgeDiscoveryListener
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.terrain.hyperspace.niko_MPC_realspaceHyperspace
import data.scripts.campaign.econ.specialItems.overgrownNanoforgeItemEffect
import data.scripts.campaign.listeners.*
import data.scripts.campaign.magnetar.MPC_omegaCoreAdminChecker
import data.scripts.campaign.magnetar.niko_MPC_omegaWeaponPurger
import data.scripts.campaign.niko_MPC_specialProcGenHandler.doSpecialProcgen
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.scripts.campaign.terrain.niko_MPC_mesonField
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin
import data.scripts.everyFrames.niko_MPC_HTFactorTracker
import data.utilities.*
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids.mesonFieldGlobalMemoryId
import data.utilities.niko_MPC_ids.overgrownNanoforgeConditionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeItemId
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeIndustryId
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeJunkStructureId
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_memoryUtils.createNewSatelliteTracker
import data.utilities.niko_MPC_settings.AOTD_vaultsEnabled
import data.utilities.niko_MPC_settings.SOTF_enabled
import data.utilities.niko_MPC_settings.generatePredefinedSatellites
import data.utilities.niko_MPC_settings.loadAllSettings
import data.utilities.niko_MPC_settings.nexLoaded
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.MCTE_debugUtils
import org.apache.log4j.Level
import org.magiclib.kotlin.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.MutableSet
import kotlin.collections.any
import kotlin.collections.hashSetOf
import kotlin.collections.minusAssign
import kotlin.collections.set
import kotlin.collections.setOf
import kotlin.collections.toMutableSet

class niko_MPC_modPlugin : BaseModPlugin() {

    companion object {
        const val modId = "niko_morePlanetaryConditions"
        val conditionsNotAllowedInCoreWorlds: MutableSet<String> = hashSetOf(
            //overgrownNanoforgeConditionId, // these two have special handling
            //"niko_MPC_antiAsteroidSatellites_derelict",
            "niko_MPC_ultraMagneticField",
            "niko_MPC_hyperspaceBipartisan",
            "niko_MPC_ftcDistricts",
            "niko_MPC_spyArrays",
            "niko_MPC_derelictEscort"
        )
        var currVersion = Global.getSettings().modManager.getModSpec(modId).version
    }

    @Throws(RuntimeException::class)
    override fun onApplicationLoad() {
        val isLazyLibEnabled = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        niko_MPC_settings.MCTE_loaded = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        niko_MPC_settings.indEvoEnabled = Global.getSettings().modManager.isModEnabled("IndEvo")
        AOTD_vaultsEnabled = Global.getSettings().modManager.isModEnabled("aotd_vok")
        nexLoaded = Global.getSettings().modManager.isModEnabled("nexerelin")
        SOTF_enabled = Global.getSettings().modManager.isModEnabled("secretsofthefrontier")
        if (!isLazyLibEnabled) {
            throw RuntimeException("LazyLib is required for more planetary conditions!")
        }
        try {
            loadAllSettings()
        } catch (ex: Exception) {
            throw RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during application load! Exception: " + ex)
        }
        addSpecialItemsToItemRepo()
        StarSystemGenerator.addTerrainGenPlugin(niko_MPC_mesonFieldGenPlugin())

        // TODO
       /*throw java.lang.RuntimeException(
            "black dwarf: very rare and very very decrepit star, phase after white dwarf" +
            "\nplanetary nebulae, hostile environment with white dwarf/neutron star/blackhole that slowly overheats your fleet (is this fun?)" +
        )*/
    }

    val overgrownNanoforgeItemInstance = overgrownNanoforgeItemEffect(overgrownNanoforgeItemId, 0, 0)

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

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.OMAN_BOMBARD_COST_ID] = niko_MPC_settings.OMAN_BOMBARD_COST

        MPC_compatabilityUtils.run(currVersion)

        Global.getSector().addTransientListener(niko_MPC_pickFleetAIListener())
        Global.getSector().addTransientListener(niko_MPC_interationDialogShownListener())
        Global.getSector().listenerManager.addListener(overgrownNanoforgeOptionsProvider(), true)
        Global.getSector().addTransientListener(niko_MPC_satelliteEventListener(false))
        Global.getSector().listenerManager.addListener(overgrownNanoforgeDiscoveryListener(), true)
        Global.getSector().addTransientListener(niko_MPC_omegaWeaponPurger())
        //MPC_omegaCoreAdminChecker().start()

        if (niko_MPC_settings.DISCOVER_SATELLITES_IN_BULK) {
            Global.getSector().listenerManager.addListener(niko_MPC_satelliteDiscoveredListener(), true)
        }

        Global.getSector().registerPlugin(niko_MPC_campaignPlugin())

        val globalMemory = Global.getSector().memory
        if (!globalMemory.contains(niko_MPC_ids.satelliteBattleTrackerId)) {
            createNewSatelliteTracker()
        }

        val nanoforgeFaction = Global.getSector().getFaction(overgrownNanoforgeFleetFactionId)
        val constructionFaction = Global.getSector().getFaction(niko_MPC_ids.derelictOmegaConstructorFactionId)
        val omegaDerelictFaction = Global.getSector().getFaction(niko_MPC_ids.OMEGA_DERELICT_FACTION_ID)
        if (nanoforgeFaction == null) {
            displayError("null nanoforge faction SOMEHTING IS VERY VERY WRONG")
        } else {
            for (faction in Global.getSector().allFactions) {
                val id = faction.id
                if (id != Factions.DERELICT && id != overgrownNanoforgeFleetFactionId) {
                    nanoforgeFaction.setRelationship(id, RepLevel.HOSTILE)
                }
                if (id != Factions.REMNANTS && id != Factions.OMEGA && id != omegaDerelictFaction.id && id != constructionFaction.id) {
                    omegaDerelictFaction.setRelationship(id, RepLevel.VENGEFUL)
                }
            }

            nanoforgeFaction.removeKnownShip("guardian")
            nanoforgeFaction.removeKnownShip("station_derelict_survey_mothership")

            constructionFaction.removeKnownShip("guardian")
            constructionFaction.removeKnownShip("station_derelict_survey_mothership")
        }

        MPC_People.createCharacters() // safe to call multiple times

        for (listener in Global.getSector().listenerManager.getListeners(niko_MPC_saveListener::class.java)) {
            listener.onGameLoad()
        }

        LunaSettings.addSettingsListener(settingsChangedListener())
    }

    override fun beforeGameSave() {
        super.beforeGameSave()

        for (listener in Global.getSector().listenerManager.getListeners(niko_MPC_saveListener::class.java)) {
            listener.beforeGameSave()
        }
    }

    override fun afterGameSave() {
        super.afterGameSave()

        for (listener in Global.getSector().listenerManager.getListeners(niko_MPC_saveListener::class.java)) {
            listener.afterGameSave()
        }
    }

    override fun onGameSaveFailed() {
        super.onGameSaveFailed()

        for (listener in Global.getSector().listenerManager.getListeners(niko_MPC_saveListener::class.java)) {
            listener.onGameSaveFailed()
        }
    }

    override fun onEnabled(wasEnabledBefore: Boolean) {
        Global.getSector().addScript(niko_MPC_HTFactorTracker())

        super.onEnabled(wasEnabledBefore)
    }

    override fun configureXStream(x: XStream?) {
        if (x == null) return

        x.alias("niko_MPC_realspaceHyperspace", niko_MPC_realspaceHyperspace.javaClass)

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
        clearCoreWorldsOfInappropiateConditions()

        clearNanoforgesFromCoreWorlds()
        clearInappropiateOvergrownFleetSpawners()

        doSpecialProcgen(true)
    }

    private fun clearCoreWorldsOfInappropiateConditions() {
        val systems = Global.getSector().starSystems
        for (system in systems) {
            if (!system.hasTag(Tags.THEME_CORE)) continue
            for (planet in system.planets) {
                val foundMarket = planet.market ?: continue
                for (id in conditionsNotAllowedInCoreWorlds) {
                    if (foundMarket.hasCondition(id)) foundMarket.removeCondition(id)
                }
            }
        }
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

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            try {
                loadAllSettings()
            } catch (ex: Exception) {
                MCTE_debugUtils.displayError("settingsChangedListener exception caught, logging info", logType = Level.ERROR)
                MCTE_debugUtils.log.debug("info:", ex)
            }
        }
    }
}

class MilitaryBaseNoRouteSaviorListener: BaseCampaignEventListener(false) {
    override fun reportFleetDespawned(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (fleet == null || reason == null) return

        if (reason == CampaignEventListener.FleetDespawnReason.REACHED_DESTINATION) {
            var militaryBase: MilitaryBase? = null
            for (listener in fleet.eventListeners) {
                if (listener is MilitaryBase) {
                    militaryBase = listener
                    break
                }
            }
            if (militaryBase == null) return
            val route = RouteManager.getInstance().getRoute(militaryBase.routeSourceId, fleet)
            if (route == null) {
                displayError("found broken route for fleet, logging info and removing listener")
                niko_MPC_debugUtils.log.info("${fleet.name}, ${fleet.market?.name}")
                fleet.removeEventListener(militaryBase)
            }
        }
    }
}

class coronaResistStationCoreFleetListener: BaseFleetEventListener() {
    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (fleet == null) return

        val station = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION_GLOBAL] as? CustomCampaignEntityAPI ?: return
        station.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET] = null
    }
}