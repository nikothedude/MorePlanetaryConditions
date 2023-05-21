package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.Industry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_settings.VOLATILE_EFFECT_INDUSTRIES_TO_DISRUPT

class overgrownNanoforgeVolatileEffect(
    handler: overgrownNanoforgeHandler
): overgrownNanoforgeRandomizedEffect(handler) {

    companion object {
        const val VOLATILE_EXPLOSION_DURATION = 90f
    }

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return overgrownNanoforgeEffectCategories.DEFICIT
    }

    override fun getName(): String {
        return "Volatile"
    }

    override fun getDescription(): String {
        return "Volatile"
    }

    override fun applyBenefits() {
        return
    }

    override fun applyDeficits() {
        return
    }

    override fun unapplyBenefits() {
        return
    }

    override fun unapplyDeficits() {
        return
    }

    override fun delete() {
        explode()
        super.delete()
    }

    override val baseFormat: String = "whatever"

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return ""
    }

    override fun getFormattedEffect(format: String, positive: Boolean, vararg args: Any): String {
        return "If this structure is culled, it will explode violently and damage other structures"
    }

    private fun Industry.isValidTarget(): Boolean {
        return (!this.isJunkStructure())
    }

    private fun explode() {
        val validTargets = HashSet<Industry>()
        for (possibleTarget in getMarket().getVisibleIndustries()) {
            if (possibleTarget.isValidTarget()) validTargets += possibleTarget
        }
        val pickedTargets: MutableSet<Industry> = HashSet()
        var industriesToDisrupt: Float = VOLATILE_EFFECT_INDUSTRIES_TO_DISRUPT
        for (target in validTargets.shuffled()) {
            pickedTargets += target
            industriesToDisrupt--
        }
        pickedTargets.forEach { it.setDisrupted(VOLATILE_EXPLOSION_DURATION, true) }

        //TODO: do intel here
    }
}
