package data.scripts.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.IntervalUtil
import data.utilities.niko_MPC_mathUtils.prob
import lunalib.lunaUtil.LunaCommons
import niko.MCTE.utils.MCTE_shipUtils.isTangible
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion

class MPC_tachyonPikeScript: BeamEffectPlugin {
    val interval = IntervalUtil(0.02f, 0.02f)
    var didRipple = false

    override fun advance(amount: Float, engine: CombatEngineAPI?, beam: BeamAPI?) {
        if (beam == null) return
        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        val target: CombatEntityAPI? = beam.damageTarget
        val point = beam.to
        val weapon = beam.weapon ?: return

        val ship = weapon.ship ?: return
        if (ship.isPhased) return
        if (weapon.chargeLevel >= 0.95f) {

            if (!didRipple) {
                didRipple = true

                val ripple = RippleDistortion(weapon.location, ship.velocity)
                ripple.intensity = 100f
                ripple.size = 200f
                ripple.fadeInSize(0.25f)
                ripple.fadeOutIntensity(0.5f)
                DistortionShader.addDistortion(ripple)
            }

            beam.width = (beam.width * 1.1f).coerceAtMost(150f)
            if (prob(70)) {

                engine?.spawnEmpArcVisual(
                    weapon.location,
                    ship,
                    point,
                    target,
                    beam.width * 0.5f,
                    beam.fringeColor,
                    beam.coreColor
                )

                if (target is CombatEntityAPI) {
                    val hitShield = target.shield != null && target.shield.isWithinArc(beam.to)
                    var pierceChance = if (target is ShipAPI) target.hardFluxLevel - 0.1f else 0f
                    pierceChance *= ship.mutableStats.dynamic.getValue(Stats.SHIELD_PIERCED_MULT)
                    val piercedShield = hitShield && Math.random().toFloat() < pierceChance

                    val dam = beam.damage.damage * 0.15f
                    val emp = beam.damage.fluxComponent * 0.35f
                    if (piercedShield) {
                        engine?.spawnEmpArcPierceShields(
                            weapon.ship,
                            point,
                            target,
                            target,
                            DamageType.ENERGY,
                            dam,
                            emp,
                            Float.MAX_VALUE,
                            "tachyon_lance_emp_impact",
                            beam.width * 0.7f,
                            beam.fringeColor,
                            beam.coreColor
                        )
                    } else {
                        engine?.spawnEmpArc(
                            weapon.ship,
                            point,
                            target,
                            target,
                            DamageType.ENERGY,
                            dam,
                            emp,
                            Float.MAX_VALUE,
                            "tachyon_lance_emp_impact",
                            beam.width * 0.9f,
                            beam.fringeColor,
                            beam.coreColor
                        )
                    }
                }
            }
        }
    }

}