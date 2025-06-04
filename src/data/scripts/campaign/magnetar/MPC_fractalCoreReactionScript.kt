package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.MPC_overgrownNanoforgeExpeditionAssignmentAI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.skills.MPC_routingOptimization
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.makeImportant

class MPC_fractalCoreReactionScript: niko_MPC_baseNikoScript(), FleetEventListener {

    var fleets = HashSet<CampaignFleetAPI>()
        get() {
            if (field == null) field = HashSet<CampaignFleetAPI>()
            return field
        }
    var destroyedFleets: Int = 0
        get() {
            if (field == null) field = 0
            return field
        }
    var destroyedFleetClearInterval = IntervalUtil(30f, 39f) // days
        get() {
            if (field == null) field = IntervalUtil(30f, 30f)
            return field
        }
    companion object {
        const val DAYS_TIL_HEGE_FLEET = 30f
        const val BASE_INSPECTOR_FP = 150f

        const val BASE_PATROLS = 3
        const val PATROL_PER_COLONY_LEVEL = 1

        fun trySpawnHegeFleet(colony: MarketAPI) {
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_hegePrivateInspectorsSpawned")) return
            val hege = Global.getSector().getFaction(Factions.HEGEMONY)
            if (hege.relToPlayer.isHostile) return

            val source = hege.getMarketsCopy().randomOrNull() ?: return

            val params = FleetParamsV3(
                source,
                FleetTypes.PATROL_SMALL,
                BASE_INSPECTOR_FP,
                5f,
                10f,
                10f,
                0f,
                10f,
                0f
            )
            params.officerLevelLimit = 7
            params.officerLevelBonus = 1
            val fleet = FleetFactoryV3.createFleet(params)

            source.containingLocation.addEntity(fleet)
            fleet.containingLocation = source.containingLocation
            val primaryEntityLoc = source.primaryEntity.location
            fleet.setLocation(primaryEntityLoc.x, primaryEntityLoc.y)
            val facingToUse = MathUtils.getRandomNumberInRange(0f, 360f)
            fleet.facing = facingToUse

            fleet.name = "Private Investigators"

            fleet.makeImportant("\$MPC_hegePrivateInspectors")

            fleet.memoryWithoutUpdate["\$MPC_hegePrivateInspectors"] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_LOW_REP_IMPACT] = true

            MPC_privateInvestigatorAssignmentAI(fleet, colony, source).start()

            Global.getSector().memoryWithoutUpdate["\$MPC_hegePrivateInspectorsSpawned"] = true
        }

        fun getFractalColony(): MarketAPI? = MPC_hegemonyFractalCoreCause.getFractalColony()
    }

    val checkInterval = IntervalUtil(3f, 3.2f) // days

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {
            checkReaction()

            checkRouting()

            checkFractalPatrols()
        }

        if (destroyedFleets > 0) {
            destroyedFleetClearInterval.advance(days)

            if (destroyedFleetClearInterval.intervalElapsed()) {
                destroyedFleets--
            }
        } else {
            destroyedFleetClearInterval.elapsed = 0f
        }
    }

    private fun checkFractalPatrols() {
        val colony = getFractalColony()
        if (colony == null) {
            fleets.forEach { fleet ->
                val assignmentAI = fleet.memoryWithoutUpdate["\$MPC_fractalPatrolAssignmentAI"] as? MPC_fractalPatrolAssignmentAI
                assignmentAI?.despawnFleet()
            }
            fleets.clear()
        }
        if (canSpawnPatrols()) {
            spawnPatrols()
        }
    }

    private fun spawnPatrols() {
        val max = getMaxPatrols()
        while (fleets.size < max) {
            spawnPatrol()
        }
    }

    private fun spawnPatrol(): CampaignFleetAPI? {
        val colony = getFractalColony() ?: return null
        val loc = colony.containingLocation ?: return null
        val fleet = createPatrolFleet() ?: return null
        loc.addEntity(fleet)
        fleet.setLocation(999999f, 99999f)

        val radius = colony.primaryEntity.radius
        val randAngle = Misc.getUnitVectorAtDegreeAngle(MathUtils.getRandomNumberInRange(0f, 360f))

        val translation = randAngle.scale(radius + MathUtils.getRandomNumberInRange(50f, 200f)) as Vector2f
        val point = colony.primaryEntity.location.translate(translation.x, translation.y)
        Global.getSector().doHyperspaceTransition(fleet, null, JumpPointAPI.JumpDestination(loc.createToken(point), null))

        val AIscript = MPC_fractalPatrolAssignmentAI(fleet)
        fleet.memoryWithoutUpdate["\$MPC_fractalPatrolAssignmentAI"] = AIscript

        return fleet
    }

    private fun createPatrolFleet(): CampaignFleetAPI? {
        val colony = getFractalColony() ?: return null
        val loc = colony.containingLocation ?: return null
        val combat = MathUtils.getRandomNumberInRange(70f, 90f)

        val params = FleetParamsV3(
            Global.getSector().playerFleet.locationInHyperspace,
            Factions.OMEGA,
            null,
            FleetTypes.PATROL_SMALL,
            combat.toFloat(),  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f
        )
        params.ignoreMarketFleetSizeMult = true
        params.qualityOverride = 1.2f
        params.aiCores = OfficerQuality.AI_OMEGA
        params.doNotIntegrateAICores = false

        val fleet = FleetFactoryV3.createFleet(params)
        fleet.stats.fleetwideMaxBurnMod.modifyFlat("MPC_VERYFASTOMEGAFLEET", 900f)
        fleet.stats.accelerationMult.modifyMult("MPC_VERYFASTOMEGAFLEET", 4f)
        for (member in fleet.membersWithFightersCopy) {
            // to "perm" the variant so it gets saved and not recreated
            member.setVariant(member.variant.clone(), false, false)
            member.variant.source = VariantSource.REFIT
            member.variant.originalVariant = null
            member.variant.addTag(Tags.SHIP_LIMITED_TOOLTIP)
        }

        fleet.removeAbility(Abilities.INTERDICTION_PULSE)
        fleet.removeAbility(Abilities.SENSOR_BURST)
        fleet.removeAbility(Abilities.EMERGENCY_BURN)

        fleet.addAbility(Abilities.TRANSVERSE_JUMP)

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true

        fleet.setFaction(Factions.PLAYER, false)
        //fleet.isNoFactionInName = true

        fleet.name = "Fractal Reliquary"

        fleets += fleet

        return fleet
    }

    private fun canSpawnPatrols(): Boolean {
        val colony = getFractalColony() ?: return false
        val admin = colony.admin
        if (!admin.stats.hasSkill(niko_MPC_ids.BATTLEMIND_CAMPAIGN_SKILL_ID)) return false
        if (fleets.size >= getMaxPatrols()) return false
        return true
    }

    private fun getMaxPatrols(): Int {
        val fractalColony = getFractalColony() ?: return 0
        return (BASE_PATROLS + (fractalColony.size * PATROL_PER_COLONY_LEVEL)) - destroyedFleets
    }

    // this shit doesnt work in the skill lmao. we do it manually
    private fun checkRouting() {
        val market = getFractalColony()

        if (market != null) {
            for (otherMarket in market.faction.getMarketsCopy().filter { it != market && it.admin?.isAICore == true }) {
                if (otherMarket.size > market.size) continue
                otherMarket.accessibilityMod.modifyFlat("MPC_routingOptAccess", MPC_routingOptimization.AI_CORE_ADMIN_ACCESSIBILITY_BONUS, "${market.name} administrator")
            }
        } else {
            for (otherMarket in Global.getSector().economy.marketsCopy) {
                otherMarket.accessibilityMod.unmodify("MPC_routingOptAccess") // this is a nuclear option, a bit slow but really it shouldnt be that bad
            }
        }
    }

    private fun checkReaction() {
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_hegePrivateInspectorsSpawned")) {
            //delete()
            return
        }

        val colony = getFractalColony() ?: return
        val AICoreCond = AICoreAdmin.get(colony) ?: return
        if (AICoreCond.daysActive >= DAYS_TIL_HEGE_FLEET) {
            trySpawnHegeFleet(colony)
        }
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (reason == null) return

        if (fleet !in fleets) return
        fleets -= fleet

        if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {
            destroyedFleets++
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