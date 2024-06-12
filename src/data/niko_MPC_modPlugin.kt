package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.ids.Entities.STATION_RESEARCH_REMNANT
import com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_NO_JUMP
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.random
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.campaign.CharacterStats.SkillLevel
import com.thoughtworks.xstream.XStream
import data.scripts.campaign.MPC_coronaResistFleetManagerScript
import data.scripts.campaign.MPC_coronaResistScript
import data.scripts.campaign.MPC_coronaResistStationScript
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners.overgrownNanoforgeDiscoveryListener
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.terrain.hyperspace.niko_MPC_realspaceHyperspace
import data.scripts.campaign.terrain.niko_MPC_mesonField
import data.scripts.campaign.econ.specialItems.overgrownNanoforgeItemEffect
import data.scripts.campaign.listeners.*
import data.scripts.campaign.niko_MPC_specialProcGenHandler.doSpecialProcgen
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
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
import data.utilities.niko_MPC_settings.generatePredefinedSatellites
import data.utilities.niko_MPC_settings.loadSettings
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin
import data.scripts.everyFrames.niko_MPC_HTFactorTracker
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils.getApproximateOrbitDays
import data.utilities.niko_MPC_reflectionUtils.get
import data.utilities.niko_MPC_settings.loadAllSettings
import data.utilities.niko_MPC_settings.loadNexSettings
import data.utilities.niko_MPC_settings.nexLoaded
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.niko_MCTE_modPlugin
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_debugUtils
import org.apache.log4j.Level
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.*

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
        )
    }

    @Throws(RuntimeException::class)
    override fun onApplicationLoad() {
        val isLazyLibEnabled = Global.getSettings().modManager.isModEnabled("lw_lazylib")
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
            "IDEAS FOR NEXT CONDITIONS: +accessability for all markets in-system EXCEPT this one? \n\
            use unused aurorae2 t exture in graphics/planets to make a tachyon field that makes you go fast around stars? \n\
            investigaet dark nebula in graphics/fx. good terrain sprite"
        )*/

        niko_MPC_settings.MCTE_loaded = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        niko_MPC_settings.indEvoEnabled = Global.getSettings().modManager.isModEnabled("IndEvo")
        nexLoaded = Global.getSettings().modManager.isModEnabled("nexerelin")
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

    fun generateMesonFields(checkExisting: Boolean = false) { // doing this since i want to start moving away from natural procgen
      /*  if (checkExisting && getMesonFields().isNotEmpty()) return

        for (system in Global.getSector().starSystems) {
            if (!system.isProcgen) return
            for (planet in system.planets) {
                if (planet.isStar)
            }
        }*/
    }

    private fun getMesonFields(): MutableSet<niko_MPC_mesonField> {
        val memory = Global.getSector().memoryWithoutUpdate
        if (memory[mesonFieldGlobalMemoryId] !is MutableSet<*>) memory[mesonFieldGlobalMemoryId] = HashSet<niko_MPC_mesonField>()
        return memory[mesonFieldGlobalMemoryId] as MutableSet<niko_MPC_mesonField>
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