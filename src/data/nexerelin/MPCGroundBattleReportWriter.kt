package data.nexerelin

import com.fs.starfarer.api.ui.TooltipMakerAPI

fun interface MPCGroundBattleReportWriter {
    fun writeLog(tooltip: TooltipMakerAPI, battleReport: MPCGroundBattleReport)
}
