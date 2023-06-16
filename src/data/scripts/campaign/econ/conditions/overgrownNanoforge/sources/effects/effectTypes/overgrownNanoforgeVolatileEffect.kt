package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.Industry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_settings.VOLATILE_EFFECT_INDUSTRIES_TO_DISRUPT

class overgrownNanoforgeVolatileEffect(
    handler: overgrownNanoforgeHandler
): overgrownNanoforgeRandomizedEffect(handler) {

    var canExplode: Boolean = true

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
        return "Apon destruction, a random industry will be disrupted for $VOLATILE_EXPLOSION_DURATION days"
    }

    override fun applyEffects() {
        canExplode = true
    }

    override fun unapplyEffects() {
        canExplode = false
    }


    override fun delete() {
        explode()
        super.delete()
    }

    private fun Industry.isValidTarget(): Boolean {
        return (canExplode && !this.isJunkStructure())
    }

    private fun explode() {
        if (!canExplode) return
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
