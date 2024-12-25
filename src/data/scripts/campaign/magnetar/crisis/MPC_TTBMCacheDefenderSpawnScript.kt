package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.rpg.Person
import data.scripts.MPC_delayedExecution
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_mathUtils.prob
import org.lazywizard.lazylib.MathUtils

class MPC_TTBMCacheDefenderSpawnScript(
    val system: StarSystemAPI
): niko_MPC_baseNikoScript() {

    companion object {
        const val DIST_TO_SPAWN = 1000f
        const val FLEET_POINT_MULT = 1.7f
        const val MIN_FLEET_POINTS = 200f
    }

    override fun startImpl() {
        system.addScript(this)
    }

    override fun stopImpl() {
        system.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false
    override fun advance(amount: Float) {
        val playerFleet = Global.getSector().playerFleet ?: return
        if (playerFleet.containingLocation == system) {
            spawnDefenders()
            stop()
            return
        }
        val dist = MathUtils.getDistance(playerFleet.locationInHyperspace, system.hyperspaceAnchor.location)
        if (dist <= DIST_TO_SPAWN) {
            spawnDefenders()
            stop()
            return
        }
    }

    private fun spawnDefenders() {
        val cache = system.getCustomEntitiesWithTag("MPC_BMDataCache").firstOrNull() ?: return
        val playerFleet = Global.getSector().playerFleet ?: return
        val toSpawnWith = (playerFleet.fleetPoints * FLEET_POINT_MULT).coerceAtLeast(MIN_FLEET_POINTS)

        val params = FleetParamsV3(
            null,
            Factions.DERELICT,
            1f,
            FleetTypes.PATROL_LARGE,
            toSpawnWith,
            0f,
            0f,
            0f,
            0f,
            0f,
            1f
        )
        params.averageSMods = 1
        params.maxShipSize = 3
        //params.officerNumberMult = 1.5f

        val fleet = FleetFactoryV3.createFleet(params)
        //fleet.containingLocation = system
        MPC_delayedExecution(
            {
                fleet.inflateIfNeeded()
                fleet.inflater = null

                fleet.fleetData.membersListCopy.forEach {
                    val copyVariant = it.variant.clone()
                    if (copyVariant.hullSpec.shieldType == ShieldAPI.ShieldType.NONE) {
                        copyVariant.addPermaMod(HullMods.MAKESHIFT_GENERATOR, true)
                    }
                    if (copyVariant.hullSpec.shieldType != ShieldAPI.ShieldType.PHASE) {
                        copyVariant.addPermaMod(HullMods.HARDENED_SHIELDS, true)
                        copyVariant.addPermaMod(HullMods.STABILIZEDSHIELDEMITTER, true)
                        if (prob(50)) {
                            copyVariant.addPermaMod(HullMods.OMNI_SHIELD_CONVERSION, true)
                        } else {
                            copyVariant.addPermaMod(HullMods.EXTENDED_SHIELDS, true)
                        }
                    } else if (!copyVariant.hasHullMod(HullMods.PHASE_ANCHOR)){
                        copyVariant.addPermaMod(HullMods.ADAPTIVE_COILS, true)
                    }
                    copyVariant.source = VariantSource.REFIT
                    it.setVariant(copyVariant, false, true)
                    if (prob(70)) {
                        it.captain = AICoreOfficerPluginImpl().createPerson(Commodities.ALPHA_CORE, Factions.TRITACHYON, MathUtils.getRandom())
                    } else {
                        it.captain = null
                    }
                    RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(it)
                    it.repairTracker.cr = it.repairTracker.maxCR
                }
                fleet.flagship.captain = AICoreOfficerPluginImpl().createPerson(Commodities.ALPHA_CORE, Factions.TRITACHYON, MathUtils.getRandom())
                RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.flagship)
                fleet.flagship.repairTracker.cr = fleet.flagship.repairTracker.maxCR
                fleet.setFaction(Factions.TRITACHYON, true)
                fleet.commander = fleet.flagship.captain
            },
            0f,
            false,
            useDays = false
        ).start()

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_REP_IMPACT] = true
        fleet.memoryWithoutUpdate["\$MPC_TTBMDerelictGuard"] = true
        fleet.isNoFactionInName = true
        fleet.name = "Hacked Derelict Guard-fleet"

        system.addEntity(fleet)
        fleet.setLocation(cache.location.x, cache.location.y)
        fleet.addAssignmentAtStart(FleetAssignment.DEFEND_LOCATION, cache, Float.MAX_VALUE, null)
    }
}