package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes.*
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags.HULLMOD_DMOD
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

object niko_MPC_fleetUtils {
    /**
     * Fills fleet with the given variants up until budget isn't high enough to generate any more variants.
     * Uses a weighted picking system to determine what ships to add.
     * @param budget The amount of FP to be added to the fleet. Hard cap.
     * @param fleet The fleet to fill.
     * @param variants The variants, in variantId -> weight format, to be picked.
     * @return a list of the newly created fleetmembers.
     */
    @JvmStatic
    fun attemptToFillFleetWithVariants(budget: Int, fleet: CampaignFleetAPI, variants: HashMap<String?, Float?>, altBudgetMode: Boolean): List<FleetMemberAPI> {
        var mutableBudget = budget
        val newFleetMembers: MutableList<FleetMemberAPI> = ArrayList()
        if (mutableBudget <= 0) {
            return newFleetMembers
        }
        val picker = WeightedRandomPicker<String?>()
        for ((key, value) in variants) { //add the contents of the variants to the picker
            picker.add(key, value!!)
        }

        // explanation of the conditions here: since when a ship is successfully added we subtract its FP from budget,
        // we need to always check to see if budget is empty so we can stop
        // and since we also remove any variants that don't have enough fp to be made, we need to check to make sure
        // the picker still hsa things to pick
        while (mutableBudget > 0 && !picker.isEmpty) {
            val pickedVariantId = picker.pick()
            val variant = Global.getSettings().getVariant(pickedVariantId)
            var variantFp = variant.hullSpec.fleetPoints
            if (altBudgetMode) variantFp = 1 //turns this into a method that adds x amount of ships
            if (variantFp <= mutableBudget) { // is only true if we can afford making this ship
                val ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, pickedVariantId)
                if (ship != null) {
                    newFleetMembers.add(ship)
                    ship.repairTracker.cr = 0.7f //the ships spawn with 50 cr, fo rsome reaosn, so i have to do this
                } else {
                    val error = "attemptToFillFleetWithVariants created null ship, fleet: $fleet"
                    displayError(error) //THIS SHOULD NEVER HAPPEN. EVER.
                }
                mutableBudget -= variantFp
            } else {
                picker.remove(pickedVariantId)
                continue  //continue for clarity
            }
        }
        for (ship in newFleetMembers) {
            fleet.fleetData.addFleetMember(ship)
        }
        return newFleetMembers
    }

    /** Despawn() on satellite fleets will trigger the despawn listener. No need for any fancy shit.*/
    fun CampaignFleetAPI.satelliteFleetDespawn(vanish: Boolean = false) {
        if (isSatelliteFleet()) {
            if (vanish) {
                setLocation(9999999f, 9999999f)
            }
        }
        despawn() //will ALWAYS call the despawn listener
    }

    fun SectorEntityToken.getSatelliteEntityHandler(): niko_MPC_satelliteHandlerCore? {
        if (niko_MPC_debugUtils.memKeyHasIncorrectType<niko_MPC_satelliteHandlerCore>(this, niko_MPC_ids.satelliteEntityHandler)) return null
        return memoryWithoutUpdate[niko_MPC_ids.satelliteEntityHandler] as niko_MPC_satelliteHandlerCore
    }

    fun HasMemory.setSatelliteEntityHandler(handler: niko_MPC_satelliteHandlerCore) {
        memoryWithoutUpdate[niko_MPC_ids.satelliteEntityHandler] = handler
    }

    /** TODO finish */
    fun CampaignFleetAPI.trimDownToFP(maxFp: Float) {
        if (fleetData.membersListCopy.isEmpty()) return
        var failsafeIndex = 0
        val failsafeThreshold = 35
        var addedDefecit = 0
        val effectiveFleetPoints = fleetData.fleetPointsUsed - addedDefecit
        while ((effectiveFleetPoints - addedDefecit) > maxFp) {
            if (++failsafeIndex >= failsafeThreshold) {
                displayError("$this trimdown interrupted due to failsafe Index ($failsafeIndex) exceeding or reaching $failsafeThreshold")
                logDataOf(this)
                return
            }
            val fleetMember: FleetMemberAPI = fleetData.membersListCopy.randomOrNull() ?: return
            val maxDmods = 6
            var amountOfDmods = 0
            for (hullmodId: String in fleetMember.variant.hullMods) {
                val hullmod: HullModSpecAPI = Global.getSettings().getHullModSpec(hullmodId)
                if (hullmod.hasTag(HULLMOD_DMOD)) amountOfDmods++
            }
            if (amountOfDmods >= maxDmods) {
                fleetData.removeFleetMember(fleetMember)
                break
            }
            val numToAdd = 1
            val variant = fleetMember.variant
            for (moduleId: String in variant.stationModules.keys) {
                val module = variant.getModuleVariant(moduleId)
                DModManager.addDMods(module, true, numToAdd, MathUtils.getRandom())
            }
            DModManager.addDMods(variant, true, numToAdd, MathUtils.getRandom())
            addedDefecit += (numToAdd * 5)

            if ((effectiveStrength - addedDefecit) <= maxFp) return
        }
    }

    fun CampaignFleetAPI.setDummyFleet(dummyMode: Boolean = true) {
        val ourMemory: MemoryAPI = memoryWithoutUpdate
        ourMemory.set(niko_MPC_ids.isDummyFleetId, dummyMode)
        isDoNotAdvanceAI = dummyMode
    }

    fun CampaignFleetAPI.isDummyFleet(): Boolean {
        return memoryWithoutUpdate.`is`(niko_MPC_ids.isDummyFleetId, true)
    }

    fun CampaignFleetAPI.isSatelliteFleet(): Boolean {
        return hasTag(niko_MPC_ids.isSatelliteFleetId)
    }

    fun CampaignFleetAPI.setSatelliteFleet(mode: Boolean) {
        memoryWithoutUpdate[niko_MPC_ids.isSatelliteFleetId] = mode
    }

    fun CampaignFleetAPI.setTemporaryFleetDespawner(script: niko_MPC_temporarySatelliteFleetDespawner?) {
        memoryWithoutUpdate[niko_MPC_ids.temporaryFleetDespawnerId] = script
    }

    fun CampaignFleetAPI.getTemporaryFleetDespawner(): niko_MPC_temporarySatelliteFleetDespawner? {
        return memoryWithoutUpdate[niko_MPC_ids.temporaryFleetDespawnerId] as? niko_MPC_temporarySatelliteFleetDespawner
    }

    val defaultFriendliesForArrayBonus = hashMapOf(
        Pair(TRADE, RepLevel.INHOSPITABLE),
        Pair(TRADE_SMUGGLER, RepLevel.INHOSPITABLE),
        Pair(TRADE_SMALL, RepLevel.INHOSPITABLE),
        Pair(TRADE_LINER, RepLevel.INHOSPITABLE),
        Pair(FOOD_RELIEF_FLEET, RepLevel.INHOSPITABLE),
        Pair(SHRINE_PILGRIMS, RepLevel.INHOSPITABLE),
        Pair(ACADEMY_FLEET, RepLevel.INHOSPITABLE),
    )
    fun CampaignFleetAPI.getRepLevelForArrayBonus(
        repMap: MutableMap<String, RepLevel> = defaultFriendliesForArrayBonus,
        defaultRep: RepLevel = RepLevel.FRIENDLY,
    ): RepLevel {

        val fleetType: String = memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] as? String ?: return defaultRep
        return repMap[fleetType] ?: return defaultRep
    }

    /// Returns a float from 0 to 1, representing a percentage of phase ships.
    fun CampaignFleetAPI.getPhaseShipPercent(): Float {
        var numPhaseShips = 0f
        for (member in fleetData.membersListCopy) { //excludes fighers, hopefully
            if (member.isMothballed) continue
            if (member.isPhaseShip) numPhaseShips++
        }
        val numMembers = numMembersFast.toFloat()
        if (numPhaseShips == 0f || numMembers == 0f) return 0f

        return (numPhaseShips / numMembers)
    }

    fun CampaignFleetAPI.getApproximateECMValue(): Float{
        var ecmValue = 0f

        for (member in fleetData.membersListCopy) {
            if (member.isMothballed) continue
            ecmValue += member.stats.dynamic.getValue(Stats.ELECTRONIC_WARFARE_FLAT, 0f)
        }

        return ecmValue
    }

    fun CampaignFleetAPI.counterTerrainMovement(days: Float, movementDivisor: Float) {
        if (containingLocation == null) return

        val velocity = velocity
        for (terrain in containingLocation.terrainCopy) {
            if (terrain.plugin == null) continue
            val offset = approximateCounterVelocityOfTerrain(terrain.plugin, days, movementDivisor) ?: continue

            setVelocity(velocity.x + offset.x, velocity.y + offset.y)
        }
    }

    /// Returns the velocity needed to counteract the pushforce of a given terrain entity.
    fun CampaignFleetAPI.approximateCounterVelocityOfTerrain(plugin: CampaignTerrainPlugin, days: Float, movementDivisor: Float): Vector2f? {
        if (!plugin.containsEntity(this)) return null // coronas still push you around bub
        if (plugin is StarCoronaTerrainPlugin) return plugin.approximateOffsetForFleet(this, days, movementDivisor)
        if (plugin is PulsarBeamTerrainPlugin) return plugin.approximateOffsetForFleet(this, days, movementDivisor)

        return null
    }
    fun PulsarBeamTerrainPlugin.approximateOffsetForFleet(fleet: CampaignFleetAPI, days: Float, movementDivisor: Float): Vector2f {
        val intensity = getIntensityAtPoint(fleet.location)

        // "wind" effect - adjust velocity
        val maxFleetBurn = fleet.fleetData.burnLevel
        val currFleetBurn = fleet.currBurnLevel

        val maxWindBurn = params.windBurnLevel

        val currWindBurn: Float = intensity * maxWindBurn
        val maxFleetBurnIntoWind = maxFleetBurn - Math.abs(currWindBurn)

        val angle = Misc.getAngleInDegreesStrict(entity.location, fleet.location)
        val windDir = Misc.getUnitVectorAtDegreeAngle(angle)
        if (currWindBurn < 0) {
            windDir.negate()
        }

        val velDir = Misc.normalise(Vector2f(fleet.velocity))
        velDir.scale(currFleetBurn)

        val fleetBurnAgainstWind = -1.0 * Vector2f.dot(windDir, velDir)

        var accelMult = 0.5
        if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
            accelMult += 0.75 + 0.25 * (fleetBurnAgainstWind - maxFleetBurnIntoWind)
        }

        val seconds: Float = days * Global.getSector().clock.secondsPerDay

        windDir.scale((seconds * fleet.acceleration * accelMult).toFloat())
        windDir.x = -windDir.x / movementDivisor // somewhat arbitrary divisors but they do the job
        windDir.y = -windDir.y / movementDivisor
        return windDir
    }
    fun StarCoronaTerrainPlugin.approximateOffsetForFleet(fleet: CampaignFleetAPI, days: Float, movementDivisor: Float): Vector2f? {
        val intensity = getIntensityAtPoint(fleet.location)
        val inFlare = flareManager.isInActiveFlareArc(fleet)

        // "wind" effect - adjust velocity
        val maxFleetBurn = fleet.fleetData.burnLevel
        val currFleetBurn = fleet.currBurnLevel

        var maxWindBurn = params.windBurnLevel
        if (inFlare) {
            maxWindBurn *= 2f
        }

        val currWindBurn: Float = intensity * maxWindBurn
        val maxFleetBurnIntoWind = maxFleetBurn - abs(currWindBurn)

        val angle = Misc.getAngleInDegreesStrict(entity.location, fleet.location)
        val windDir = Misc.getUnitVectorAtDegreeAngle(angle)
        if (currWindBurn < 0) {
            windDir.negate()
        }

        val velDir = Misc.normalise(Vector2f(fleet.velocity))
        velDir.scale(currFleetBurn)

        val fleetBurnAgainstWind = -1f * Vector2f.dot(windDir, velDir)

        var accelMult = 0.5f
        if (fleetBurnAgainstWind > maxFleetBurnIntoWind) {
            accelMult += 0.75f + 0.25f * (fleetBurnAgainstWind - maxFleetBurnIntoWind)
        }
        val fleetAccelMult = fleet.stats.accelerationMult.modifiedValue
        if (fleetAccelMult > 0) { // && fleetAccelMult < 1) {
            accelMult /= fleetAccelMult
        }

        val seconds = days * Global.getSector().clock.secondsPerDay

        windDir.scale(seconds * fleet.acceleration * accelMult)

        windDir.x = -windDir.x / (movementDivisor)
        windDir.y = -windDir.y / (movementDivisor)
        return windDir
    }

    fun MarketAPI.getDerelictEscortTimeouts(): MutableMap<CampaignFleetAPI, Float> {
        var timeouts = memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_TIMEOUTS] as? MutableMap<CampaignFleetAPI, Float>
        if (timeouts !is HashMap<*, *>) {
            memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_TIMEOUTS] = HashMap<MarketAPI, CampaignFleetAPI>()
            timeouts = memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_TIMEOUTS] as MutableMap<CampaignFleetAPI, Float>
        }
        return timeouts
    }
}
