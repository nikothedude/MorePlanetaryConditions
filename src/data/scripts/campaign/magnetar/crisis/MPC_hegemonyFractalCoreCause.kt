package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.impl.campaign.intel.events.*
import com.fs.starfarer.api.ui.MapParams
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor.Companion.getContributingFactions
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import lunalib.lunaExtensions.getMarketsCopy
import kotlin.math.roundToInt

abstract class MPC_hegemonyFractalCoreCause(intel: HostileActivityEventIntel?) : BaseHostileActivityCause2(intel) {

    companion object {
        fun getFractalColony(): MarketAPI? {
            return Global.getSector().playerFaction.getMarketsCopy().firstOrNull { it.isFractalMarket() }
        }
    }

    open var preDefeat = false

    override fun getDesc(): String {
        return "Exotic intelligence use"
    }

    override fun getProgress(): Int {
        return getProgress(true)
    }

    override fun getMagnitudeContribution(system: StarSystemAPI?): Float {
        return (getProgress(true) * 0.3f)
    }

    fun getProgress(checkNegated: Boolean): Int {
        if (!MPC_fractalCoreFactor.isActive()) return 0

        if (preDefeat) {
            if (HegemonyHostileActivityFactor.isPlayerDefeatedHegemony()) return 0
        } else if (!HegemonyHostileActivityFactor.isPlayerDefeatedHegemony()) {
            return 0
        }
        var progress = 25

        if (checkNegated && isNegatedByPLMembership()) {
            progress = (progress * 0.3f).toInt()
        }
        return progress
    }

    override fun getTooltip(): TooltipMakerAPI.TooltipCreator {
        val fractalColony = getFractalColony()
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val opad = 10f
                tooltip.addPara(
                    "Your use of %s on %s has attracted the attention of many different polities, " +
                            "with the hegemony being the largest.", 0f, Misc.getHighlightColor(),
                    "exotic intelligences", "${fractalColony?.name}"
                )
                if (HegemonyHostileActivityFactor.isPlayerDefeatedHegemony()) {
                    tooltip.addPara(
                        "While significantly more covert as a result of the hegemony's previous defeat, " +
                                "this still represents a major threat to your operations.", 0f
                    )
                }
                if (isNegatedByPLMembership()) {
                    val c = Global.getSector().getFaction(Factions.PERSEAN).baseUIColor
                    val label = tooltip.addPara(
                        ("While your %s Membership makes it problematic for this matter to be pursued," +
                                " the inter-faction nature of the operation somewhat bypasses that restriction."), opad
                    )
                    label.setHighlight("Persean League", "problematic")
                    label.setHighlightColors(c, Misc.getPositiveHighlightColor())
                }
                if (fractalColony != null) {
                    val params = MapParams()
                    params.showSystem(fractalColony.starSystem)
                    val w = tooltip.widthSoFar
                    val h = (w / 1.6f).roundToInt().toFloat()
                    params.positionToShowAllMarkersAndSystems(true, w.coerceAtMost(h))
                    val map = tooltip.createSectorMap(
                        w,
                        h,
                        params,
                        fractalColony.starSystem.nameWithLowercaseTypeShort
                    )
                    tooltip.addCustom(map, opad)
                }
            }
        }
    }

    fun isNegatedByPLMembership(): Boolean {
        //if (true) return true;
        return PerseanLeagueMembership.isLeagueMember() && getProgress(false) > 0
    }
}