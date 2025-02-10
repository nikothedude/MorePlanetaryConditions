package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityCause2
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.derelictEscort.crisis.MPC_derelictEscortCause
import data.scripts.campaign.econ.conditions.derelictEscort.crisis.MPC_derelictEscortFactor
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCauseOne
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCauseTwo
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_hostileActivityHook: niko_MPC_baseNikoScript() {
    val interval = IntervalUtil(0.1f, 0.2f) // days

    override fun startImpl() {
        Global.getSector().addTransientScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (interval.intervalElapsed()) {
            val ha = HostileActivityEventIntel.get() ?: return
            val hegemonyFactor = ha.getActivityOfClass(HegemonyHostileActivityFactor::class.java)
            if (hegemonyFactor != null && hegemonyFactor.getCauseOfClass(MPC_hegemonyFractalCoreCauseOne::class.java) == null) {
                hegemonyFactor.addCause(MPC_hegemonyFractalCoreCauseOne(ha))
            }

            val fractalFactor = ha.getActivityOfClass(MPC_fractalCoreFactor::class.java)
            if (fractalFactor == null) ha.addActivity(MPC_fractalCoreFactor(ha), MPC_hegemonyFractalCoreCauseTwo(ha))

            val FRCfactor = ha.getActivityOfClass(MPC_derelictEscortFactor::class.java)
            if (FRCfactor == null) ha.addActivity(MPC_derelictEscortFactor(ha), MPC_derelictEscortCause(ha))
        }
    }
}