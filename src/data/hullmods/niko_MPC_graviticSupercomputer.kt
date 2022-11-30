package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize

class niko_MPC_graviticSupercomputer : BaseHullMod() {
    val rangeMult = 10.5f
    val pdRangeMult = 0.7f
    val missileRangeMalice = 0.7f
    val visionIncrement = 9000f
    val recoilMult = 0.3f
    val accuracyMult = 0.5f
    val autofireMult = 1f
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.ballisticWeaponRangeBonus.modifyMult(id, rangeMult)
        stats.energyWeaponRangeBonus.modifyMult(id, rangeMult)
        stats.missileWeaponRangeBonus.modifyMult(id, (rangeMult * missileRangeMalice))
        stats.nonBeamPDWeaponRangeBonus.modifyMult(id, pdRangeMult)
        stats.sightRadiusMod.modifyFlat(id, visionIncrement)
        stats.recoilPerShotMult.modifyMult(id, recoilMult)
        stats.recoilDecayMult.modifyMult(id, recoilMult)
        stats.maxRecoilMult.modifyMult(id, accuracyMult)
        stats.autofireAimAccuracy.modifyFlat(id, autofireMult)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + Math.round(rangeMult) + "00" + "%" else if (index == 1) return "" + Math.round(
            recoilMult
        ) + "00" + "%"
        return null
    }
}