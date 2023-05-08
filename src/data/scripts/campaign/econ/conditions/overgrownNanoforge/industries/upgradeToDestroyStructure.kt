package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.baseNikoIndustry

class upgradeToDestroyStructure: baseNikoIndustry() {

    var timesApplied = 0
    val thresholdToDelete = 2

    override fun apply() {
        timesApplied++
        if (timesApplied >= thresholdToDelete) delete()
    }

    override fun sendBuildOrUpgradeMessage() {
        if (market.isPlayerOwned) {
            val intel = MessageIntel("Overgrown nanoforge" + " at " + market.name, Misc.getBasePlayerColor())
            intel.addLine(BaseIntelPlugin.BULLET + "Segment/Core destroyed")
            intel.icon = Global.getSector().playerFaction.crest
            intel.sound = BaseIntelPlugin.getSoundStandardUpdate()
            Global.getSector().campaignUI.addMessage(intel, CommMessageAPI.MessageClickAction.COLONY_INFO, market)
        }
    }
}