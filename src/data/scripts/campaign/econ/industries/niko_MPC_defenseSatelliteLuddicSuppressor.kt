package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry

class niko_MPC_defenseSatelliteLuddicSuppressor : BaseIndustry() {
    override fun apply() {
        super.apply(true)
    }

    override fun unapply() {
        super.unapply()
    }

    override fun getPatherInterest(): Float {
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