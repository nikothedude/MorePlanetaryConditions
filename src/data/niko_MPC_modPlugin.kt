package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.listeners.niko_MPC_satelliteDiscoveredListener
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.utilities.niko_MPC_ids
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
    }

    /* @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.getHullSpec().hasTag("niko_MPC_isSatelliteHullId")) {

            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, config), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
    } */

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)
        Global.getSector().addTransientListener(niko_MPC_satelliteEventListener(false))
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
    }

    fun clearSatellitesFromCoreWorlds() {
        for (handler: niko_MPC_satelliteHandlerCore in ArrayList(niko_MPC_satelliteUtils.getAllSatelliteHandlers())) {
            if (handler.allowedInLocationWithTag(Tags.THEME_CORE)) continue
            val location: LocationAPI = handler.getLocation() ?: continue
            if (location.hasTag(Tags.THEME_CORE)) handler.delete()
        }
    }
}