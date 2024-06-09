package data.scripts.campaign

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure.Companion.coronaResistance
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_fleetUtils.approximateCounterVelocityOfTerrain
import data.utilities.niko_MPC_fleetUtils.counterTerrainMovement
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.UNDER_CORONA_RESIST_EFFECT
import data.utilities.niko_MPC_miscUtils
import data.utilities.niko_MPC_reflectionUtils
import data.utilities.niko_MPC_reflectionUtils.get

open class MPC_coronaResistScript(
    val entity: SectorEntityToken,
): niko_MPC_baseNikoScript() {
    val affecting: MutableSet<FleetMemberAPI> = HashSet()

    override fun startImpl() {
        entity.addScript(this)
    }

    override fun stopImpl() {
        entity.removeScript(this)
        unapplyToFleets()
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)

        unapplyToFleets()

        interateThroughFleets(days)
    }

    open fun unapplyToFleets() {
        for (member in affecting) {
            member.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).unmodify("${entity.id}_coronaEffect")
        }
        affecting.clear()
    }

    private fun interateThroughFleets(days: Float) {
        for (fleet in getTargetFleets()) {
            if (!shouldAffectFleet(fleet)) continue
            affectFleet(fleet, days)
        }
    }

    private fun affectFleet(fleet: CampaignFleetAPI, days: Float) {
        fleet.counterTerrainMovement(days) // this doesnt seem to work very well either, its inconsistant between fleets

        for (fleetMember in fleet.fleetData.membersListCopy) {
            fleetMember.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(
                "${entity.id}_MPCspecialCoronaResistance",
                coronaResistance,
                "Unknown corona resistance"
            )
            affecting += fleetMember
        }
    }

    open fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        if (fleet.isInHyperspaceTransition) return false

        return true
    }

    open fun getTargetFleets(): MutableSet<CampaignFleetAPI> {
        val containingLocation = entity.containingLocation

        return containingLocation.fleets.toMutableSet()
    }

}