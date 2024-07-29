package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_magnetarCalibrator: niko_MPC_baseNikoScript() {
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val magnetarIntel = Global.getSector().intelManager.getFirstIntel(MPC_magnetarQuest::class.java) as? MPC_magnetarQuest ?: return
        if (magnetarIntel.stage >= MPC_magnetarQuest.Stage.RETURN_CHAIR) {
            delete()
            return
        }

        val tango = WormholeManager.get().getDeployed("MPC_Tango")
        if (tango != null && tango.jumpPoint.memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] == true) {
            tango.jumpPoint.memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] = false
            delete() // it only happens once
        }
    }
}