package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.procgen.themes.OmegaOfficerGeneratorPlugin
import com.fs.starfarer.api.loading.VariantSource
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils

object niko_MPC_derelictOmegaFleetConstructor {

    const val CHANCE_FOR_OMEGA_IN_FLEET = 0.05f
    const val PERCENT_OF_FP_TO_OMEGA = 0.49f
    /** The approximate FP, from base, an omega ship "really" has. */
    const val OMEGA_FP_MULT = 2f
    const val MIN_OMEGA_FP = (6f) // sinstral shard

    fun setupFleet(fleet: CampaignFleetAPI): CampaignFleetAPI {
        fleet.memoryWithoutUpdate[niko_MPC_ids.IMMUNE_TO_MAGNETAR_PULSE] = true
        fleet.memoryWithoutUpdate["\$MPC_magnetarFleet"] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE_WHILE_TOFF] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true // kinda unfun
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = false

        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY] = true

        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS] = true

        fleet.removeAbility(Abilities.SUSTAINED_BURN)
        //fleet.removeAbility(Abilities.INTERDICTION_PULSE)
        fleet.removeAbility(Abilities.SENSOR_BURST)
        fleet.removeAbility(Abilities.EMERGENCY_BURN)

        fleet.stats.sensorRangeMod.modifyMult("MPC_magnetarFleetSensorMalus", 0.45f, "you shouldnt see this")
        fleet.stats.sensorProfileMod.modifyMult("MPC_magnetarFleetProfileMalus", 4f, "you shouldnt see this")

        return fleet
    }

    fun createFleet(fleetPoints: Float, source: SectorEntityToken?, omegaChance: Float = CHANCE_FOR_OMEGA_IN_FLEET, addListener: Boolean = true): CampaignFleetAPI {
        var omegaFleet: CampaignFleetAPI? = null
        var omegaParams: FleetParamsV3? = null
        var derelictPoints = fleetPoints
        val tryOmega = (MathUtils.getRandom().nextFloat() <= omegaChance)
        if (tryOmega) {

            // step 1: get a % of the base budget in omegaBudget
            // step 4: multiply omegaBudget against a inverted OMEGA_FP_MULT because otherwise we get a buncha tesseracts and stuff
            // (reduces the amount of omega FP to reflect the actual strength of omega - e.g. a sinstral shard is actually around 12 FP or smthn)
            var omegaBudget = (derelictPoints * PERCENT_OF_FP_TO_OMEGA)
            omegaBudget /= OMEGA_FP_MULT
            if (omegaBudget > MIN_OMEGA_FP)  {
                derelictPoints *= (PERCENT_OF_FP_TO_OMEGA)

                omegaParams = createOmegaParams(omegaBudget, source)
                omegaFleet = createOmegaFleet(omegaParams)
            }
        }
        val derelictFleet = createDerelictFleet(derelictPoints, source)
        if (omegaFleet != null) {
            for (member in omegaFleet.membersWithFightersCopy) {
                omegaFleet.fleetData.removeFleetMember(member)
                derelictFleet.fleetData.addFleetMember(member)
            }
            omegaFleet.despawn()
        }
        derelictFleet.fleetData.sort()
        val bestShip = derelictFleet.fleetData.membersInPriorityOrder.firstOrNull()
        if (bestShip != null) {
            val captain = bestShip.captain
            derelictFleet.commander = captain
            captain.rankId = Ranks.SPACE_COMMANDER
            captain.postId = Ranks.POST_FLEET_COMMANDER
            derelictFleet.commander = captain
            derelictFleet.fleetData.setFlagship(bestShip)
            OmegaOfficerGeneratorPlugin.addCommanderSkills(captain, derelictFleet, omegaParams, 2, MathUtils.getRandom())
        }
        derelictFleet.fleetData.sort()
        derelictFleet.forceSync()
        derelictFleet.fleetData.setSyncNeeded()
        derelictFleet.fleetData.syncIfNeeded()
        derelictFleet.setFaction(niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, true)

        derelictFleet.fleetData.membersListCopy.forEach { it.variant.originalVariant = null }

        if (addListener) { // NOT NEEDED, I FIGURED IT OUT
            //MPC_variantFixerListener(derelictFleet).begin() // the nuclear option
        }

        return derelictFleet
    }

    fun createOmegaFleet(params: FleetParamsV3): CampaignFleetAPI {
        val fleet = FleetFactoryV3.createFleet(params)

        for (member in fleet.membersWithFightersCopy) {
            // to "perm" the variant so it gets saved and not recreated
            member.setVariant(member.variant.clone(), false, false)
            member.variant.source = VariantSource.REFIT
            member.variant.addTag(Tags.SHIP_LIMITED_TOOLTIP)
        }

        return fleet
    }

    fun createOmegaParams(fleetPoints: Float, source: SectorEntityToken?): FleetParamsV3 {
        var type = FleetTypes.PATROL_SMALL
        if (fleetPoints > 60) type = FleetTypes.PATROL_MEDIUM
        if (fleetPoints > 90) type = FleetTypes.PATROL_LARGE
        val params = FleetParamsV3(
            source?.market,
            source?.locationInHyperspace,
            niko_MPC_ids.OMEGA_DERELICT_FACTION_ID,
            null,
            type,
            fleetPoints,  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod
        )
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA

        return params
    }

    fun createDerelictFleet(params: FleetParamsV3): CampaignFleetAPI {
        val faction = Global.getSector().getFaction(niko_MPC_ids.derelictOmegaConstructorFactionId)
        val knownShips = faction.knownShips // it doesnt work on modplugin so i guess this works lol
        if (knownShips.contains("guardian")) {
            faction.removeKnownShip("guardian")
        }
        params.maxShipSize = 3 // prevents gaurdian from spawning in this fleet
        val fleet = FleetFactoryV3.createFleet(params)

        //fleet.inflater = MPC_derelictOmegaDerelictInflater()
        for (member in fleet.fleetData.membersListCopy) {
            //val omegaCore = plugin.createPerson(Commodities.OMEGA_CORE, omegaFaction.id, MathUtils.getRandom())
            //member.captain = omegaCore

            val clonedVariant = member.variant.clone()
            clonedVariant.source = VariantSource.REFIT
            clonedVariant.addMod("niko_MPC_subsumedIntelligence")
            clonedVariant.addTag(Tags.VARIANT_UNBOARDABLE) // they can drop with omega weapons
            clonedVariant.removeTag(Tags.AUTOMATED_RECOVERABLE)
            clonedVariant.originalVariant = null
            member.setVariant(clonedVariant, false, true)
        }

        fleet.inflateIfNeeded()
        fleet.inflater = null

        fleet.fleetData.sort()
        fleet.forceSync()
        fleet.fleetData.setSyncNeeded()
        fleet.fleetData.syncIfNeeded()
        fleet.isNoFactionInName = false

        return fleet
    }

    fun createDerelictFleet(fleetPoints: Float, source: SectorEntityToken?): CampaignFleetAPI {
        var type = FleetTypes.PATROL_SMALL
        if (fleetPoints > 60) type = FleetTypes.PATROL_MEDIUM
        if (fleetPoints > 90) type = FleetTypes.PATROL_LARGE

        val params = FleetParamsV3(
            source?.market,
            source?.locationInHyperspace,
            niko_MPC_ids.derelictOmegaConstructorFactionId,
            null,
            type,
            fleetPoints,  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod
        )
        params.averageSMods = 1
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA
        params.quality = 10f
        params.allWeapons = true

        return createDerelictFleet(params)
    }
}