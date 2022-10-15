package data.scripts.campaign.econ

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition
import org.apache.log4j.Level

class niko_MPC_planetaryDroneReplicator : BaseHazardCondition() {
    override fun apply(id: String) {
        val primaryEntity : SectorEntityToken? = market.primaryEntity
        if (primaryEntity != null) {
        }
    }

    companion object {
        private val log = Global.getLogger(niko_MPC_planetaryDroneReplicator::class.java)

        init {
            log.level = Level.ALL
        }
    }
}