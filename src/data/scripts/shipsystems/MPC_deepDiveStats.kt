package data.scripts.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
import com.fs.starfarer.api.impl.hullmods.AdaptivePhaseCoils
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_settings
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

class MPC_deepDiveStats: BaseShipSystemScript() {

    companion object {
        const val MAX_TIME_MULT = 20f
        const val CLOAK_GEN_MULT = 0f
        const val SPEED_MULT = 2f
    }

    override fun isUsable(system: ShipSystemAPI?, ship: ShipAPI?): Boolean {
        if (ship == null) return false
        if (ship.isPhased) {
            return true
        }
        return false
    }

    override fun getInfoText(system: ShipSystemAPI?, ship: ShipAPI?): String? {
        if (ship?.isPhased != true) return "Must be phased"
        if (system == null) return null

        return null
    }

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {

        super.apply(stats, id, state, effectLevel)

        if (id == null || stats == null) return

        val ship = stats.entity as? ShipAPI ?: return
        if (ship.customData["MPC_didDiveExplosion"] == true) {
            if (!ship.isPhased) {
                ship.system.deactivate()
                return
            }
        }
        val timeMult = MAX_TIME_MULT * effectLevel
        stats.timeMult.modifyMult(id, timeMult)
        if (Global.getCombatEngine().playerShip == ship) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / timeMult)
        }
        stats.phaseCloakUpkeepCostBonus.modifyMult(id, CLOAK_GEN_MULT * effectLevel)
        stats.maxSpeed.modifyMult(id, SPEED_MULT)
        stats.acceleration.modifyMult(id, SPEED_MULT)
        stats.deceleration.modifyMult(id, SPEED_MULT)
        stats.turnAcceleration.modifyMult(id, SPEED_MULT)
        stats.maxTurnRate.modifyMult(id, SPEED_MULT)
        stats.dynamic.getMod(
            Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD
        ).modifyPercent(id, Float.MAX_VALUE)

        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)

        if (ship.customData["MPC_didDiveExplosion"] != true) {
            val fader = MPC_diveFader(0.05f, ship)
            Global.getCombatEngine().addPlugin(fader)

            ship.setCustomData("MPC_didDiveExplosion", true)

            val p = RiftCascadeMineExplosion.createStandardRiftParams(
                Color(220, 43, 181, 221),
                ship.shieldRadiusEvenIfNoShield * 0.8f
            )
            p.fadeOut = 0.35f
            p.hitGlowSizeMult = 0.25f
            p.underglow = Color(255, 175, 255, 50)
            p.withHitGlow = false
            p.noiseMag = 1.25f

            val e = Global.getCombatEngine().addLayeredRenderingPlugin(NegativeExplosionVisual(p))
            e.location.set(ship.location)

            if (niko_MPC_settings.graphicsLibEnabled) {
                val ripple = RippleDistortion(ship.location, ship.velocity)
                ripple.intensity = ship.collisionRadius * 0.75f
                ripple.size = ship.shieldRadiusEvenIfNoShield
                ripple.fadeInSize(0.15f)
                ripple.fadeOutIntensity(0.5f)
                DistortionShader.addDistortion(ripple)
            }
        }

        if (ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_IN_GOOD_SPOT) && !ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) {
            val target = ship.aiFlags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) as? ShipAPI
            if (target != null) {
                val minDist = target.collisionRadius * 0.8f
                val dist = MathUtils.getDistance(ship, target)

                if (dist >= minDist) {
                    ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0) // deactivate
                    return
                }
            }
        }
        if (ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)) {
            ship.aiFlags.setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 0f)
        } else {
            ship.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 0f)
        }
        if (ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)) {
            if (!ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) {
                ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0) // deactivate
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        super.unapply(stats, id)

        if (stats == null || id == null) return

        stats.timeMult.unmodify(id)
        stats.phaseCloakUpkeepCostBonus.unmodify(id)
        stats.maxSpeed.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.dynamic.getMod(
            Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD
        ).unmodify(id)
        Global.getCombatEngine().timeMult.unmodify(id)
        val ship = stats.entity as? ShipAPI ?: return
        ship.isDefenseDisabled = false

        val fader = MPC_diveFader(0.1f, ship, true)
        Global.getCombatEngine().addPlugin(fader)

        if (ship.customData["MPC_didDiveExplosion"] == true) {
            val p = RiftCascadeMineExplosion.createStandardRiftParams(
                Color(220, 43, 181, 221),
                ship.shieldRadiusEvenIfNoShield * 0.8f
            )
            p.fadeOut = 0.35f
            p.hitGlowSizeMult = 0.25f
            p.underglow = Color(255, 175, 255, 50)
            p.withHitGlow = false
            p.noiseMag = 1.25f

            val e = Global.getCombatEngine().addLayeredRenderingPlugin(NegativeExplosionVisual(p))
            e.location.set(ship.location)

            if (niko_MPC_settings.graphicsLibEnabled) {
                val ripple = RippleDistortion(ship.location, ship.velocity)
                ripple.intensity = ship.collisionRadius * 0.75f
                ripple.size = ship.shieldRadiusEvenIfNoShield
                ripple.fadeInSize(0.15f)
                ripple.fadeOutIntensity(0.5f)
                DistortionShader.addDistortion(ripple)
            }
            val sound = ship.system.specAPI.useSound ?: return
            Global.getSoundPlayer().playSound(
                sound,
                1f,
                1f,
                ship.location,
                Misc.ZERO
            )
        }

        ship.setCustomData("MPC_didDiveExplosion", false)
    }

    class MPC_diveFader(
        val time: Float,
        val ship: ShipAPI,
        val invert: Boolean = false
    ): BaseEveryFrameCombatPlugin() {
        var timeLeft = time

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (!invert && !ship.system.isActive) {
                Global.getCombatEngine().removePlugin(this)
                return
            }
            timeLeft -= amount
            var delta = (timeLeft / time).coerceAtLeast(0f)
            if (invert) {
                delta = 1f - delta
            }
            ship.extraAlphaMult2 = delta
            if (timeLeft <= 0f) {
                Global.getCombatEngine().removePlugin(this)
            }
        }

    }

    override fun getStatusData(
        index: Int,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ): ShipSystemStatsScript.StatusData? {
        if (index == 3) {
            return ShipSystemStatsScript.StatusData(
                "In deep phase-space", false
            )
        }
        if (index == 2) {
            return ShipSystemStatsScript.StatusData(
                "Timeflow increased by ${MAX_TIME_MULT.toInt()}x", false
            )
        }
        if (index == 1) {
            return ShipSystemStatsScript.StatusData(
                "Engine performance enhanced", false
            )
        }
        return null
    }

}

