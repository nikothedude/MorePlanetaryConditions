package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.utilities.niko_MPC_debugUtils.memKeyHasIncorrectType
import kotlin.math.abs

class overgrownNanoforgeAlterAccessibilityEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeRandomizedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (isAccessabilityNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isAccessabilityNegative(): Boolean {
        return (increment < 0)
    }

    override fun getName(): String = if (isAccessabilityNegative()) "Eerie" else "Pleasant"


    override fun getDescription(): String {
        if (isAccessabilityNegative()) {
            return "This area is filled with eerie sounds, structures, and is all around uninviting, reducing accessibility by ${increment}%."
        } else {
            return "This area is surprisingly tame and easy to traverse, increasing accessibility by ${increment}%."
        }
    }

    override fun applyBenefits() {
        if (isAccessabilityNegative()) return
        getMarket().accessibilityMod.modifyFlat(getOurId(), increment, getNameForModifier())
    }

    override fun applyDeficits() {
        if (!isAccessabilityNegative()) return
        getMarket().accessibilityMod.modifyFlat(getOurId(), increment, getNameForModifier())
    }

    override fun unapplyBenefits() {
        if (isAccessabilityNegative()) return
        getMarket().accessibilityMod.unmodifyFlat(getOurId())
    }

    override fun unapplyDeficits() {
        if (!isAccessabilityNegative()) return
        getMarket().accessibilityMod.unmodifyFlat(getOurId())
    }

    override val baseFormat: String = "Market accessibility $adjectiveChar by $changeChar"

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive && isAccessabilityNegative()) return list
        if (!positive && !isAccessabilityNegative()) return list
        return super.getAllFormattedEffects(positive)
    }

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return "${abs(increment * 100)}"
    }

}
