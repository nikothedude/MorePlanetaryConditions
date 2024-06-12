package data.scripts.campaign

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure.Companion.coronaResistance
import data.scripts.campaign.objectives.MPC_baryonEmitterObjectiveScript
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_fleetUtils.counterTerrainMovement
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_industryIds

open class MPC_coronaResistScript(
    val entity: SectorEntityToken,
): niko_MPC_baseNikoScript() {
    open var terrainMovementDivisor: Float = 40f
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
        fleet.counterTerrainMovement(days, terrainMovementDivisor) // this doesnt seem to work very well either, its inconsistant between fleets

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

    companion object {
        fun interferenceDetected(location: LocationAPI): Boolean {
            return getScriptsInLocation(location).isNotEmpty()
        }

        private fun getScriptsInLocation(location: LocationAPI): MutableSet<MPC_coronaResistScript> {
            val scripts = HashSet<MPC_coronaResistScript>()

            for (iterMarket in Misc.getMarketsInLocation(location)) {
                val industry = iterMarket.getIndustry(niko_MPC_industryIds.coronaResistIndustry) as? MPC_coronaResistStructure ?: continue

                scripts += industry.script ?: continue
            }
            for (objective in location.getEntitiesWithTag(niko_MPC_ids.BARYON_EMITTER_TAG)) {
                val plugin = objective.customPlugin as? MPC_baryonEmitterObjectiveScript ?: continue
                scripts += plugin.script ?: continue
            }
            return scripts
        }
    }

}