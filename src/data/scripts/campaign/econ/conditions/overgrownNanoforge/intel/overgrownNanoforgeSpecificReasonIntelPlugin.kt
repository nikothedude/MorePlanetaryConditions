package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import data.scripts.campaign.intel.baseNikoIntelPlugin

class overgrownNanoforgeSpecificReasonIntelPlugin(
    val market: MarketAPI,
    val specifics: MutableSet<overgrownNanoforgeIntel.cullingStrengthValues.cullingStrengthReasons>,
): baseNikoIntelPlugin() {

    override fun hasLargeDescription(): Boolean {
        return true
    }

    override fun getName(): String {
        return "test"
    }

    override fun reportPlayerClickedOn() {
        super.reportPlayerClickedOn()
    }

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        super.createLargeDescription(panel, width, height)
    }

}