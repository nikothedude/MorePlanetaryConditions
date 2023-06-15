package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource

class overgrownNanoforgeSpawnFleetEffect(
    nanoforgeHandler: overgrownNanoforgeIndustryHandler
): overgrownNanoforgeRandomizedEffect(nanoforgeHandler) {
    val spawningScript: overgrownNanoforgeSpawnFleetScript = overgrownNanoforgeSpawnFleetScript(this)

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return overgrownNanoforgeEffectCategories.DEFICIT
    }

    override fun getName(): String {
        return "Derelict auto-factory"
    }

    override fun getDescription(): String {
        return "TODO"
    }

    override fun applyEffects() {
        spawningScript.start()
    }

    override fun unapplyEffects() {
        spawningScript.stop()
    }

    override fun delete() {
        spawningScript.spawnBombardmentFleet()
        spawningScript.delete()
        super.delete()
    }
}
