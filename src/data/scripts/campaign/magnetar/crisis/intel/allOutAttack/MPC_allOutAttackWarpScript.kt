package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_allOutAttackWarpScript: niko_MPC_baseNikoScript() {

    enum class Stage {
        BEGINNING,
        WARPING
    }

    var stage = Stage.BEGINNING

    override fun startImpl() {
        TODO("Not yet implemented")
    }

    override fun stopImpl() {
        TODO("Not yet implemented")
    }

    override fun runWhilePaused(): Boolean {
        TODO("Not yet implemented")
    }

    override fun advance(amount: Float) {
        TODO("Not yet implemented")
    }
}