package data.scripts.campaign

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure
import data.scripts.campaign.objectives.MPC_baryonEmitterObjectiveScript
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_fleetUtils.counterTerrainMovement
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.CORONA_RESIST_MEMORY_FLAG
import data.utilities.niko_MPC_industryIds
import com.fs.starfarer.api.util.IntervalUtil

open class MPC_coronaResistScript(val entity: SectorEntityToken,): niko_MPC_baseNikoScript() {
    var coronaResistance: Float = 0.0f
    open var terrainMovementDivisor: Float = 40f
    private val affecting: MutableSet<FleetMemberAPI> = HashSet()
    var interval = IntervalUtil(1f, 1f);

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

    private fun iterateThroughAffected(){
        val iter = affecting.iterator()
        val fleetBin: MutableSet<CampaignFleetAPI> = HashSet()

        while (iter.hasNext()){
            val it = iter.next()
            var shouldRemove=false
            if((it.fleetData==null)||(it.fleetData.fleet==null)){
                shouldRemove=true
            }
            else if (!shouldAffectFleet((it.fleetData.fleet))){
                if (!fleetBin.contains(it.fleetData.fleet)){
                    fleetBin += it.fleetData.fleet
                }
                shouldRemove=true
            }
            if(shouldRemove){
                unAffectFleetMember(it)
                iter.remove()
            }
        }

        val iter2=fleetBin.iterator()
        while (iter2.hasNext()) {
            val it = iter2.next()
            it.memoryWithoutUpdate?.unset(CORONA_RESIST_MEMORY_FLAG)
            iter2.remove()
        }
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        if (interval==null){interval = IntervalUtil(1f, 1f);}

        interval.advance(days)
        if(interval.intervalElapsed()) {
            iterateThroughAffected()
        }
        interateThroughFleets(days)
    }

    open fun unapplyToFleets() {
        val iter = affecting.iterator()
        val fleetBin: MutableSet<CampaignFleetAPI> = HashSet()
        while (iter.hasNext()){
            val it = iter.next()
            if (((it.fleetData!=null)&&(it.fleetData.fleet != null))&&(!fleetBin.contains(it.fleetData.fleet))) {
                fleetBin.add(it.fleetData.fleet)
            }
            unAffectFleetMember(it)
            iter.remove()
        }
        affecting.clear()
        val iter2=fleetBin.iterator()
        while (iter2.hasNext()) {
            val it = iter2.next()
            it.memoryWithoutUpdate?.unset(CORONA_RESIST_MEMORY_FLAG)
        }
    }

    private fun interateThroughFleets(days: Float) {
        for (fleet in getTargetFleets()) {
            if (!shouldAffectFleet(fleet)) continue
            affectFleet(fleet, days)
        }
    }

    private fun affectFleetMember(member: FleetMemberAPI){
        member.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(
            "${entity.id}_MPCspecialCoronaResistance",
            coronaResistance,
            "Unknown corona resistance"
        )
    }

    private fun unAffectFleetMember(member: FleetMemberAPI){
        member.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).unmodify("${entity.id}_MPCspecialCoronaResistance")
    }

    private fun affectFleet(fleet: CampaignFleetAPI, days: Float) {
        fleet.counterTerrainMovement(days, terrainMovementDivisor) // this doesnt seem to work very well either, its inconsistant between fleets
        fleet.memoryWithoutUpdate.set(CORONA_RESIST_MEMORY_FLAG, coronaResistance, 1f)

        for (fleetMember in fleet.fleetData.membersListCopy) {
            if (!affecting.contains(fleetMember)){
                affectFleetMember(fleetMember)
                affecting.add(fleetMember)
            }
        }
    }

    open fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        return !fleet.isInHyperspaceTransition
    }

    open fun getTargetFleets(): MutableSet<CampaignFleetAPI> {
        val containingLocation = entity.containingLocation

        return containingLocation.fleets.toMutableSet()
    }

    companion object {
        fun interferenceDetected(location: LocationAPI): Boolean {
            return getScriptsInLocation(location).isNotEmpty()
        }

        fun getScriptsInLocation(location: LocationAPI): MutableSet<MPC_coronaResistScript> {
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