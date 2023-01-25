package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.Industry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.isJunk
import data.utilities.niko_MPC_marketUtils.isJunkStructure

class overgrownNanoforgeVolatileEffect(
    nanoforge: overgrownNanoforgeIndustry
): overgrownNanoforgeRandomizedEffect(nanoforge) {

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

    private fun Industry.isValidTarget(): Boolean {
        return (!this.isJunkStructure())
    }

    private fun explode() {
        val validTargets = HashSet<Industry>()
        for (possibleTarget in getMarket().getVisibleIndustries()) {
            if (possibleTarget.isValidTarget()) validTargets += possibleTarget
        }
        val target = validTargets.randomOrNull() ?: return
        target.setDisrupted(VOLATILE_EXPLOSION_DURATION, true)
    }
}
