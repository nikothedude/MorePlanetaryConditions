package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes.*
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags.HULLMOD_DMOD
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_FleetRequest.FleetType
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import org.lazywizard.lazylib.MathUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

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

    fun CampaignFleetAPI.getRepLevelForArrayBonus(): RepLevel {
        val level: RepLevel
        val fleetType: String = memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] as? String ?: return RepLevel.FRIENDLY
        when (fleetType) {
            TRADE, TRADE_SMUGGLER, TRADE_SMALL, TRADE_LINER, FOOD_RELIEF_FLEET, SHRINE_PILGRIMS, ACADEMY_FLEET -> {
                level = RepLevel.SUSPICIOUS
            }
            else -> level = RepLevel.FRIENDLY
        }
        return level
    }

    /// Returns a float from 0 to 1, representing a percentage of phase ships.
    fun CampaignFleetAPI.getPhaseShipPercent(): Float {
        var numPhaseShips = 0
        for (member in fleetData.membersListCopy) { //excludes fighers, hopefully
            if (member.isPhaseShip) numPhaseShips++
        }

        return (numMembersFast / numPhaseShips).toFloat()
    }
}