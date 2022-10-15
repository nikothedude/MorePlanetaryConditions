package data

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.listeners.niko_MPC_satelliteDiscoveredListener
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_memoryUtils.createNewSatelliteTracker
import data.utilities.niko_MPC_satelliteUtils.allDefenseSatellitePlanets
import data.utilities.niko_MPC_satelliteUtils.purgeSatellitesFromEntity
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.loadSettings
import org.apache.log4j.Level
import org.json.JSONException
import java.io.IOException
import java.lang.Exception

class niko_MPC_modPlugin : BaseModPlugin() {
    @Throws(RuntimeException::class)
    override fun onApplicationLoad() {
        val isMagicLibEnabled = Global.getSettings().modManager.isModEnabled("MagicLib")
        if (!isMagicLibEnabled) {
            throw RuntimeException("MagicLib is required for more planetary conditions!")
        }
        val isLazyLibEnabled = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        if (!isLazyLibEnabled) {
            throw RuntimeException("LazyLib is required for more planetary conditions!")
        }
        try {
            loadSettings()
        } catch (ex: Exception) {
            throw RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during application load! Exception: " + ex)
        }

        if (niko_MPC_settings.DEBUG_MODE) {

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
        removeSatellitesFromMarkets(allDefenseSatellitePlanets, niko_MPC_settings.DEFENSE_SATELLITES_ENABLED)
    }

    fun removeSatellitesFromMarkets(toRemoveFrom: List<SectorEntityToken>?, removeOnlyFromCore: Boolean) {
        var toRemoveFrom = toRemoveFrom
        if (toRemoveFrom == null) {
            toRemoveFrom = allDefenseSatellitePlanets //by default: nuke it
        }
        for (entity in toRemoveFrom) {
            val containingLocation = entity.containingLocation
            if (!removeOnlyFromCore || containingLocation.tags.contains(Tags.THEME_CORE)) { //we dont want to spawn in core worlds
                val market = entity.market
                if (market != null) {
                    val conditionsCopy: List<MarketConditionAPI> = ArrayList(market.conditions)
                    for (condition in conditionsCopy) {
                        if (niko_MPC_ids.satelliteConditionIds.contains(condition.id)) {
                            market.removeCondition(condition.id)
                        }
                    }
                } else {
                    purgeSatellitesFromEntity(entity)
                }
            }
        }
    }

    companion object {
        private val log = Global.getLogger(niko_MPC_modPlugin::class.java)

        init {
            log.level = Level.ALL
        }
    }
}