package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.skills.MPC_planetaryOperations.Companion.LEVEL_1_BONUS
import data.scripts.campaign.skills.MPC_planetaryOperations.Companion.STABILITY_BONUS

class MPC_immortalSnailSkill {
    class Level1 : ShipSkillEffect {

        override fun apply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?,
            level: Float
        ) {
            if (stats == null) return

            stats.timeMult.modifyFlat(id, 15f)
            stats.peakCRDuration.modifyMult(id, 10f)
            stats.missileMaxSpeedBonus.modifyMult(id, 3f)
            stats.missileMaxTurnRateBonus.modifyMult(id, 2f)
            stats.missileAccelerationBonus.modifyMult(id, 3f)
            stats.missileTurnAccelerationBonus.modifyMult(id, 2f)
            stats.shieldArcBonus.modifyFlat(id, 360f)

            stats.missileAmmoRegenMult.modifyFlat(id, 5000f)
            stats.missileAmmoBonus.modifyFlat(id, 9559f)
            stats.damageToTargetHullMult.modifyFlat(id, 3f)
            stats.damageToTargetShieldsMult.modifyFlat(id, 3f)

            stats.peakCRDuration.modifyMult(id, 0.5f)
        }

        override fun unapply(
            stats: MutableShipStatsAPI?,
            hullSize: ShipAPI.HullSize?,
            id: String?
        ) {
            if (stats == null) return

            stats.timeMult.unmodify(id)
            stats.missileMaxSpeedBonus.unmodify(id)
            stats.missileMaxTurnRateBonus.unmodify(id)
            stats.missileAccelerationBonus.unmodify(id)
            stats.missileTurnAccelerationBonus.unmodify(id)
            stats.shieldArcBonus.unmodify(id)

            stats.peakCRDuration.unmodify(id)
        }

        override fun getEffectDescription(level: Float): String? {
            return "u shopuldnt see this"
        }

        override fun getEffectPerLevelDescription(): String? {
            return "u shopuldnt see this"
        }

        override fun getScopeDescription(): ScopeDescription? {
            return ScopeDescription.PILOTED_SHIP
        }
    }
}