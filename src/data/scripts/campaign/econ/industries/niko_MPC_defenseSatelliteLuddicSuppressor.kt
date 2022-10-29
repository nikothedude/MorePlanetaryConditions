package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Factions

class niko_MPC_defenseSatelliteLuddicSuppressor : BaseIndustry() {
    override fun apply() {
        super.apply(true)
    }

    override fun getPatherInterest(): Float {
        val ourMarket = getMarket() ?: return 0f
        if (ourMarket.isFreePort) return 0f
        return -3f
    }

    override fun isHidden(): Boolean { //hidden structures dont block structure construction
        return true
    }

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean {
        return false
    }
}