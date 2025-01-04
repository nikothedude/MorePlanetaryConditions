package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin
import com.fs.starfarer.api.loading.VariantSource
import data.scripts.MPC_delayedExecution
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_mathUtils
import org.lazywizard.lazylib.MathUtils

class MPC_IAIICDKFuelHubFleetSpawner(
    val system: StarSystemAPI
): niko_MPC_baseNikoScript() {

    companion object {
        const val DIST_TO_SPAWN = 1000f
        const val FLEET_POINT_MULT = 1.1f
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
        val target = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as SectorEntityToken
        val playerFleet = Global.getSector().playerFleet ?: return
        val toSpawnWith = (playerFleet.fleetPoints * FLEET_POINT_MULT).coerceAtLeast(MIN_FLEET_POINTS)

        val params = FleetParamsV3(
            null,
            Factions.DIKTAT,
            null,
            FleetTypes.PATROL_LARGE,
            toSpawnWith,
            40f,
            40f,
            5f,
            0f,
            6f,
            0f
        )
        params.averageSMods = 2
        //params.maxShipSize = 3
        //params.officerNumberMult = 1.5f

        val fleet = FleetFactoryV3.createFleet(params)
        fleet.inflateIfNeeded()
        fleet.setFaction(Factions.DIKTAT, true)

        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_LOW_REP_IMPACT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.memoryWithoutUpdate["\$MPC_IAIICDKFuelHubExpeditionFleet"] = true
        fleet.name = "Expedition"
        MPC_IAIICKDFuelHubFleetScript(fleet, target).start()

        system.addEntity(fleet)
        fleet.setLocation(target.location.x, target.location.y)
        fleet.addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, target, Float.MAX_VALUE, "salvaging ruins on ${target.name}",null)
    }
}