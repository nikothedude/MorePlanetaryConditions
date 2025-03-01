package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode
import com.fs.starfarer.api.campaign.econ.Industry.ImprovementDescriptionMode
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.GroundDefenses
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Strings
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_constructionAcceleratorIndustry: baseNikoIndustry() {

    companion object {
        const val CONSTRUCTION_SPEED_MULT = 2f
        const val ALPHA_CORE_SPEED_BONUS = 1f
        const val IMPROVED_BONUS = 1f
    }

    override fun apply() {
        super.apply(true)

        val size = market.size

        demand(Commodities.SUPPLIES, size)
        demand(Commodities.RARE_METALS, size - 2)
        demand(Commodities.METALS, size - 1)
        demand(Commodities.HEAVY_MACHINERY, size - 2)
        demand(Commodities.ORGANICS, size - 2)
        demand(Commodities.CREW, size + 1)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!isFunctional) return

        val days = Misc.getDays(amount)
        val deficitMult = getDeficitMult(
            Commodities.SUPPLIES,
            Commodities.RARE_METALS,
            Commodities.METALS,
            Commodities.HEAVY_MACHINERY,
            Commodities.ORGANICS,
            Commodities.CREW
        )
        var baseMult = CONSTRUCTION_SPEED_MULT
        if (isImproved) baseMult += IMPROVED_BONUS
        if (aiCoreId == Commodities.ALPHA_CORE) baseMult += ALPHA_CORE_SPEED_BONUS
        val toIncrease = ((days / 2) * baseMult) * deficitMult

        for (iterInd in getMarket().industries) {
            if (iterInd == this) continue
            val castedIndustry = iterInd as? BaseIndustry ?: continue
            if (!iterInd.isBuilding || !iterInd.isUpgrading) continue

            iterInd.buildProgress += toIncrease
        }
    }

    override fun addPostDemandSection(
        tooltip: TooltipMakerAPI?,
        hasDemand: Boolean,
        mode: Industry.IndustryTooltipMode?
    ) {
        super.addRightAfterDescriptionSection(tooltip, mode)
        tooltip?.addPara(
            "Increases construction and upgrade speed of other industries by %s.", 5f, Misc.getHighlightColor(), "${((CONSTRUCTION_SPEED_MULT - 1f) * 100f).trimHangingZero()}%"
        )
        val deficitMult = getDeficitMult(
            Commodities.SUPPLIES,
            Commodities.RARE_METALS,
            Commodities.METALS,
            Commodities.HEAVY_MACHINERY,
            Commodities.ORGANICS,
            Commodities.CREW
        )
        if (deficitMult < 1f) {
            tooltip?.addPara(
                "Construction bonus decreased by %s due to deficits.", 5f, Misc.getHighlightColor(), "${(((1 - deficitMult) * 100f)).roundNumTo(1).trimHangingZero()}%"
            )?.setColor(Misc.getNegativeHighlightColor())
        }
    }

    override fun addAlphaCoreDescription(tooltip: TooltipMakerAPI?, mode: Industry.AICoreDescriptionMode?) {
        val opad = 10f
        val highlight = Misc.getHighlightColor()

        var pre = "Alpha-level AI core currently assigned. "
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = "Alpha-level AI core. "
        }
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            val coreSpec = Global.getSettings().getCommoditySpec(aiCoreId)
            val text = tooltip!!.beginImageWithText(coreSpec.iconName, 48f)
            text.addPara(
                pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                        "Increases construction speed by an additional %s.", 0f, highlight,
                "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
                "" + "${(ALPHA_CORE_SPEED_BONUS * 100f).trimHangingZero()}%"
            )
            tooltip!!.addImageWithText(opad)
            return
        }

        tooltip!!.addPara(
            pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                    "Increases construction speed by an additional %s.", 0f, highlight,
            "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
            "" + "${(ALPHA_CORE_SPEED_BONUS * 100f).trimHangingZero()}%"
        )
    }

    override fun getUnavailableReason(): String? {
        if (!super.isAvailableToBuild()) {
            return super.getUnavailableReason()
        }

        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return "Blueprint unknown"
        }
        return null
    }

    override fun isAvailableToBuild(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        return super.isAvailableToBuild()
    }

    override fun showWhenUnavailable(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        return super.showWhenUnavailable()
    }

    override fun canImprove(): Boolean = true
    override fun addImproveDesc(info: TooltipMakerAPI, mode: ImprovementDescriptionMode) {
        val opad = 10f
        val highlight = Misc.getHighlightColor()
        val bonus = IMPROVED_BONUS
        val str = "${(IMPROVED_BONUS * 100f).trimHangingZero()}%"
        if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
            info.addPara("Construction speed increased by an additional %s.", 0f, highlight, str)
        } else {
            info.addPara("Increases construction speed by an additional %s.", 0f, highlight, str)
        }
        info.addSpacer(opad)
        super.addImproveDesc(info, mode)
    }
}