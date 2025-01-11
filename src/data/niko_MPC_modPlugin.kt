package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin.InstallableItemDescriptionMode
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import com.fs.starfarer.api.combat.MissileAIPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import data.compatability.MPC_compatabilityUtils
import data.kaysaar.aotd.vok.Ids.AoTDTechIds
import data.kaysaar.aotd.vok.scripts.research.AoTDMainResearchManager
import data.scripts.campaign.MPC_People
import data.scripts.campaign.MPC_hostileActivityHook
import data.scripts.campaign.econ.MPC_incomeTallyListener
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners.overgrownNanoforgeDiscoveryListener
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.terrain.hyperspace.niko_MPC_realspaceHyperspace
import data.scripts.campaign.econ.specialItems.overgrownNanoforgeItemEffect
import data.scripts.campaign.listeners.*
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobInvasionListener
import data.scripts.campaign.magnetar.crisis.intel.MPC_luddicContributionIntel.Companion.SECT_NAME
import data.scripts.campaign.magnetar.niko_MPC_omegaWeaponPurger
import data.scripts.campaign.niko_MPC_specialProcGenHandler.doSpecialProcgen
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.scripts.campaign.rulecmd.MPC_IAIICTriTachCMD.Companion.DOWN_PAYMENT
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin
import data.scripts.everyFrames.niko_MPC_HTFactorTracker
import data.utilities.*
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids.overgrownNanoforgeConditionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeItemId
import data.utilities.niko_MPC_ids.specialSyncrotronItemId
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeIndustryId
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeJunkStructureId
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_memoryUtils.createNewSatelliteTracker
import data.utilities.niko_MPC_settings.AOTD_vaultsEnabled
import data.utilities.niko_MPC_settings.SOTF_enabled
import data.utilities.niko_MPC_settings.astralAscensionEnabled
import data.utilities.niko_MPC_settings.generatePredefinedSatellites
import data.utilities.niko_MPC_settings.graphicsLibEnabled
import data.utilities.niko_MPC_settings.loadAllSettings
import data.utilities.niko_MPC_settings.nexLoaded
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.MCTE_debugUtils
import org.apache.log4j.Level
import org.dark.shaders.light.LightData
import org.dark.shaders.util.ShaderLib
import org.magiclib.kotlin.*
import kotlin.collections.set

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
        val facsIAIICKnowsShipsFrom = hashSetOf(Factions.HEGEMONY, Factions.TRITACHYON, Factions.DIKTAT, Factions.LIONS_GUARD, Factions.INDEPENDENT, Factions.LUDDIC_CHURCH)

        fun setupIAIICBlueprints() {
            val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return
            for (factionId in facsIAIICKnowsShipsFrom) {
                val faction = Global.getSector().getFaction(factionId)

                for (ship in faction.knownShips) {
                    IAIIC.knownShips += ship
                    val hullFreq = faction.hullFrequency[ship]
                    if (hullFreq != null) {
                        IAIIC.hullFrequency[ship] = hullFreq
                    }
                }

                IAIIC.knownFighters.addAll(faction.knownFighters)
                IAIIC.knownHullMods.addAll(faction.knownHullMods)
                IAIIC.knownWeapons.addAll(faction.knownWeapons)
                IAIIC.knownIndustries.addAll(faction.knownIndustries)
            }
            IAIIC.clearShipRoleCache()

            val intel = MPC_IAIICFobIntel.get() ?: return
            intel.removeBlueprintFunctions.forEach { it() }
        }
    }

    private fun setupIAIICResearch() {
        if (!AOTD_vaultsEnabled) return
        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return
        val manager = AoTDMainResearchManager.getInstance()
        val facManager = manager.getSpecificFactionManager(IAIIC) ?: return
        facManager.getResearchOptionFromRepo(AoTDTechIds.HAZMAT_WORKING_EQUIPMENT)?.isResearched = true
    }


    @Throws(RuntimeException::class)
    override fun onApplicationLoad() {
        val isLazyLibEnabled = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        niko_MPC_settings.MCTE_loaded = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        niko_MPC_settings.indEvoEnabled = Global.getSettings().modManager.isModEnabled("IndEvo")
        AOTD_vaultsEnabled = Global.getSettings().modManager.isModEnabled("aotd_vok")
        nexLoaded = Global.getSettings().modManager.isModEnabled("nexerelin")
        SOTF_enabled = Global.getSettings().modManager.isModEnabled("secretsofthefrontier")
        graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
        astralAscensionEnabled = Global.getSettings().modManager.isModEnabled("Planetace_AstralAscension")
        if (!isLazyLibEnabled) {
            throw RuntimeException("LazyLib is required for more planetary conditions!")
        }
        try {
            loadAllSettings()
        } catch (ex: Exception) {
            throw RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during application load! Exception: " + ex)
        }
        if (graphicsLibEnabled) {
            ShaderLib.init()
            LightData.readLightDataCSV("data/lights/MPC_light_data.csv")
        }
        addSpecialItemsToItemRepo()
        StarSystemGenerator.addTerrainGenPlugin(niko_MPC_mesonFieldGenPlugin())

        Global.getSettings().loadTexture("graphics/portraits/MPC_fractalCore.png")
        Global.getSettings().getShipSystemSpec("MPC_deepDive").isCanUseWhileRightClickSystemOn = true
        // afaik, theres no flag for this you can set via json, has to be done through code

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
        ItemEffectsRepo.ITEM_EFFECTS[specialSyncrotronItemId] = object : BoostIndustryInstallableItemEffect(
            specialSyncrotronItemId, niko_MPC_settings.SPECIAL_SYNCROTRON_FUEL_BOOST, 2
        ) {
            override fun addItemDescriptionImpl(
                industry: Industry?, text: TooltipMakerAPI, data: SpecialItemData,
                mode: InstallableItemDescriptionMode, pre: String, pad: Float
            ) {
                //text.addPara(pre + "Increases fuel production and demand for volatiles by %s.",
                text.addPara(
                    pre + "Increases fuel production output by %s units. Increases demand by %s units.",
                    pad, Misc.getHighlightColor(), "" + niko_MPC_settings.SPECIAL_SYNCROTRON_FUEL_BOOST, "${2}"
                )
            }

            override fun getSimpleReqs(industry: Industry?): Array<String> {
                return arrayOf(ItemEffectsRepo.NO_ATMOSPHERE)
            }
        }
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

    override fun pickMissileAI(missile: MissileAPI?, launchingShip: ShipAPI?): PluginPick<MissileAIPlugin>? {
        if (missile == null) return null

        /*when (missile.projectileSpecId) {
            "MPC_steelpeckerStageOne" -> return PluginPick(MPC_interceptorMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "MPC_steelpeckerStageTwo" -> return PluginPick(MPC_interceptorMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
        }*/

        return null
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
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DELAYED_REPAIR_TIME_ID] = niko_MPC_settings.DELAYED_REPAIR_TIME
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PATHER_SECT_NAME] = SECT_NAME
        Global.getSector().memoryWithoutUpdate["\$MPC_arrayScanTime"] = niko_MPC_settings.ARRAY_SCAN_TIME // days

        Global.getSector().memoryWithoutUpdate["\$MPC_downPaymentAmount"] = DOWN_PAYMENT
        Global.getSector().memoryWithoutUpdate["\$MPC_downPaymentDGS"] = Misc.getDGSCredits(DOWN_PAYMENT.toFloat())
        Global.getSector().memoryWithoutUpdate["\$MPC_deliveryPirateName"] = MPC_People.getImportantPeople()[MPC_People.DONN_PIRATE]?.name?.fullName
        Global.getSector().memoryWithoutUpdate["\$MPC_disarmamentFleetSizeMult"] = MPC_IAIICFobIntel.DISARMAMENT_FLEET_SIZE_MULT

        MPC_compatabilityUtils.run(currVersion)

        Global.getSector().addTransientListener(niko_MPC_pickFleetAIListener())
        Global.getSector().addTransientListener(niko_MPC_interationDialogShownListener())
        Global.getSector().listenerManager.addListener(overgrownNanoforgeOptionsProvider(), true)
        Global.getSector().addTransientListener(niko_MPC_satelliteEventListener(false))
        Global.getSector().listenerManager.addListener(overgrownNanoforgeDiscoveryListener(), true)
        Global.getSector().addTransientListener(niko_MPC_omegaWeaponPurger())
        Global.getSector().addTransientListener(MPC_incomeTallyListener())
        //Global.getSector().addTransientListener(niko_MPC_spyFleetBattleListener())
        MPC_hostileActivityHook().start()
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
        setupIAIICBlueprints()
        setupIAIICResearch()

        for (listener in Global.getSector().listenerManager.getListeners(niko_MPC_saveListener::class.java)) {
            listener.onGameLoad()
        }

        MPC_IAIICFobInvasionListener.get(true)
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

        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
        IAIIC.setRelationship(Factions.HEGEMONY, RepLevel.FAVORABLE)
        IAIIC.setRelationship(Factions.LUDDIC_CHURCH, RepLevel.WELCOMING)
        IAIIC.setRelationship(Factions.INDEPENDENT, RepLevel.FAVORABLE)
        IAIIC.setRelationship(Factions.DIKTAT, -0.05f)
        IAIIC.setRelationship(Factions.PIRATES, -0.5f)
        IAIIC.setRelationship(Factions.REMNANTS, -0.5f)
        IAIIC.setRelationship(Factions.LUDDIC_PATH, -0.5f) // only officially
        IAIIC.setRelationship(Factions.PLAYER, -0.2f)

        for (faction in Global.getSector().allFactions.filter { Global.getSector().getFaction(Factions.HEGEMONY).isHostileTo(it) }) {
            IAIIC.setRelationship(faction.id, Global.getSector().getFaction(Factions.HEGEMONY).getRelationshipLevel(faction))
        }
        IAIIC.setRelationship(Factions.PERSEAN, RepLevel.SUSPICIOUS)

        setupIAIICBlueprints()

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