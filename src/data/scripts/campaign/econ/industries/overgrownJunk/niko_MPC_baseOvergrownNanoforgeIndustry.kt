package data.scripts.campaign.econ.industries.overgrownJunk

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.baseNikoIndustry
import data.utilities.niko_MPC_settings

abstract class niko_MPC_baseOvergrownNanoforgeIndustry: baseNikoIndustry() {

    override fun canDowngrade(): Boolean {
        val playerFleet = Global.getSector().playerFleet ?: return false
        if (playerNotInSystemAndDoWeCare(playerFleet)) return false
        if (playerNotNearUsAndDoWeCare(playerFleet)) return false
        return super.canDowngrade()
    }

    override fun canShutDown(): Boolean {
        return false
    }

    fun playerNotNearUsAndDoWeCare(playerFleet: CampaignFleetAPI): Boolean {
        if (!niko_MPC_settings.OVERGROWN_NANOFORGE_DISTANCE_JUNK_DOWNGRADE) return false
        val playerLocation = playerFleet.location ?: return false
        val marketLocation = market.location ?: return false
        if (Misc.getDistance(playerLocation, marketLocation) > getShutdownDistance()) return true
        return false
    }

    fun playerNotInSystemAndDoWeCare(playerFleet: CampaignFleetAPI): Boolean {
        if (niko_MPC_settings.OVERGROWN_NANOFORGE_INSYSTEM_JUNK_DOWNGRADE && market.containingLocation != playerFleet.containingLocation) return true
        return false
    }

    fun getShutdownDistance(): Float {
        return 2500f
    }

}
