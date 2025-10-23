package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.loading.IndustrySpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition

class MPC_arrowPatherCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val INDUSTRY_INCREMENT = 1f
        const val INCOME_MULT = 0.25f

        const val CONSTRUCTION_PERCENT = 0.6f // construction is jumpstarted to this percent
    }

    override fun apply(id: String) {
        super.apply(id)

        market.incomeMult.modifyMult(modId, INCOME_MULT, name)
        market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).modifyFlat(modId, INDUSTRY_INCREMENT, name)
        accelerateConstruction()
    }

    private fun accelerateConstruction() {
        for (industry in market.industries) {
            val castedIndustry = industry as? BaseIndustry ?: continue
            if (!(castedIndustry.isUpgrading || castedIndustry.isBuilding)) continue
            var buildTime = castedIndustry.buildTime
            if (castedIndustry.isUpgrading) {
                val upgradeId = industry.spec.upgrade
                if (upgradeId != null) {
                    val upgradeSpec = Global.getSettings().getIndustrySpec(upgradeId)
                    if (upgradeSpec != null) {
                        buildTime = upgradeSpec.buildTime
                    }
                }
            }
            val minPercentProgress = (CONSTRUCTION_PERCENT)
            val minProgress = (buildTime * CONSTRUCTION_PERCENT)
            if (castedIndustry.buildOrUpgradeProgress < minPercentProgress) {
                castedIndustry.buildProgress = minProgress
            }
        }

    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.incomeMult.unmodify(modId)
        market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).unmodify(modId)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        tooltip.addPara(
            "%s industries",
            10f,
            Misc.getHighlightColor(),
            "+${INDUSTRY_INCREMENT.toInt().toString()}"
        )
        tooltip.addPara(
            "Structures/Upgrades start at %s progress",
            10f,
            Misc.getHighlightColor(),
            "${(CONSTRUCTION_PERCENT * 100f).toInt()}%"
        )

        tooltip.addPara(
            "%s colony income",
            10f,
            Misc.getNegativeHighlightColor(),
            "${INCOME_MULT}x"
        )

        tooltip.addPara(
            "You could, of course, use their manpower to rapidly build a colony, then remove them with a marine raid...",
            10f,
            Misc.getNegativeHighlightColor()
        ).color = Misc.getGrayColor()
    }
}