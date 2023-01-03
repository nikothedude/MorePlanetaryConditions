package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import data.scripts.campaign.econ.industries.baseNikoIndustry
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

abstract class baseOvergrownNanoforgeStructure: baseNikoIndustry() {

    override fun canShutDown(): Boolean {
        return false
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
        val marketLocation = market.location ?: return false
        val distance = MathUtils.getDistance(market.location, playerLocation)
        if (distance > niko_MPC_settings.OVERGROWN_NANOFORGE_INTERACTION_DISTANCE) return true
        return false
    }

    override fun upgradeFinished(previous: Industry?) {
        super.upgradeFinished(previous)
        delete()
        reportDestroyed()
    }

    open fun reportDestroyed() {
        return
    }

}
