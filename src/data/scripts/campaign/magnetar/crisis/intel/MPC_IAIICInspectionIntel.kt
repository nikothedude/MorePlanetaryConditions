package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate
import data.utilities.niko_MPC_ids

class MPC_IAIICInspectionIntel(val from: MarketAPI, val target: MarketAPI, val inspectionFP: Float): RaidIntel(target.starSystem, from.faction, null), RaidDelegate {

    protected var expectedCores: MutableList<String> = ArrayList()
    var targettingFractalCore: Boolean = false
    var orders
    override fun notifyRaidEnded(raid: RaidIntel?, status: RaidStageStatus?) {
        TODO("Not yet implemented")
    }

    init {
        for (curr in target.industries) {
            val id = curr?.aiCoreId
            if (id != null) {
                expectedCores.add(id)
            }
        }
        val admin: PersonAPI = target.admin
        if (admin.isAICore) {
            expectedCores += (admin.aiCoreId)
            if (admin.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) targettingFractalCore = true
        }

        val orgDur = 20f + 10f * Math.random().toFloat()
    }

}