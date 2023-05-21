package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider.IndustryOptionData

// unused, for now
class overgrownNanoforgeOptionsProvider: BaseIndustryOptionProvider() {

    companion object {
        const val OVERGROWN_NANOFORGE_INDUSTRY_OPEN_INTEL_OPTION_ID = "overgrownNanoforgeIndustryBeginDestruction"
        const val OVERGROWN_NANOFORGE_JUNK_DECONSTRUCTION_ID = "overgrownNanoforgeJunkBeginDestruction"
    }


    // TODO: just have this open the intel page
    override fun getIndustryOptions(ind: Industry?): MutableList<IndustryOptionData>? {
        if (isUnsuitable(ind, false)) return null

        if (ind is baseOvergrownNanoforgeStructure) {

            val result: MutableList<IndustryOptionData> = ArrayList()
            val option = ind.getIntelOption(this)
            if (option != null) result += option

            return result
        }
        return null
    }

    override fun optionSelected(opt: IndustryOptionData?, ui: DialogCreatorUI?) {
        if (opt == null || ui == null) return

        if (opt.id == OVERGROWN_NANOFORGE_JUNK_DECONSTRUCTION_ID || opt.id == OVERGROWN_NANOFORGE_INDUSTRY_OPEN_INTEL_OPTION_ID) {
            val overgrownStructure = opt.ind as? baseOvergrownNanoforgeStructure ?: return
            overgrownStructure.openIntel()
        }
    }


}