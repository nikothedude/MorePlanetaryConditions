package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource

class overgrownNanoforgeSpawnFleetEffect(
    nanoforgeHandler: overgrownNanoforgeIndustryHandler, source: overgrownNanoforgeEffectSource
): overgrownNanoforgeRandomizedEffect(nanoforgeHandler, source) {
    val spawningScript: overgrownNanoforgeSpawnFleetScript = overgrownNanoforgeSpawnFleetScript(this)

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return overgrownNanoforgeEffectCategories.DEFICIT
    }

    override fun getName(): String {
        return "Derelict auto-factory"
    }

    override fun getDescription(): String {
        return "Derelict aruhi8oawui"
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
        spawningScript.spawnBombardmentFleet()
        spawningScript.delete()
        super.delete()
    }

    override val baseFormat: String
        get() = "5"

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return "52"
    }
}
