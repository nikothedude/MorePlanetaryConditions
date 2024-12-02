package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyAICoresActivityCause
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_ids

class MPC_IAIICInterferenceCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val BASE_ACCESSIBILITY_MALUS = -0.20f
        const val BASE_STABILITY_MALUS = -2f
        const val NON_HOSTILE_ACCESSIBILITY_MALUS = -0.25f

        const val MIN_COLONY_SIZE_FOR_DEFICIT = 4
        const val CORE_POINTS_NEEDED_FOR_DEFICIT = 5f

        fun isHostile(market: MarketAPI? = null): Boolean {
            val hostFaction = if (market != null) market.faction else Global.getSector().playerFaction
            val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return false
            if (hostFaction.getRelationshipLevel(IAIIC).min >= RepLevel.INHOSPITABLE.min) return true
            return false
        }
    }

    private fun isHostile(): Boolean {
        return Companion.isHostile(getMarket())
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        if (!marketIsSuspicious()) return

        market.stability.modifyFlat(id, BASE_STABILITY_MALUS, "IAIIC Interference")
        var accessibilityMod = BASE_ACCESSIBILITY_MALUS
        if (!isHostile()) {
            accessibilityMod += NON_HOSTILE_ACCESSIBILITY_MALUS
        }
        market.accessibilityMod.modifyFlat(id, accessibilityMod, "IAIIC Interference")
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        if (id == null) return

        val market = getMarket() ?: return
        market.stability.unmodify(id)
        market.accessibilityMod.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        if (!marketIsSuspicious()) {
            tooltip.addPara(
                "The IAIIC seems to be ignoring ${market.name}, likely due to the limited AI presence.", 10f
            )
            return
        }

        if (isHostile()) {
            tooltip.addPara(
                "The belligerence of the governing polity certainly limits the IAIIC's efforts, but not completely. Trade vessels often find themselves " +
                    "stop-searched by unsanctioned patrols, security keys frequently \"go missing\", and many spacers simply feel the space is too \"hot\" to enter.", 5f
            )
        } else {
            tooltip.addPara(
                "Groundside stop-searches by grey-and-green uniforms are common even for ${market.faction.displayName} officials. Things in orbit aren't any easier, with " +
                "STC being made hellish by the pacing spy-skiffs along trade routes, waiting for any opportunity to board a hapless civilian's vessel.",
                5f
            )
        }

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getNegativeHighlightColor(),
            "${(BASE_ACCESSIBILITY_MALUS * 100f).toInt()}%"
        )
        tooltip.addPara(
            "%s stability",
            5f,
            Misc.getNegativeHighlightColor(),
            "${BASE_STABILITY_MALUS.toInt()}"
        )

        if (isHostile()) return
        tooltip.addPara(
            "Due to the IAIIC's \"permitted\" access to your space, there is little in the way of resistance to their aggressive " +
            "investigatory tactics, resulting in a further %s accessibility penalty.",
            10f,
            Misc.getNegativeHighlightColor(),
            "${(NON_HOSTILE_ACCESSIBILITY_MALUS * 100f).toInt()}%"
        )
    }

    fun marketIsSuspicious(): Boolean {
        return market.admin.isAICore || HegemonyAICoresActivityCause.getAICorePoints(market) >= CORE_POINTS_NEEDED_FOR_DEFICIT
    }
}