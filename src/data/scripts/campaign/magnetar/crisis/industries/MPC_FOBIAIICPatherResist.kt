package data.scripts.campaign.magnetar.crisis.industries

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.Factions
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel

class MPC_FOBIAIICPatherResist: BaseIndustry() {
    override fun apply() {
        super.apply(false)
        return
    }

    override fun getPatherInterest(): Float {
        if (MPC_IAIICFobIntel.get()?.factionContributions?.any { it.factionId == Factions.LUDDIC_PATH } != true) return 0f
        return -Float.MAX_VALUE
    }
}