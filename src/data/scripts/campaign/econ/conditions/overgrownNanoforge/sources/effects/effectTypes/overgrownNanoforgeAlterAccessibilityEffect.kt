package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.utilities.niko_MPC_debugUtils.memKeyHasIncorrectType
import kotlin.math.abs

class overgrownNanoforgeAlterAccessibilityEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeFormattedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (isAccessabilityNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isAccessabilityNegative(): Boolean {
        return (increment < 0)
    }

    override fun getName(): String = if (isAccessabilityNegative()) "Eerie" else "Pleasant"

    override fun applyEffects() {
        getMarket().accessibilityMod.modifyFlat(getOurId(), increment, getNameForModifier())
    }

    override fun unapplyEffects() {
        getMarket().accessibilityMod.unmodifyFlat(getOurId())
    }

    override fun getBaseFormat(): String {
        return "Market accessibility $adjectiveChar by $changeChar%"
    }

    override fun getChange(positive: Boolean): String {
        return "${abs(increment * 100)}"
    }

}
