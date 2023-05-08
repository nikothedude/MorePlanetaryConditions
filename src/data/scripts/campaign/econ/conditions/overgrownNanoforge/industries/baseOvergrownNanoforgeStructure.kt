package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.baseNikoIndustry
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

abstract class baseOvergrownNanoforgeStructure: baseNikoIndustry() {

    override fun canShutDown(): Boolean {
        return false
    }

    override fun unapply() {
        super.unapply()
    }

    override fun canUpgrade(): Boolean {
        return canBeDestroyed()
    }
    abstract fun canBeDestroyed(): Boolean

    fun playerNotNearAndDoWeCare(): Boolean {
        if (!niko_MPC_settings.OVERGROWN_NANOFORGE_CARES_ABOUT_PLAYER_PROXIMITY_FOR_DECON) return false

        val playerFleet = Global.getSector().playerFleet ?: return true
        val marketContainingLocation = market.containingLocation ?: return false
        val playerContainingLocation = playerFleet.containingLocation ?: return true
        if (marketContainingLocation != playerContainingLocation) return true

        val playerLocation = playerFleet.location ?: return true
        val marketLocation = market.primaryEntity?.location ?: market.location ?: return false
        val distance = MathUtils.getDistance(marketLocation, playerLocation)
        if (distance > niko_MPC_settings.OVERGROWN_NANOFORGE_INTERACTION_DISTANCE) return true
        return false
    }

    override fun upgradeFinished(previous: Industry?) {
        super.upgradeFinished(previous)
        reportDestroyed()
    }

    override fun sendBuildOrUpgradeMessage() {
        if (market.isPlayerOwned) {
            val intel = MessageIntel(currentName + " at " + market.name, Misc.getBasePlayerColor())
            intel.addLine(BaseIntelPlugin.BULLET + "dasfxgjjy")
            intel.icon = Global.getSector().playerFaction.crest
            intel.sound = BaseIntelPlugin.getSoundStandardUpdate()
            Global.getSector().campaignUI.addMessage(intel, MessageClickAction.COLONY_INFO, market)
        }
    }

    open fun reportDestroyed() {
        delete()
    }

}
