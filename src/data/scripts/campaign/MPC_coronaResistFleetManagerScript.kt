package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import java.util.*

open class MPC_coronaResistFleetManagerScript(
    source: SectorEntityToken, thresholdLY: Float, minFleets: Int, maxFleets: Int, respawnDelay: Float, val minPoints: Int, val maxPoints: Int
): SourceBasedFleetManager(source, thresholdLY, minFleets, maxFleets, respawnDelay) {

    private var totalLost = 0

    override fun advance(amount: Float) {
        super.advance(amount)
    }

    override fun spawnFleet(): CampaignFleetAPI? {
        if (source == null) return null

        val random = Random()

        var combatPoints: Int = minPoints + random.nextInt(maxPoints - minPoints + 1)

        var bonus: Int = totalLost * 4
        if (bonus > maxPoints) bonus = maxPoints

        combatPoints += bonus

        var type = FleetTypes.PATROL_SMALL
        if (combatPoints > 8) type = FleetTypes.PATROL_MEDIUM
        if (combatPoints > 16) type = FleetTypes.PATROL_LARGE

        combatPoints *= 8f.toInt()

        val params = FleetParamsV3(
            source.market,
            source.locationInHyperspace,
            overgrownNanoforgeFleetFactionId,
            1f,
            type,
            combatPoints.toFloat(),  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod
        )
        //params.officerNumberBonus = 10;
        //params.officerNumberBonus = 10;
        params.random = random

        val fleet = FleetFactoryV3.createFleet(params) ?: return null

        val location = source.containingLocation
        location.addEntity(fleet)

        initDerelictFleetProperties(fleet)

        fleet.setLocation(source.location.x, source.location.y)
        fleet.facing = random.nextFloat() * 360f

        fleet.memoryWithoutUpdate["\$sourceId"] = source.id

        return fleet
    }

    private fun initDerelictFleetProperties(fleet: CampaignFleetAPI) {
        fleet.setFaction(overgrownNanoforgeFleetFactionId)

        fleet.removeAbility(Abilities.EMERGENCY_BURN)
        fleet.removeAbility(Abilities.SENSOR_BURST)
        fleet.removeAbility(Abilities.GO_DARK)

        // to make sure they attack the player on sight when player's transponder is off
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = false

        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS] = true
        fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_DEFENDER] = true

        fleet.addScript(overgrownNanoforgeFleetAssignmentAI(fleet, source.starSystem, source))
        fleet.addEventListener(this)

        RemnantSeededFleetManager.addRemnantInteractionConfig(fleet)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: FleetDespawnReason, param: Any?) {
        super.reportFleetDespawnedToListener(fleet, reason, param)
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) {
            val sid = fleet.memoryWithoutUpdate.getString("\$sourceId") ?: return
            if (source == null) return
            if (sid != source.id) return
                //if (sid != null && sid.equals(source.getId())) {
                totalLost++
        }
    }

    protected open fun readResolve(): Any? {
        return this
    }
}

