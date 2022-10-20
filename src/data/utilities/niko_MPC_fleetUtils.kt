package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids.isSatelliteFleetId
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlerOfEntity
import org.lwjgl.util.vector.Vector2f

object niko_MPC_fleetUtils {
    /**
     * Creates an empty fleet with absolutely nothing in it, except for the memflags satellite fleets must have.
     * @return A new satellite fleet.
     */
    fun createSatelliteFleetTemplate(handler: niko_MPC_satelliteHandlerCore): CampaignFleetAPI {
        return handler.createSatelliteFleetTemplate()
    }

    /**
     * Fills fleet with the given variants up until budget isn't high enough to generate any more variants.
     * Uses a weighted picking system to determine what ships to add.
     *
     * @param budget   The amount of FP to be added to the fleet. Hard cap.
     * @param fleet    The fleet to fill.
     * @param variants The variants, in variantId -> weight format, to be picked.
     */
    fun attemptToFillFleetWithVariants(budget: Int, fleet: CampaignFleetAPI, variants: HashMap<String?, Float?>) {
        attemptToFillFleetWithVariants(budget, fleet, variants, false)
    }

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

    @JvmStatic
    @JvmOverloads
    fun despawnSatelliteFleet(fleet: CampaignFleetAPI, vanish: Boolean = false) {
        genericPreDeleteSatelliteFleetCleanup(fleet)
        if (vanish) {
            fleet.setLocation(9999999f, 9999999f)
        }
        fleet.despawn() //will ALWAYS call the despawn listener
    }

    @JvmStatic
    fun genericPreDeleteSatelliteFleetCleanup(fleet: CampaignFleetAPI) {
        fleet.orbit = null
        val fleetMemory = fleet.memoryWithoutUpdate
        val script = fleetMemory[niko_MPC_ids.temporaryFleetDespawnerId] as niko_MPC_temporarySatelliteFleetDespawner
        script?.prepareForGarbageCollection()
        val handler = getSatelliteHandlerOfEntity(fleet)
        handler?.cleanUpSatelliteFleetBeforeDeletion(fleet)
    }

    fun spawnSatelliteFleet(
        handler: niko_MPC_satelliteHandlerCore,
        coordinates: Vector2f?,
        location: LocationAPI?
    ): CampaignFleetAPI {
        return spawnSatelliteFleet(handler, coordinates, location, true, false)
    }

    fun spawnSatelliteFleet(
        handler: niko_MPC_satelliteHandlerCore,
        coordinates: Vector2f?,
        location: LocationAPI?,
        temporary: Boolean,
        dummy: Boolean
    ): CampaignFleetAPI {
        return handler.spawnSatelliteFleet(coordinates!!, location!!, temporary, dummy)
    }

    @JvmStatic
    fun getHandlerDialogFleet(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken): CampaignFleetAPI? {
        if (handler.fleetForPlayerDialog == null) {
            handler.fleetForPlayerDialog = createNewFullSatelliteFleet(handler, entity)
        }
        return handler.fleetForPlayerDialog
    }

    fun createNewFullSatelliteFleet(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken): CampaignFleetAPI {
        return createNewFullSatelliteFleet(handler, entity, true)
    }

    fun createNewFullSatelliteFleet(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken, temporary: Boolean): CampaignFleetAPI {
        return createNewFullSatelliteFleet(handler, entity.location, entity.containingLocation, temporary, false)
    }

    fun createNewFullSatelliteFleet(handler: niko_MPC_satelliteHandlerCore, coordinates: Vector2f?, location: LocationAPI?, temporary: Boolean, dummy: Boolean): CampaignFleetAPI {
        return handler.createNewFullSatelliteFleet(coordinates, location, temporary, dummy)
    }

    @JvmStatic
    fun fleetIsSatelliteFleet(fleet: CampaignFleetAPI): Boolean {
        return fleet.memoryWithoutUpdate.`is`(isSatelliteFleetId, true)
    }

    fun createDummyFleet(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken?): CampaignFleetAPI {
        return handler.createDummyFleet(entity!!)
    }

    @JvmStatic
    fun isFleetValidEngagementTarget(fleet: CampaignFleetAPI?): Boolean {
        if (fleet == null) return false
        if (fleet === Global.getSector().playerFleet) return false
        return if (fleetIsSatelliteFleet(fleet)) false else true
    }

    fun CampaignFleetAPI.satelliteFleetDespawn(vanish: Boolean = false) {
        if (isSatelliteFleet()) {
            genericPreDeleteSatelliteFleetCleanup(this) //todo come back to this and clean it up
            if (vanish) {
                setLocation(9999999f, 9999999f)
            }
        }
        despawn() //will ALWAYS call the despawn listener
    }
}