package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.listeners.TestIndustryOptionProvider
import com.fs.starfarer.api.impl.campaign.econ.impl.ItemEffectsRepo
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners.overgrownNanoforgeDiscoveryListener
import data.scripts.campaign.econ.specialItems.overgrownNanoforgeItemEffect
import data.scripts.campaign.listeners.niko_MPC_interationDialogShownListener
import data.scripts.campaign.listeners.niko_MPC_pickFleetAIListener
import data.scripts.campaign.listeners.niko_MPC_satelliteDiscoveredListener
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.overgrownNanoforgeConditionId
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
        ItemEffectsRepo.ITEM_EFFECTS[niko_MPC_ids.overgrownNanoforgeItemId] = overgrownNanoforgeItemInstance

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