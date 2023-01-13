package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import data.scripts.campaign.intel.baseNikoIntelPlugin

class overgrownNanoforgeIntel: baseNikoIntelPlugin() {

    var name: String = "Overgrown Nanoforge"

    override fun getName(): String = name

    override fun isImportant(): Boolean {
        return true
    }

}