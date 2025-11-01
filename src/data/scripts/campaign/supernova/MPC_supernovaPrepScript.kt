package data.scripts.campaign.supernova

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_supernovaPrepScript(
    val star: PlanetAPI
): niko_MPC_baseNikoScript() {

    companion object {
        fun getExplosionInterval(withUpdate: Boolean = false): IntervalUtil? {
            var timer = Global.getSector().memoryWithoutUpdate[EXPLOSION_TIMER_MEMID] as? IntervalUtil
            if (timer == null && withUpdate) {
                timer = IntervalUtil(niko_MPC_settings.MIN_TIME_TIL_SUPERNOVA, niko_MPC_settings.MAX_TIME_TIL_SUPERNOVA)
                Global.getSector().memoryWithoutUpdate[EXPLOSION_TIMER_MEMID] = timer
            }
            return timer
        }

        const val EXPLOSION_TIMER_MEMID = "\$MPC_supernovaTimer"
    }

    override fun startImpl() {
        star.addScript(this)
    }

    override fun stopImpl() {
        star.removeScript(this)
    }

    var baseMiddleRad = 0f
    var baseBandwidth = 0f
    var solarWinds = 0f
    init {
        val corona = Misc.getCoronaFor(star)
        val params = (corona.params)

        baseMiddleRad = params.middleRadius
        baseBandwidth = params.bandWidthInEngine
        solarWinds = params.windBurnLevel
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val supernovaInterval = getExplosionInterval(true)!!
        val days = Misc.getDays(amount) * getCurrentSupernovaAdvanceMult(supernovaInterval)
        supernovaInterval.advance(days)
        if (supernovaInterval.intervalElapsed()) {
            doSupernova()
            delete()
            return
        }
        checkIntervalThresholds(supernovaInterval)
    }

    private fun checkIntervalThresholds(interval: IntervalUtil) {
        val dur = interval.intervalDuration
        val curr = interval.elapsed
        val progress = (curr / dur)

        val corona = Misc.getCoronaFor(star) ?: return
        val params = corona.params
        val sizeMult = 1f + (3f * (progress))
        params.middleRadius = baseMiddleRad * sizeMult
        params.bandWidthInEngine = baseBandwidth * sizeMult
        params.windBurnLevel = solarWinds * sizeMult
    }

    private fun getCurrentSupernovaAdvanceMult(interval: IntervalUtil): Float {
        var base = 1f

        val dur = interval.intervalDuration
        val curr = interval.elapsed
        val playerStats = Global.getSector().playerStats

        if (dur * 0.7f <= curr) {
            if (playerStats.level >= 10f) {
                base += 2f
            }
        }

        return base
    }

    private fun doSupernova() {
        TODO("Not yet implemented")
    }
}