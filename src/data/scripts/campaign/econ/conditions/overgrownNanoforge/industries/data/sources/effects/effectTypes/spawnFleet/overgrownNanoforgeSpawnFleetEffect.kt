package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.spawnFleet

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeSpawnFleetEffect(
    nanoforge: overgrownNanoforgeIndustry
): overgrownNanoforgeRandomizedEffect(nanoforge) {
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
}
