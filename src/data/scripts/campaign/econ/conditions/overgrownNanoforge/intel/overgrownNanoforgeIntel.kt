package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import data.scripts.campaign.intel.baseNikoIntelPlugin

class overgrownNanoforgeIntel: baseNikoIntelPlugin() {

    override fun getName(): String {
        return "Overgrown Nanoforges"
    }

    override fun isImportant(): Boolean {
        return true
    }

}