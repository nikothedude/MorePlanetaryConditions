package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import data.scripts.campaign.intel.baseNikoEventIntelPlugin

class overgrownNanoforgeIntel: baseNikoEventIntelPlugin() {

    var ourName: String = "Overgrown Nanoforge"
    val ourNanoforge: overgrownNanoforgeCondition

    override fun getName(): String = ourName

    override fun isImportant(): Boolean {
        return true
    }

}