package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import kotlin.math.abs

class overgrownNanoforgeAlterHazardEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeRandomizedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (!hazardIsNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun hazardIsNegative(): Boolean {
        return (increment < 0)
    }

    override fun getName(): String {
        if (!hazardIsNegative()) return "Hazardous" else return "Safe"
    }

    override fun getDescription(): String {
        return "wdabuhiwdawu"
    }

    override fun applyBenefits() {
        if (!hazardIsNegative()) return
        getMarket().hazard.modifyFlat(getId(), increment, getNameForModifier())
    }

    override fun applyDeficits() {
        if (hazardIsNegative()) return
        getMarket().hazard.modifyFlat(getId(), increment, getNameForModifier())
    }

    override fun unapplyBenefits() {
        if (hazardIsNegative()) return
        getMarket().hazard.unmodifyFlat(getId())
    }

    override fun unapplyDeficits() {
        if (!hazardIsNegative()) return
        getMarket().hazard.unmodifyFlat(getId())
    }

    override val baseFormat: String = "Market hazard $adjectiveChar by $changeChar"

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return "${abs(increment * 100)}"
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive && !hazardIsNegative()) return list
        if (!positive && hazardIsNegative()) return list
        return super.getAllFormattedEffects(positive)
    }

}
