package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.intel.raid.AssembleStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel.Companion.ALL_OUT_ATTACK_FP_MULT

class MPC_IAIICAllOutAttackAssemble(raid: RaidIntel?, gatheringPoint: SectorEntityToken?) : AssembleStage(raid, gatheringPoint) {

    override fun getLargeSize(limitToSpawnFP: Boolean): Float {
        var base = MPC_IAIICFobIntel.ALL_OUT_ATTACK_FP_MULT
        if (limitToSpawnFP) base = base.coerceAtMost(spawnFP)
        return base
    }

    override fun getFPSmall(): Float {
        return super.getFPSmall() * ALL_OUT_ATTACK_FP_MULT
    }
    override fun getFPMedium(): Float {
        return super.getFPMedium() * ALL_OUT_ATTACK_FP_MULT
    }
    override fun getFPLarge(): Float {
        return super.getFPLarge() * ALL_OUT_ATTACK_FP_MULT
    }
}