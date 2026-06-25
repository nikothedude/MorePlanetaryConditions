package data.scripts.ghosts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes.PATROL_SMALL
import com.fs.starfarer.api.impl.campaign.ids.MemFlags.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.ai.ModularFleetAI
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_debugUtils
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import kotlin.math.max

class MPC_ambushFleetScript(
    val fleet: CampaignFleetAPI,
    val target: CampaignFleetAPI,
    val idealDist: Float,
    val idealAngle: Float
): niko_MPC_baseNikoScript() {
    companion object {
        fun canDoAmbush(target: CampaignFleetAPI): Boolean {
            if (!target.isInHyperspace) return false
            if (Misc.getHyperspaceTerrainPlugin().isInAbyss(target)) return false
            // TODO something here

            return true
        }

        fun createNewEncounter(target: CampaignFleetAPI) {
            if (!canDoAmbush(target))
                return

            val viableFactions = Global.getSector().allFactions.filter { it.custom.optBoolean("pirateBehavior") }
            val hostileViableFactions = viableFactions.filter { it.isHostileTo(target.faction) }
            if (hostileViableFactions.isEmpty()) {
                niko_MPC_debugUtils.log.info("failed to find a viable faction for a ambush fleet, aborting")
                return
            }

            val pickedFac = hostileViableFactions.random()
            createFleet(createFleetParams(target, pickedFac), target)
        }

        fun createFleetParams(target: CampaignFleetAPI, faction: FactionAPI): FleetParamsV3 {
            val fp = target.fleetPoints * MathUtils.getRandomNumberInRange(0.7f, 1.3f)
            val source = faction.getMarketsCopy().randomOrNull()
            val params = FleetParamsV3(
                source,
                PATROL_SMALL,
                fp,
                MathUtils.getRandomNumberInRange(5f, 10f),
                MathUtils.getRandomNumberInRange(20f, 30f),
                0f,
                0f,
                3f,
                0f
            )
            params.averageSMods = 1

            return params
        }

        fun createFleet(params: FleetParamsV3, target: CampaignFleetAPI): CampaignFleetAPI {
            val fleet = FleetFactoryV3.createFleet(params)
            target.containingLocation.addEntity(fleet)

            val idealDist = MathUtils.getRandomNumberInRange(500f, 600f)
            val idealAngle = VectorUtils.getAngle(target.location, fleet.location)
            val newDest = MathUtils.getPointOnCircumference(target.location, idealDist, idealAngle)

            MPC_ambushFleetScript(fleet, target, idealDist, idealAngle).start()
            init(fleet, target)
            fleet.setLocation(newDest.x, newDest.y)
            return fleet
        }

        fun init(fleet: CampaignFleetAPI, target: CampaignFleetAPI) {
            fleet.detectedRangeMod.modifyMult(MOD_ID, SENSOR_PROFILE_MULT)
            fleet.stats.fleetwideMaxBurnMod.modifyMult(MOD_ID, 1.3f)
            fleet.sensorRangeMod.modifyMult(MOD_ID, 0f) // no blips

            //fleet.memoryWithoutUpdate[FLEET_IGNORED_BY_OTHER_FLEETS] = true
            fleet.memoryWithoutUpdate[FLEET_IGNORES_OTHER_FLEETS] = true
            fleet.memoryWithoutUpdate[MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
            fleet.memoryWithoutUpdate[MEMORY_KEY_FORCE_TRANSPONDER_OFF] = true
            fleet.memoryWithoutUpdate["\$MPC_radiantAmbushFleet"] = true
        }
        const val SENSOR_PROFILE_MULT = 0.1f
        const val MOD_ID = "MPC_ambushFleetScript"
        const val LY_CUTOFF = 10f
    }

    private var coverBlown = false
    val patienceTimer = IntervalUtil(60f, 65f)
    private val entity = fleet.containingLocation.addCustomEntity(
        null,
        null,
        Entities.SENSOR_GHOST,
        Factions.NEUTRAL
    )
    init {
        entity.isDiscoverable = true
        entity.sensorProfile = fleet.sensorProfile * (1/SENSOR_PROFILE_MULT)
        entity.discoveryXP = 0f
        entity.detectionRangeDetailsOverrideMult = -100f
        entity.radius = fleet.radius
        entity.forceSensorFaderOut()
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
        fleet.containingLocation.addEntity(entity)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
        fleet.containingLocation.removeEntity(entity)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!fleet.isAlive || fleet.containingLocation == null || fleet.isDespawning) {
            delete()
            return
        }

        handleECM(amount)
        handleOverlay(amount)
        handleBehavior(amount)
    }

    fun coverIsBlown() {
        coverBlown = true
        fleet.memoryWithoutUpdate[FLEET_IGNORES_OTHER_FLEETS] = false
        fleet.memoryWithoutUpdate[MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = false
        fleet.memoryWithoutUpdate[MEMORY_KEY_FORCE_TRANSPONDER_OFF] = false
        fleet.memoryWithoutUpdate["\$MPC_ambushFleetCoverBlown"] = true
        fleet.stats.fleetwideMaxBurnMod.unmodify(MOD_ID)
        fleet.sensorRangeMod.unmodify(MOD_ID)

        class AfterAssignmentScript(): Script {
            override fun run() {
                abort()
            }
        }

        val ai = target.ai as? ModularFleetAI
        ai?.tacticalModule?.target = target

        val ability = fleet.getAbility(Abilities.EMERGENCY_BURN)
        if (ability != null && !ability.isActiveOrInProgress) {
            ability.activate()
        }

        abort()
    }

    fun handleOverlay(amount: Float) {
        if (fleet.getVisibilityLevelTo(target) >= SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS) {
            entity.setLocation(999999f, 999999f)
        } else {
            entity.setLocation(fleet.location.x, fleet.location.y)
        }
    }

    fun handleECM(amount: Float) {
        val loc = fleet.containingLocation

        for (iterFleet in loc.fleets - fleet) {
            if (fleet.getVisibilityLevelTo(iterFleet) < SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS) {
                val ai = target.ai as? ModularFleetAI
                ai?.doNotAttack(fleet, amount)
            } else if (!coverBlown && iterFleet.isHostileTo(fleet)) {
                coverIsBlown()
            }
        }
    }

    fun handleBehavior(amount: Float) {
        if (fleet.currentAssignment?.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN || coverBlown) {
            return
        }

        patienceTimer.advance(Misc.getDays(amount))
        if (patienceTimer.intervalElapsed()) {
            coverIsBlown()
            return
        }

        val ai = fleet.ai as? ModularFleetAI ?: return
        val nav = ai.navModule

        val newDest = MathUtils.getPointOnCircumference(target.location, idealDist, idealAngle)
        nav.setDestination(newDest)

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.GO_TO_LOCATION,
            fleet.containingLocation.createToken(newDest),
            30f,
            "stalking your fleet",
            null
        )

        if ((Misc.getDistanceLY(fleet, target) > LY_CUTOFF) || fleet.containingLocation != target.containingLocation) {
            abort()
            return
        }
    }

    private fun abort() {
        fleet.clearAssignments()

        val fac = fleet.faction
        val randMarket = fac.getMarketsCopy().randomOrNull() ?: Global.getSector().economy.marketsCopy.random()

        fleet.addAssignmentAtStart(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            randMarket.primaryEntity,
            Float.MAX_VALUE,
            null
        )
    }

    // TODO player ability idea
    // ECM based sensor ghost thing
    // costs a lot of fuel to maintain
    // tracks the proximity of nearby fleets - if they would see our exact composition and fac, they will turn hostile
    // get it by defeating one of these fleets
    // fleets should probs be rare

    // impl of the ghost - tries to be a echo ghost, but if you get too close, or too much time passes, it eburns and breaks its cloak
}