package data.scripts.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.impl.combat.DamperFieldStats
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import java.awt.Color
import kotlin.math.roundToInt

class niko_MPC_superheavyDamperSystemStats : BaseShipSystemScript() {

    val damageTakenMult = 0.1f
    val weaponSpeedMult = 0.5f
    val STATUSKEY1 = Any()

    override fun apply(stats: MutableShipStatsAPI?, id: String?, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        super.apply(stats, id, state, effectLevel)


        if (stats == null) return
        if (id == null) return
        stats.hullDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
        stats.armorDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
        stats.empDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
        stats.ballisticRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)
        stats.energyRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)
        stats.missileRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)

        val ship: CombatEntityAPI = stats.entity
        if (ship is ShipAPI) {
            val system = ship.system
            val systemSpec = system.specAPI

            /*for (module in ship.childModulesCopy) {
                if (module.isHulk) continue
                val moduleStats = module.mutableStats
                moduleStats.hullDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
                moduleStats.armorDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
                moduleStats.empDamageTakenMult.modifyMult(id, 1f - (1f - damageTakenMult) * effectLevel)
                /*moduleStats.ballisticRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)
                moduleStats.energyRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)
                moduleStats.missileRoFMult.modifyMult(id, 1f - (1f - weaponSpeedMult) * effectLevel)*/

                module.setJitterUnder(id, systemSpec.jitterUnderEffectColor, 1f, systemSpec.jitterUnderCopies, systemSpec.jitterUnderMinRange, systemSpec.jitterUnderRange)
                module.setJitter(id, systemSpec.jitterEffectColor, 1f, systemSpec.jitterCopies, systemSpec.jitterMinRange, systemSpec.jitterRange)
            }*/

            val player: Boolean = stats.entity === Global.getCombatEngine().playerShip
            if (player) {
                val system = DamperFieldStats.getDamper(ship)
                if (system != null) {
                    val percent: Float = (1f - damageTakenMult) * effectLevel * 100
                    Global.getCombatEngine().maintainStatusForPlayerShip(
                        STATUSKEY1,
                        system.specAPI.iconSpriteName, system.displayName,
                        "${percent.roundToInt()} % less damage taken", false
                    )
                }
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        super.unapply(stats, id)

        if (stats == null || id == null) return
        stats.hullDamageTakenMult.unmodifyMult(id)
        stats.armorDamageTakenMult.unmodifyMult(id)
        stats.empDamageTakenMult.unmodifyMult(id)

        stats.ballisticRoFMult.unmodifyMult(id)
        stats.energyRoFMult.unmodifyMult(id)
        stats.missileRoFMult.unmodifyMult(id)
    }
}