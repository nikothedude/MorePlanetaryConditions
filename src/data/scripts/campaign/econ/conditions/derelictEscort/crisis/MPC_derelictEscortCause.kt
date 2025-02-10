package data.scripts.campaign.econ.conditions.derelictEscort.crisis

import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityCause2
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.rulecmd.KantaCMD
import com.fs.starfarer.api.ui.MapParams
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

class MPC_derelictEscortCause(intel: HostileActivityEventIntel?) : BaseHostileActivityCause2(intel) {
    override fun getDesc(): String {
        val market = MPC_derelictEscortFactor.getMarketWithHighestFRCTime() ?: return "error lol"
        return "${market.name} escorts"
    }

    override fun getTooltip(): TooltipCreator? {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val market = MPC_derelictEscortFactor.getMarketWithHighestFRCTime() ?: return
                val opad = 10f
                tooltip.addPara(
                    "Escorts from %s are reinforcing supply chains sector-wide, causing major headaches for pirates.", 0f,
                    market.faction.baseUIColor, market.name
                )
                val params = MapParams()
                params.showSystem(market.starSystem)
                val w = tooltip.widthSoFar
                val h = (w / 1.6f).roundToInt().toFloat()
                params.positionToShowAllMarkersAndSystems(true, w.coerceAtMost(h))
                val map = tooltip.createSectorMap(w, h, params, market.starSystem.nameWithLowercaseTypeShort)
                tooltip.addCustom(map, opad)
            }
        }
    }


    override fun getProgress(): Int {
        return getProgress(true)
    }

    override fun getMagnitudeContribution(system: StarSystemAPI?): Float {
        return (getProgress(true) * 0.3f)
    }

    fun getProgress(checkKanta: Boolean): Int {
        if (!MPC_derelictEscortFactor.isActive()) return 0

        var progress = 10

        if (checkKanta && KantaCMD.playerHasProtection()) {
            progress = (progress * 0.1f).toInt()
        }
        return progress
    }
}