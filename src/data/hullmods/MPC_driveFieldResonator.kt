package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.lazywizard.lazylib.combat.AIUtils
import org.magiclib.kotlin.interruptAbilitiesWithTag

class MPC_driveFieldResonator: BaseHullMod() {

    companion object{
        const val SUSTAINED_BURN_BONUS = 1f
        const val BURN_BONUS = 1f
        const val SENSOR_PROFILE = 100f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI, id: String?) {
        stats.dynamic.getMod(Stats.FLEET_BURN_BONUS).modifyFlat(id, BURN_BONUS)
        stats.sensorProfile.modifyFlat(id, SENSOR_PROFILE)
    }

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        super.advanceInCampaign(member, amount)
        if (member == null) return
        val fleet = member.fleetData?.fleet ?: return
        if (fleet.abilities.values.any { it.isActive && it.spec.hasTag(Abilities.TAG_BURN + "+") }) {
            //member.stats.dynamic.getMod(Stats.FLEET_BURN_BONUS).modifyFlat(this.spec.id, SUSTAINED_BURN_BONUS)
            fleet.stats.fleetwideMaxBurnMod.modifyFlat(member.id + spec.id, SUSTAINED_BURN_BONUS, "Drive field resonator")
        } else {
            fleet.stats.fleetwideMaxBurnMod.unmodify(member.id + spec.id)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        if (index == 0) return "" + BURN_BONUS.toInt()
        if (index == 1) return "" + SUSTAINED_BURN_BONUS.toInt()
        return if (index == 2) "" + SENSOR_PROFILE.toInt() else null
    }
}