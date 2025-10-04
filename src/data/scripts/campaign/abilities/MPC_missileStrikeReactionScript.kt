package data.scripts.campaign.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_missileStrikeReactionScript: niko_MPC_baseNikoScript() {

    companion object {
        fun get(withUpdate: Boolean): MPC_missileStrikeReactionScript? {
            var listener = Global.getSector().memoryWithoutUpdate["\$MPC_missileStrikeReactionScript"] as? MPC_missileStrikeReactionScript
            if (listener == null && withUpdate) {
                listener = MPC_missileStrikeReactionScript()
                Global.getSector().memoryWithoutUpdate["\$MPC_missileStrikeReactionScript"] = listener
            }
            return listener
        }
    }

    val interval = IntervalUtil(60f, 90f)

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
        Global.getSector().memoryWithoutUpdate.unset("\$MPC_missileStrikeReactionScript")
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (interval.intervalElapsed()) {
            Global.getSector().memoryWithoutUpdate["\$MPC_canDoHegeMissileCarrierReaction"] = true
            Global.getSector().memoryWithoutUpdate["\$MPC_missileStrikeReactionPrepared"] = true
            delete()
            return
        }
    }
}