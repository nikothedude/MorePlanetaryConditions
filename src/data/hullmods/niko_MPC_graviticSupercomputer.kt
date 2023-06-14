package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.*

class niko_MPC_graviticSupercomputer : BaseHullMod() {
    val rangeMult = 3f
    val pdRangeMult = 1f
    val visionIncrement = 10000f
    val recoilMult = 0.3f
    val autofireMult = 1f

    val maxRecoilMult = 0.05f
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.ballisticWeaponRangeBonus.modifyMult(id, rangeMult)
        stats.energyWeaponRangeBonus.modifyMult(id, rangeMult)
        stats.missileWeaponRangeBonus.modifyMult(id, (rangeMult))
        stats.nonBeamPDWeaponRangeBonus.modifyMult(id, pdRangeMult)
        stats.sightRadiusMod.modifyFlat(id, visionIncrement)
        stats.recoilPerShotMult.modifyMult(id, recoilMult)
        stats.recoilDecayMult.modifyMult(id, recoilMult)
        stats.maxRecoilMult.modifyMult(id, maxRecoilMult)
        stats.autofireAimAccuracy.modifyFlat(id, autofireMult)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + Math.round(rangeMult) + "00" + "%" else if (index == 1) return "" + Math.round(
            recoilMult
        ) + "00" + "%"
        return null
    }
}