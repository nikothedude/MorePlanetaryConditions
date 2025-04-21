package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc

open class MPC_hegemonyPatrolsDestroyedFactor(points: Int) : BaseOneTimeFactor(points) {

    companion object {
        fun isActive(): Boolean {
            return !Global.getSector().getFaction(Factions.HEGEMONY).relToPlayer.isHostile
        }
    }

    override fun getDesc(intel: BaseEventIntel?): String? {
        return "Military fleet ships destroyed"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    "Ships belonging to Hegemony warfleets or patrols destroyed by your fleet.",
                    0f
                )

                tooltip.addPara(
                    "This factor is only obtainable as long as you are not in %s with the Hegemony.",
                    5f,
                    Misc.getHighlightColor(),
                    "active hostilities"
                )
            }
        }
    }
}