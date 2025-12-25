package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BeamAPI
import com.fs.starfarer.api.combat.BeamEffectPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EmpArcEntityAPI
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.IonBeamEffect
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

class MPC_icewindPayloadScript: BeamEffectPlugin {

    companion object {
        const val FIRST_ARC_SPEED = 10000f

        const val DAMAGE_MULT = 0.5f
        const val EMP_MULT = 0.5f
    }

    var firstArcInterval = IntervalUtil(0.1f, 0.3f)

    override fun advance(amount: Float, engine: CombatEngineAPI?, beam: BeamAPI?) {
        if (beam == null || engine == null) return
        if (engine.isPaused) return
        var amount = amount
        amount *= beam.source?.mutableStats?.timeMult?.modifiedValue ?: 1f
        firstArcInterval.advance(amount)
        if (!firstArcInterval.intervalElapsed()) return
        val targetLoc = beam.to
        val target = beam.damageTarget

        var to = beam.rayEndPrevFrame
        var to2 = beam.to

        var dist: Float = Misc.getDistance(beam.from, beam.rayEndPrevFrame)
        var dist2: Float = Misc.getDistance(beam.from, beam.to)
        if (dist2 < dist) {
            to = to2
            dist = dist2
        }

        //if (dist > 100f && spawnedExplosion) {
        /*val params = EmpArcParams()
        params.segmentLengthMult = 8f
        params.zigZagReductionFactor = 3f
        params.fadeOutDist = 50f
        params.minFadeOutMult = 10f

        //			params.flickerRateMult = 0.7f;
        params.flickerRateMult = 0.3f


        //			params.flickerRateMult = 0.05f;
        //			params.glowSizeMult = 3f;-

        val fraction = min(0.33f, 300f / dist)
        params.brightSpotFullFraction = fraction
        params.brightSpotFadeFraction = fraction

        //val arcSpeed = FIRST_ARC_SPEED
        //params.movementDurOverride = max(0.05f, dist / arcSpeed)

        val ship = beam.source

        //Color color = weapon.getSpec().getGlowColor();
        val arc = engine.spawnEmpArcVisual(
            beam.from, ship, to, ship,
            beam.width,  // thickness
            beam.fringeColor,
            beam.coreColor,
            params
        ) as EmpArcEntityAPI
        //arc.coreWidthOverride = 40f

        arc.setRenderGlowAtStart(false)
        arc.setFadedOutAtStart(true)
        arc.setSingleFlickerMode(true)*/

        val weapon = beam.weapon ?: return

        engine.spawnEmpArcVisual(
            weapon.location,
            weapon.ship,
            to,
            target,
            beam.width * 0.5f,
            beam.fringeColor,
            beam.coreColor
        )

        Global.getSoundPlayer().playSound(
            "tachyon_lance_emp_impact",
            1f,
            1f,
            to,
            Misc.ZERO
        )

        if (target != null) {
            val hitShield = target.shield != null && target.shield.isWithinArc(beam.to)
            var pierceChance = 1f
            if (target is ShipAPI) {
                val hardFlux = target.hardFluxLevel
                val coeff = 0.1f * target.mutableStats.dynamic.getValue(Stats.SHIELD_PIERCED_MULT)

                pierceChance = (hardFlux * coeff)
            }
            pierceChance *= 100f
            if (target != null && (!hitShield || niko_MPC_mathUtils.prob(pierceChance))) {
                engine.spawnEmpArcPierceShields(
                    weapon.ship,
                    to,
                    weapon.ship,
                    target,
                    beam.damage.type,
                    beam.damage.damage * DAMAGE_MULT,
                    beam.damage.fluxComponent * EMP_MULT,
                    999999f,
                    null,
                    beam.width * 0.5f,
                    beam.fringeColor,
                    beam.coreColor
                )
            }
        }
    }

}