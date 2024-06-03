package data.scripts.campaign.econ.conditions.defenseSatellite

import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.plugins.MarketConditionPlugin

class niko_MPC_satellitesNexPlugin: MarketConditionPlugin() {
    override fun init(intel: GroundBattleIntel?) {
        if (intel == null) return

        val handlers = intel.market?.getSatelliteHandlers() ?: return


        super.init(intel)
    }

    override fun advance(days: Float) {
        super.advance(days)

        val handlers = intel.market?.getSatelliteHandlers() ?: return
    }
}