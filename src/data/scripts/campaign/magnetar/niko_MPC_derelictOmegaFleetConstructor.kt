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
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils

object niko_MPC_derelictOmegaFleetConstructor {

    const val CHANCE_FOR_OMEGA_IN_FLEET = 0.2f
    const val PERCENT_OF_FP_TO_OMEGA = 0.3f
    const val OMEGA_FP_MULT = 2.3f
    const val MIN_OMEGA_FP = (6f * OMEGA_FP_MULT) // sinstral shard

    fun setupFleet(fleet: CampaignFleetAPI): CampaignFleetAPI {
        fleet.memoryWithoutUpdate[niko_MPC_ids.IMMUNE_TO_MAGNETAR_PULSE] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true // kinda unfun
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = false

        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF] = true

        fleet.removeAbility(Abilities.SUSTAINED_BURN)
        fleet.removeAbility(Abilities.INTERDICTION_PULSE)
        fleet.removeAbility(Abilities.SENSOR_BURST)
        fleet.removeAbility(Abilities.EMERGENCY_BURN)

        return fleet
    }

    fun createFleet(fleetPoints: Float, source: SectorEntityToken?): CampaignFleetAPI {
        var omegaFleet: CampaignFleetAPI? = null
        var omegaParams: FleetParamsV3? = null
        var derelictPoints = fleetPoints
        val tryOmega = (MathUtils.getRandom().nextFloat() <= CHANCE_FOR_OMEGA_IN_FLEET)
        if (tryOmega) {
            var omegaBudget = (derelictPoints * PERCENT_OF_FP_TO_OMEGA) * OMEGA_FP_MULT
            if (omegaBudget > MIN_OMEGA_FP)  {
                derelictPoints -= omegaBudget
                omegaBudget /= OMEGA_FP_MULT

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
            captain.setRankId(Ranks.SPACE_COMMANDER)
            captain.setPostId(Ranks.POST_FLEET_COMMANDER)
            derelictFleet.setCommander(captain)
            derelictFleet.getFleetData().setFlagship(bestShip)
            OmegaOfficerGeneratorPlugin.addCommanderSkills(captain, derelictFleet, omegaParams, 2, MathUtils.getRandom())
        }
        derelictFleet.fleetData.sort()
        derelictFleet.setFaction(niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, true)
        return derelictFleet
    }

    fun createOmegaFleet(params: FleetParamsV3): CampaignFleetAPI {
        val fleet = FleetFactoryV3.createFleet(params)
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

        return params
    }

    fun createDerelictFleet(params: FleetParamsV3): CampaignFleetAPI {
        val fleet = FleetFactoryV3.createFleet(params)
        val omegaFaction = Global.getSector().getFaction(Factions.OMEGA)

        val plugin = Misc.getAICoreOfficerPlugin(Commodities.OMEGA_CORE)

        //fleet.inflater = MPC_derelictOmegaDerelictInflater()
        fleet.inflateIfNeeded()
        fleet.inflater = null
        for (member in fleet.fleetData.membersListCopy) {
            //val omegaCore = plugin.createPerson(Commodities.OMEGA_CORE, omegaFaction.id, MathUtils.getRandom())
            //member.captain = omegaCore

            val clonedVariant = member.variant.clone()
            clonedVariant.source = VariantSource.REFIT
            clonedVariant.addPermaMod("niko_MPC_subsumedIntelligence")
            clonedVariant.addTag(Tags.UNRECOVERABLE) // they can drop with omega weapons
            member.setVariant(clonedVariant, false, true)
        }
        fleet.fleetData.sort()
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
        params.modeOverride
        params.averageSMods = 1
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA
        params.quality = 10f
        params.allWeapons = true

        return createDerelictFleet(params)
    }
}