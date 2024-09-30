package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import lunalib.lunaExtensions.getMarketsCopy

class MPC_omegaCoreAdminChecker: niko_MPC_baseNikoScript() {
    val interval = IntervalUtil(2.3f, 2.4f)

    override fun startImpl() {
        Global.getSector().addTransientScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeTransientScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            checkOmega()
        }
    }

    private fun checkOmega() {
        for (market in Global.getSector().playerFaction.getMarketsCopy()) {
            val core = market.admin
            market.memoryWithoutUpdate[niko_MPC_ids.HAS_HAD_OMEGA_CORE_FOR_SOME_TIME] = (core.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID)
        }
    }
}