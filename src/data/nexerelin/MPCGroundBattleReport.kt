package data.nexerelin

import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.utilities.niko_MPC_ids
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundBattleLog

class MPCGroundBattleReport(intel: GroundBattleIntel, type: String = "MPCLog", turn: Int = intel.turnNum): GroundBattleLog(intel, type, turn) {
    override fun writeLog(tooltip: TooltipMakerAPI?) {

        if (tooltip == null) return

        val plugin = params[niko_MPC_ids.NEX_GROUND_REPORT_PLUGIN_ID] as MPCGroundBattleReportWriter
        plugin.writeLog(tooltip, this)
    }
}