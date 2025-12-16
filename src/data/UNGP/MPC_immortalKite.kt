package data.UNGP

import data.utilities.niko_MPC_mathUtils.trimHangingZero
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.sinkhole.MPC_sinkholeTerrain
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_mathUtils.roundNumTo
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import ungp.api.rules.UNGP_BaseRuleEffect
import ungp.api.rules.tags.UNGP_CampaignListenerTag
import ungp.api.rules.tags.UNGP_CampaignTag
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings
import kotlin.math.PI

class MPC_immortalKite: UNGP_BaseRuleEffect(), UNGP_CampaignTag, UNGP_CampaignListenerTag<FleetEventListener> {

    companion object {
        const val IMMORTAL_KITE_MEMID = "\$MPC_immortalKite"
        const val BASE_SINKHOLE_SIZE = 400f

        fun getRespawnTimer(): IntervalUtil? {
            var timer = Global.getSector().memoryWithoutUpdate["\$MPC_immortalKiteRespawn"] as? IntervalUtil
            if (getImmortalKite() == null && timer == null) {
                timer = IntervalUtil(0.2f, 0.3f) // sanity
                Global.getSector().memoryWithoutUpdate["\$MPC_immortalKiteRespawn"] = timer
            }
            return timer
        }
        fun getImmortalKite(): CampaignFleetAPI? = Global.getSector().memoryWithoutUpdate.getFleet(IMMORTAL_KITE_MEMID)

        fun createNewFleet(effectMult: Float): CampaignFleetAPI {
            val fleet = FleetFactoryV3.createEmptyFleet(Factions.NEUTRAL, FleetTypes.PATROL_SMALL, null)
            fleet.fleetData.addFleetMember(createImmortalKite())
            fleet.forceSync()

            fleet.stats.fleetwideMaxBurnMod.modifyMult("MPC_immortalSnail", 0.4f)
            fleet.stats.sensorProfileMod.modifyMult("MPC_immortalSnail", 4f)
            fleet.removeAbility(Abilities.SUSTAINED_BURN)
            fleet.removeAbility(Abilities.EMERGENCY_BURN)
            fleet.removeAbility(Abilities.INTERDICTION_PULSE)

            fleet.memoryWithoutUpdate[FleetAIFlags.WANTS_TRANSPONDER_ON] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
            fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE] = true
            fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_REP_IMPACT] = true
            fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS] = true
            fleet.memoryWithoutUpdate["\$MPC_immortalKite"] = true

            fleet.isNoFactionInName = true
            fleet.name = "Your Immortal Adversary"

            fleet.addAssignment(
                FleetAssignment.INTERCEPT,
                Global.getSector().playerFleet,
                Float.MAX_VALUE,
                "hunting you down"
            )

            return fleet
        }
        fun createImmortalKite(): FleetMemberAPI {
            val member = Global.getSettings().createFleetMember(FleetMemberType.SHIP, "kite_immortalsnail")
            member.captain = getSnailPerson()

            val variant = member.variant
            val newVariant = variant.clone()
            newVariant.source = VariantSource.REFIT
            newVariant.originalVariant = null
            newVariant.hullVariantId = Misc.genUID()
            newVariant.addTag(Tags.VARIANT_UNBOARDABLE)
            member.setVariant(newVariant, false, false)

            return member
        }
        fun getSnailPerson(): PersonAPI {
            val person = MPC_People.getImportantPeople()[MPC_People.IMMORTAL_SNAIL]!!
            return person
        }
    }

    class MPC_immortalKiteListener(val rule: MPC_immortalKite): FleetEventListener {
        override fun reportFleetDespawnedToListener(
            fleet: CampaignFleetAPI?,
            reason: CampaignEventListener.FleetDespawnReason?,
            param: Any?
        ) {
            if (fleet != getImmortalKite()) return

            Global.getSector().memoryWithoutUpdate.unset(IMMORTAL_KITE_MEMID)
            rule.beginRespawnCooldown()
        }

        override fun reportBattleOccurred(
            fleet: CampaignFleetAPI?,
            primaryWinner: CampaignFleetAPI?,
            battle: BattleAPI?
        ) {
            return
        }
    }

    var effectMult = 1f

    override fun updateDifficultyCache(difficulty: UNGP_SpecialistSettings.Difficulty?) {
        if (difficulty == null) return

        effectMult = difficulty.getLinearValue(1f, 1f)
        super.updateDifficultyCache(difficulty)
    }

    override fun advanceInCampaign(amount: Float, params: UNGP_CampaignPlugin.TempCampaignParams?) {
        if (Global.getSector().isPaused) return

        val days = Misc.getDays(amount)
        val respawn = getRespawnTimer()
        if (respawn != null) {
            respawn.advance(days)
            if (respawn.intervalElapsed()) {
                respawnFleet()
            }
        }

        val fleet = getImmortalKite() ?: return
        val playerFleet = Global.getSector().playerFleet
        val lyDist = Misc.getDistanceLY(fleet, playerFleet)
        if (lyDist >= 5f) {
            repositionFleet(fleet)
        }

        val visibility = fleet.visibilityLevelOfPlayerFleet
        if (visibility.ordinal < SectorEntityToken.VisibilityLevel.SENSOR_CONTACT.ordinal) {
            if (fleet.assignmentsCopy.firstOrNull()?.assignment != FleetAssignment.DELIVER_CREW) {
                fleet.clearAssignments()
                fleet.addAssignment(
                    FleetAssignment.DELIVER_CREW,
                    Global.getSector().playerFleet,
                    Float.MAX_VALUE,
                    "hunting you down"
                )
            }
        } else if (fleet.assignmentsCopy.firstOrNull()?.assignment != FleetAssignment.INTERCEPT) {
            fleet.clearAssignments()
            fleet.addAssignment(
                FleetAssignment.INTERCEPT,
                Global.getSector().playerFleet,
                Float.MAX_VALUE,
                "hunting you down"
            )
        }
    }

    private fun respawnFleet() {
        Global.getSector().memoryWithoutUpdate["\$MPC_immortalKiteRespawn"] = null
        spawnFleet()
    }

    private fun spawnFleet() {
        val newFleet = createNewFleet(effectMult)
        repositionFleet(newFleet)
        val size = BASE_SINKHOLE_SIZE * effectMult
        val params = MPC_sinkholeTerrain.SinkholeParams(
            size,
            size * 0.1f,
            newFleet
        )
        MPC_sinkholeTerrain.addFieldToEntity(newFleet, "snailhole", params)
        Global.getSector().memoryWithoutUpdate[IMMORTAL_KITE_MEMID] = newFleet
    }

    fun repositionFleet(fleet: CampaignFleetAPI) {
        val playerFleet = Global.getSector().playerFleet
        val containing = playerFleet.containingLocation
        val goodDistToSpawn = 5000f.coerceAtLeast(playerFleet.sensorStrength * 2f) // arbitrary
        val point = Misc.getPointAtRadius(playerFleet.location, goodDistToSpawn)

        fleet.containingLocation?.removeEntity(fleet)
        containing.addEntity(fleet)
        fleet.setLocation(point.x, point.y)
        fleet.facing = VectorUtils.getAngle(fleet.location, playerFleet.location)
    }

    override fun cleanUp() {
        despawnFleet()
    }

    private fun despawnFleet() {
        getImmortalKite()?.despawn()
        Global.getSector().memoryWithoutUpdate.unset(IMMORTAL_KITE_MEMID)
    }

    fun beginRespawnCooldown() {
        if (getImmortalKite() != null) return
        Global.getSector().memoryWithoutUpdate["\$MPC_immortalKiteRespawn"] = IntervalUtil(getMinRespawnTime(), getMaxRespawnTime())
    }

    override fun getListener(): FleetEventListener? {
        return MPC_immortalKiteListener(this)
    }

    override fun getClassOfListener(): Class<FleetEventListener>? {
        return MPC_immortalKiteListener::class.java as Class<FleetEventListener>?
    }

    fun getMinRespawnTime(): Float {
        return 25f * (1 / effectMult)
    }

    fun getMaxRespawnTime(): Float {
        return 30f * (1 / effectMult)
    }

    override fun getDescriptionParams(index: Int, difficulty: UNGP_SpecialistSettings.Difficulty?): String {
        updateDifficultyCache(difficulty)
        when (index) {
            0 -> return "interdiction field"
            1 -> return "${(BASE_SINKHOLE_SIZE * effectMult).roundNumTo(1).trimHangingZero()}su"
            2 -> return "${getMinRespawnTime().roundNumTo(1).trimHangingZero()}-${getMaxRespawnTime().roundNumTo(1).trimHangingZero()} days"
            else -> return ""
        }
    }

}