package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel

class MPC_IAIICAllOutAttackAssemble(raid: RaidIntel?, gatheringPoint: SectorEntityToken?) : AssembleStage(raid, gatheringPoint) {

    override fun getFPSmall(): Float {
        return super.getFPSmall() * 7f
    }
    override fun getFPMedium(): Float {
        return super.getFPMedium() * 7f
    }
    override fun getFPLarge(): Float {
        return super.getFPLarge() * 7f
    }
}