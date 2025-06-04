package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.CountingMap
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI.Companion.objectiveWeight
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI.Companion.planetWeight
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI.Companion.sourceWeight
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript.Companion.getFractalColony
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getMarketsInLocation
import java.util.*
import kotlin.math.max

class MPC_fractalPatrolAssignmentAI(
    val fleet: CampaignFleetAPI
): niko_MPC_baseNikoScript(),
    FleetEventListener
{

    companion object {
        const val MIN_DAYS_TIL_END = 70f
        const val MAX_DAYS_TIL_END = 90f
    }
    val interval = IntervalUtil(MIN_DAYS_TIL_END, MIN_DAYS_TIL_END)

    init {
        giveInitialAssignments()
        fleet.addEventListener(this)
    }

    protected fun giveInitialAssignments() {
        refreshAssignments()
    }

    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            despawnFleet()
            return
        }
    }

    fun despawnFleet() {
        val token = fleet.containingLocation.createToken(9999999f, 9999999f)
        fleet.clearAssignments()
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            token,
            Float.MAX_VALUE,
            "shimmering"
        )
        Global.getSector().doHyperspaceTransition(fleet, null, JumpPointAPI.JumpDestination(token, null))

        delete()
    }

    protected fun refreshAssignments() {
        fleet.clearAssignments()
        val colony = getFractalColony() ?: return
        val colonyLoc = colony.containingLocation as? StarSystemAPI ?: return
        val loc = fleet.containingLocation

        val target = pickEntityToGuard(MathUtils.getRandom(), colonyLoc, fleet)

        class refresh(): Script {
            override fun run() {
                refreshAssignments()
            }
        }

        if (target == null) {
            fleet.addAssignment(
                FleetAssignment.PATROL_SYSTEM,
                null,
                MathUtils.getRandomNumberInRange(30f, 40f),
                "skirmishing",
                refresh()
            )
        }
        else if (target.market != null) {
            var actionText = "hovering around ${target.market.name}"
            if (target.market.faction.isHostileTo(fleet.faction)) {
                actionText = "skirmishing around ${target.market.name}"
            }

            class actOne(): Script {
                override fun run() {
                    fleet.clearAssignments()
                    fleet.addAssignment(
                        FleetAssignment.PATROL_SYSTEM,
                        target,
                        MathUtils.getRandomNumberInRange(30f, 40f),
                        actionText,
                        refresh()
                    )
                }
            }
            fleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION,
                target,
                Float.MAX_VALUE,
                "travelling",
                actOne()
            )
        } else if (target is CampaignFleetAPI) {
            fleet.addAssignment(
                FleetAssignment.DELIVER_CREW,
                target,
                MathUtils.getRandomNumberInRange(50f, 60f),
                "hunting",
                refresh()
            )
            target.addEventListener(this)
        } else {
            fleet.addAssignment(
                FleetAssignment.PATROL_SYSTEM,
                target,
                MathUtils.getRandomNumberInRange(20f, 30f),
                "guarding",
                refresh()
            )
        }

        /*val validMags = getValidMagFields()
        val picked = validMags.randomOrNull() ?: return
        val plugin = picked as MagneticFieldTerrainPlugin

        val distFromCenterRangeBottom = plugin.auroraInnerRadius
        val distFromCenterRangeCeil = plugin.auroraOuterRadius

        val*/ // toooo much work


    }

    private fun pickEntityToGuard(
        random: Random,
        system: StarSystemAPI,
        fleet: CampaignFleetAPI
    ): SectorEntityToken? {

        val colony = getFractalColony() ?: return null
        val colonyLoc = colony.containingLocation ?: return null
        val loc = fleet.containingLocation

        val picker = WeightedRandomPicker<SectorEntityToken>(random)

        picker.add(system.hyperspaceAnchor, 1f)
        picker.add(null, 0.5f)

        for (planet in system.planets) {
            if (planet.isStar) continue
            picker.add(planet, 0.5f)
        }

        for (entity in system.jumpPoints) {
            picker.add(entity, 1f)
        }

        for (entity in system.getEntitiesWithTag(Tags.OBJECTIVE)) {
            picker.add(entity, 1f)
        }

        for (fleet in system.fleets.filter { fleet.isHostileTo(it) }) {
            picker.add(fleet, 0.25f) // it just has a second sense
        }

        return picker.pick()
    }

    fun getValidMagFields(): MutableSet<BaseRingTerrain> {
        val fields = HashSet<BaseRingTerrain>()
        val colony = getFractalColony() ?: return fields
        val colonyLoc = colony.containingLocation as? StarSystemAPI ?: return fields

        val maxDist = getMaxDistanceForStalk()
        for (terrain in colonyLoc.terrainCopy.filter { it.plugin is MagneticFieldTerrainPlugin }) {
            if (MathUtils.getDistance(terrain, colonyLoc.center) <= maxDist) {
                fields += terrain.plugin as BaseRingTerrain
            }
        }

        return fields
    }

    fun getMaxDistanceForStalk(): Float {
        val loc = getFractalColony()?.containingLocation as? StarSystemAPI ?: return 0f

        val furthestMarket: MarketAPI? = loc.getMarketsInLocation(Global.getSector().playerFaction.id).maxByOrNull { MathUtils.getDistance(it.primaryEntity, loc.center) } ?: return 0f
        return MathUtils.getDistance(furthestMarket!!.primaryEntity, loc.center) * 1.2f
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (fleet == this.fleet) {
            delete()
            return
        }

        if (fleet.assignmentsCopy.firstOrNull()?.target == fleet) {
            refreshAssignments()
        }
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        return
    }
}