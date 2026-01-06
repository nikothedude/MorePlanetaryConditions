package data.scripts.campaign.supernova

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.ParticleControllerAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_moteSwarm(val parent: MPC_supernovaMoteScript, val params: MPC_moteSwarmParams): BaseCustomEntityPlugin() {

    companion object {
        const val PERMAMOTE_RADIUS_DIVISOR = 5f
        const val TARGETTING_RANGE = 1000f
        const val TURN_RATE = 5f

        // at this range, we will no longer be swarming AROUND the target. we will be accurately pursuing
        const val PURSUE_AT_RANGE = 25f
    }

    val permaMotes = populatePermaMotes()
    var maneuverTarget: SectorEntityToken? = null
    val movementDest: Vector2f = Misc.ZERO

    val retargetInterval = IntervalUtil(5f, 5.1f)
    val maneuverInterval = IntervalUtil(3f, 3.4f)

    private fun populatePermaMotes(): HashSet<ParticleControllerAPI> {
        val motes = HashSet<ParticleControllerAPI>()
        var totalMotes = (entity.radius / PERMAMOTE_RADIUS_DIVISOR).toInt().coerceAtLeast(3)

        while (totalMotes-- > 0) {

            var size = 3f + Math.random().toFloat() * 5f
            size *= 3f
            val color = if (false) Color(255, 100, 255, 175) else if (params.dweller) Color(255, 65, 65, 235) else Color(100,165,255,255)

            val newMote = Misc.addGlowyParticle(
                getSys(),
                Vector2f(entity.location),
                Misc.ZERO,
                size,
                0.5f,
                Float.MAX_VALUE,
                color
            )

            motes += newMote
        }

        return motes
    }

    val nextMote = IntervalUtil(params.minMoteDelay, params.maxMoteDelay)

    override fun advance(amount: Float) {
        retarget(amount)
        maneuver(amount)
        doMoteGraphics(amount)
    }

    private fun retarget(amount: Float) {
        retargetInterval.advance(amount)
        if (!retargetInterval.intervalElapsed()) return

        var lowestDist = Float.MAX_VALUE
        var closestFleet: CampaignFleetAPI? = null
        for (fleet in getSys().fleets) {
            val dist = MathUtils.getDistance(entity, fleet)
            if (dist > TARGETTING_RANGE) continue
            if (dist < lowestDist) {
                lowestDist = dist
                closestFleet = fleet
            }
        }

        maneuverTarget = closestFleet
    }

    private fun maneuver(amount: Float) {
        if (maneuverTarget == null) {

        }
    }

    private fun doMoteGraphics(amount: Float) {
        nextMote.advance(amount)
        if (nextMote.intervalElapsed()) {
            //Misc.glow
        }

        for (mote in permaMotes) {
            mote.setPos(entity.location.x, entity.location.y) // TODO - make them wiggle and swarm
        }
    }

    fun impactFleet(fleet: CampaignFleetAPI) {

    }

    fun getSys(): StarSystemAPI = parent.star.starSystem

    data class MPC_moteSwarmParams(
        var minMoteDelay: Float,
        var maxMoteDelay: Float,
        val dweller: Boolean = false,
    )
}