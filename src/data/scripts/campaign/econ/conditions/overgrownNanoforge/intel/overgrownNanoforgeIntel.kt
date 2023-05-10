package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import data.scripts.campaign.intel.baseNikoEventIntelPlugin

class overgrownNanoforgeIntel(
    val ourNanoforgeHandler: overgrownNanoforgeIndustryHandler
): baseNikoEventIntelPlugin() {

    var hidden: Boolean = false

    fun init() {

    }

    override fun getName(): String {
        return "Overgrown Nanoforge on {$ourNanoforgeHandler.market.name}"
    }

    override fun isImportant(): Boolean {
        return true
    }

    override fun isHidden(): Boolean {
        return hidden
    }

}