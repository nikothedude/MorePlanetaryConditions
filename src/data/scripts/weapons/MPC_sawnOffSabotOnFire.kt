package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.ai.missile.MirvAI
import com.fs.starfarer.combat.ai.missile.MissileAI
import com.fs.starfarer.combat.entities.BaseEntity
import data.utilities.niko_MPC_reflectionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

class MPC_sawnOffSabotOnFire: OnFireEffectPlugin {
    override fun onFire(
        projectile: DamagingProjectileAPI?,
        weapon: WeaponAPI?,
        engine: CombatEngineAPI?
    ) {
        if (projectile == null || engine == null) return

        if (projectile !is MissileAPI) return
        if (projectile.missileAI !is MirvAI) return
        val mirv = projectile.missileAI as MirvAI

        val enemy = if (projectile.owner == 0) 1 else 0
        val manager = engine.getFleetManager(enemy)
        val old = manager.isSuppressDeploymentMessages
        manager.isSuppressDeploymentMessages = true

        val targetLoc = MathUtils.getPointOnCircumference(projectile.location, 20f, projectile.facing)
        val drone = manager.spawnShipOrWing("wasp_Interceptor", targetLoc, 0f) as BaseEntity
        drone.setWasRemoved(false)

        manager.isSuppressDeploymentMessages = old

        mirv.target = drone
        val ai = niko_MPC_reflectionUtils.get("steeringAI", mirv) as MissileAI
        ai.target = drone
        mirv.checkSplit(1f)

        engine.removeEntity(drone)
        drone.setWasRemoved(true)
        manager.removeDeployed(drone as ShipAPI, false)
    }
}