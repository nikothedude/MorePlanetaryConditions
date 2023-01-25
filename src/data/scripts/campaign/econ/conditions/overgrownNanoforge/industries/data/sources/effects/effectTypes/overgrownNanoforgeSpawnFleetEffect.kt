package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.Industry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.isJunkStructure

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
