package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_ids.isSatelliteFleetId
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlerOfEntity
import org.lwjgl.util.vector.Vector2f

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

    fun CampaignFleetAPI.setSatelliteEntityHandler(handler: niko_MPC_satelliteHandlerCore) {
        memory[niko_MPC_ids.satelliteEntityHandler] = handler
    }

    /** TODO finish */
    fun CampaignFleetAPI.trimDownToFP(maxFp: Float) {
        var failsafeIndex = 0
        val failsafeThreshold = 25 //25 iterations before we break out
        while (effectiveStrength > maxFp) {
            if (++failsafeIndex >= failsafeThreshold) {
                displayError("$this trimdown interrupted due to failsafe Index exceeding or reaching $failsafeThreshold")
                logDataOf(this)
                return
            }
            for (fleetMember: FleetMemberAPI in fleetData.membersListCopy) {
                fleetData.removeFleetMember(fleetMember)
                if (effectiveStrength <= maxFp) return
            }
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
}