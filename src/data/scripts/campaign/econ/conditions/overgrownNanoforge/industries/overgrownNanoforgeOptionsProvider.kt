package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider.IndustryOptionData
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import java.awt.Color

class overgrownNanoforgeOptionsProvider: BaseIndustryOptionProvider() {

    companion object {
        const val OVERGROWN_NANOFORGE_INDUSTRY_DECONSTRUCTION_ID = "overgrownNanoforgeIndustryBeginDestruction"
        const val OVERGROWN_NANOFORGE_JUNK_DECONSTRUCTION_ID = "overgrownNanoforgeJunkBeginDestruction"
    }

    override fun getIndustryOptions(ind: Industry?): MutableList<IndustryOptionProvider.IndustryOptionData>? {
        if (isUnsuitable(ind, false)) return null

        if (ind is baseOvergrownNanoforgeStructure) {

            val result: MutableList<IndustryOptionData> = ArrayList()
            val option = ind.getDestructionOption(this)
            result += option

            return result
        }
        return null
    }

    override fun optionSelected(opt: IndustryOptionData?, ui: DialogCreatorUI?) {
        if (opt == null || ui == null) return

        if (opt.id == OVERGROWN_NANOFORGE_JUNK_DECONSTRUCTION_ID || opt.id == OVERGROWN_NANOFORGE_INDUSTRY_DECONSTRUCTION_ID) {
            val overgrownStructure = opt.ind as? baseOvergrownNanoforgeStructure ?: return
            overgrownStructure.startDestroying()
        }
    }


}