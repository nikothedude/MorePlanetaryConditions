package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil

class MPC_perseanLeagueFractalSupport: MPC_fractalCrisisSupport() {
    override val tracker: IntervalUtil = IntervalUtil(2f, 3f)

    override fun createFleet(): CampaignFleetAPI? {
        TODO("Not yet implemented")
    }

    override fun addDesc(info: TooltipMakerAPI) {
        TODO("Not yet implemented")
    }
}