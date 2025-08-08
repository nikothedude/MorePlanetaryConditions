package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import data.scripts.weapons.MPC_shieldMissileScript.MPC_shieldMissileEveryframeScript.Companion.updateDronePos
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.PI
import kotlin.math.ceil

abstract class MPC_shieldMissileScript: OnFireEffectPlugin {

    abstract val id: String
    open val variantId: String = "MPC_shield_drone_Shield"

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI) {
        if (projectile == null) return

        createShieldDrone(projectile, engine)
    }

    open fun createShieldDrone(projectile: DamagingProjectileAPI, engine: CombatEngineAPI): ShipAPI {
        val fleetManager = engine.getFleetManager(projectile.owner)
        val oldSuppress = fleetManager.isSuppressDeploymentMessages
        fleetManager.isSuppressDeploymentMessages = true
        val drone = fleetManager.spawnShipOrWing(variantId, projectile.location, 0f) // we update its movement later
        drone.isAlly = true
        drone.isHoldFire = true
        fleetManager.isSuppressDeploymentMessages = oldSuppress

        drone.mutableStats.engineDamageTakenMult.modifyMult(id, 0f)
        drone.mutableStats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, 0f)
        drone.mutableStats.hullDamageTakenMult.modifyMult(id, 0f) // cant kill it

        drone.isRenderEngines = false
        drone.isDoNotRenderSprite = true
        drone.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)

        drone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)

        drone.collisionClass = CollisionClass.FIGHTER

        val shield = createShieldAndFlux(drone, projectile)
        if (projectile is MissileAPI) {
            val empResist = ceil((shield.radius * 0.1f)).toInt()
            projectile.empResistance = empResist
        }

        updateDronePos(drone, projectile)
        val script = MPC_shieldMissileEveryframeScript(drone, projectile)
        engine.addPlugin(script)
        return drone
    }

    fun createShieldAndFlux(drone: ShipAPI, projectile: DamagingProjectileAPI): ShieldAPI {
        drone.setShield(
            getShieldType(),
            getShieldUpkeep(),
            getShieldEff(),
            getShieldArc(),
        )
        drone.mutableStats.fluxCapacity.modifyFlat(id, getFluxCapacity())
        drone.mutableStats.fluxDissipation.modifyFlat(id, getFluxDissipation())
        drone.mutableStats.hardFluxDissipationFraction.modifyFlat(id, getHardFluxFraction())
        drone.mutableStats.overloadTimeMod.modifyMult(id, getOverloadTimeMult())

        drone.shield.radius = projectile.collisionRadius * 5f
        drone.collisionRadius = drone.shield.radius * 1.2f

        return drone.shield
    }

    abstract fun getShieldType(): ShieldAPI.ShieldType
    open fun getShieldUpkeep(): Float = 0f
    open fun getShieldEff(): Float = 1f
    abstract fun getShieldArc(): Float

    abstract fun getFluxCapacity(): Float
    abstract fun getFluxDissipation(): Float
    open fun getHardFluxFraction(): Float = 0f
    open fun getOverloadTimeMult(): Float = 1f

    open class MPC_shieldMissileEveryframeScript(
        val drone: ShipAPI,
        val projectile: DamagingProjectileAPI
    ): BaseEveryFrameCombatPlugin() {

        var storedHP = 0f

        companion object {
            fun updateDronePos(drone: ShipAPI, projectile: DamagingProjectileAPI) {
                val newPos = getTargetDronePos(drone, projectile)
                drone.location.set(newPos)
                drone.facing = projectile.facing
            }

            fun getTargetDronePos(drone: ShipAPI, projectile: DamagingProjectileAPI): Vector2f {
                val projLocCopy = Vector2f(projectile.location)
                return projLocCopy
            }
        }

        val checkInterval = IntervalUtil(0f, 0f) // seconds

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            checkInterval.advance(amount)
            if (checkInterval.intervalElapsed()) {

                if (drone.shield.activeArc >= 360f) { // so we can be SURE it wont die with a bubble shield
                    storedHP = projectile.maxHitpoints
                    projectile.hitpoints = Float.MAX_VALUE
                } else if (storedHP > 0f) {
                    projectile.hitpoints = storedHP
                    storedHP = 0f
                }

                updateDronePos(drone, projectile)
                if (projectile.isFading) {
                    drone.aiFlags.unsetFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)
                    drone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS)
                    drone.shield.toggleOff()
                } else {
                    drone.shield.toggleOn() // idk why but its super hesitant to do this otherwise, even with the ai flag
                }

                if (projectile is MissileAPI) {
                    if (projectile.isFizzling) {
                        drone.shield.toggleOff()
                        drone.aiFlags.unsetFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)
                        drone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS)
                    }
                }

                if (projectile.isExpired || !Global.getCombatEngine().isEntityInPlay(projectile)) {
                    delete()
                    return
                }
            }
        }

        fun delete() {
            Global.getCombatEngine().removePlugin(this)
            Global.getCombatEngine().removeEntity(drone)
        }
    }
}