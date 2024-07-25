package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.console.BaseCommand
import java.awt.Color

class niko_MPC_magnetarStarScript(
    val magnetar: PlanetAPI
): niko_MPC_baseNikoScript() {

    companion object {
        const val MIN_DAYS_PER_PULSE = 4f
        const val MAX_DAYS_PER_PULSE = 4.2f
    }

    val daysPerPulse = IntervalUtil(MIN_DAYS_PER_PULSE, MAX_DAYS_PER_PULSE)

    override fun startImpl() {
        magnetar.addScript(this)
    }

    override fun stopImpl() {
        magnetar.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val containingLocation = magnetar.containingLocation ?: return
        if (containingLocation != Global.getSector().playerFleet?.containingLocation) return

        val days = Misc.getDays(amount)
        daysPerPulse.advance(days)
        if (daysPerPulse.intervalElapsed()) {
            doPulse()
        }
    }

    fun doPulse() {
        val color = niko_MPC_magnetarPulse.BASE_COLOR
        val params = ExplosionEntityPlugin.ExplosionParams(color, magnetar.containingLocation, magnetar.location, 500f, 2f)
        params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        val explosion = magnetar.containingLocation.addCustomEntity(
            Misc.genUID(), "Ionized Pulse",
            "MPC_magnetarPulse", Factions.NEUTRAL, params
        )
        explosion.setLocation(magnetar.location.x, magnetar.location.y)
    }
}