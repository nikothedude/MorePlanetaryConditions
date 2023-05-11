package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import data.utilities.niko_MPC_ids.INTEL_OVERGROWN_NANOFORGES

class overgrownNanoforgeIntel(
    val ourNanoforgeHandler: overgrownNanoforgeIndustryHandler
): baseNikoEventIntelPlugin() {

    init {
        hidden = true
    }

    fun init() {

    }

    override fun advance(amount: Float) {
        super.advance(amount)
    }

    override fun getName(): String {
        return "Overgrown Nanoforge on {$ourNanoforgeHandler.market.name}"
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags += INTEL_OVERGROWN_NANOFORGES
        return tags
    }

    override fun isImportant(): Boolean {
        return true
    }

    override fun isHidden(): Boolean {
        return hidden
    }

}