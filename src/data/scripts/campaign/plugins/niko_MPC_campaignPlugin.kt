package data.scripts.campaign.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.campaign.ai.TacticalModulePlugin
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAITacticalModule
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.magnetar.AIPlugins.MPC_slavedOmegaCoreAdminPlugin
import data.scripts.campaign.magnetar.AIPlugins.MPC_slavedOmegaCoreOfficerPlugin
import data.scripts.campaign.magnetar.MPC_derelictOmegaDerelictInflater
import data.scripts.campaign.magnetar.niko_MPC_derelictOmegaFleetConstructor
import data.scripts.campaign.singularity.MPC_ultimaJumpPointInteractionPlugin
import data.utilities.*
import data.utilities.niko_MPC_battleUtils.getStationFleet
import data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId
import data.utilities.niko_MPC_miscUtils.getStationFleet
import data.utilities.niko_MPC_miscUtils.getStationFleetMarket
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.hasSatellites
import data.utilities.niko_MPC_satelliteUtils.isSatelliteEntity
import org.lwjgl.util.vector.Vector2f

class niko_MPC_campaignPlugin : BaseCampaignPlugin() {
    override fun getId(): String {
        return niko_MPC_campaignPluginId
    }

    override fun isTransient(): Boolean {
        return true
    }

    /**
     * A hack that causes satellite fleets to spawn whenever the player interacts with anything. This is mandatory for
     * ensuring proper behavior in things like interacting with a fleet over jangala and having jangala's satellites appear
     * if you choose to fight it, or having jangala's satellites defend its station in a player-driven battle.
     *
     *
     * The station searching part of this method is necessary due to the way stations work. Since I always want satellites to interfere with a station battle
     * if the station is associated with the satellite planet, regardless of distance, I need to always find the station's
     * entity, E.G. either itself or the planet it orbits, so I can manually add it to the list of entities to try to spawn
     * satellites from, bypassing the distance check.
     *
     * @param interactionTarget If this is a fleet, checks to see if it has a battle. If it does, and if a station is involved in it,
     * scans every fleet on the side of the station to see if it's the station fleet. Once it finds it, it will grab the market
     * of the fleet, then return the primary entity of that market, which can, in 99% of cases, be either the planet
     * the station orbits, or the station itself, in the case of say, kantas den. 99% sure a getMarket() call doesn't
     * work here, as the fleet doesn't hold a ref to the market directly.
     *
     *
     * If it is NOT a fleet, we can assume it is either a station of a market, a market holder itself, or something mundane. In either
     * case, we know it's market is stored in getMarket(), so we get that, then get the result's getEntity(), which
     * may or may not be interactionTarget, or something else.
     *
     * @return An interactionplugin. Can either be null or a instance of satelliteInteractionDialogPlugin, depending on if
     * interactionTarget is engaged in a battle or not.
     */
    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken?): PluginPick<InteractionDialogPlugin>? {
        if (interactionTarget == null) return null

        if (interactionTarget.customEntityType == Entities.DERELICT_GATEHAULER) {
            Global.getSector().memoryWithoutUpdate["\$MPC_playerFoundGatehauler"] = true
        }
        doSatelliteStuff(interactionTarget)

        if (interactionTarget is JumpPointAPI && (interactionTarget.hasTag("MPC_ultimaSingularityJumpPoint"))) {
            return PluginPick(MPC_ultimaJumpPointInteractionPlugin(), CampaignPlugin.PickPriority.MOD_SPECIFIC)
        }
        return null
    }

    private fun doSatelliteStuff(interactionTarget: SectorEntityToken) {
        // SATELLITE HANDLING BELOW
        if (interactionTarget.shouldSkip()) return

        var entityToExpandRadiusFrom: SectorEntityToken? = null // this entity will always be checked to see if it should deploy satellites
        var battle: BattleAPI? = null
        var targetFleet: CampaignFleetAPI? = null
        if (interactionTarget is CampaignFleetAPI) {
            targetFleet = interactionTarget // we're interacting with a fleet
            battle = targetFleet.battle
            if (battle != null) {
                if (battle.isStationInvolved) { // the literal only situation in which we should try to dig for an entity
                    val stationFleet: CampaignFleetAPI? = battle.getStationFleet()
                    // ^ this is necessary due to the fact that we still need to get the primaryentity of the station the
                    // battle has involved. we have no link to the station other than the station fleet which holds a ref to
                    // the market holding the station. the story is different if we interact with the station itself,
                    // which we handle in the else statement lower down in this method
                    if (stationFleet == null) { // this should never happen, but lets just be safe
                        niko_MPC_debugUtils.displayError("null stationfleet when expecting station, interaction target: " + interactionTarget.getName())
                        return
                    }
                    val primaryEntity = stationFleet.getStationFleetMarket()?.primaryEntity
                    if (primaryEntity != null) { //doesnt matter if we get a station or not, we got the primary entity
                        if (primaryEntity.hasSatellites()) entityToExpandRadiusFrom = primaryEntity
                        //im leaving this for sanity
                        // since this returns the primaryentity of the market... we SHOULD be able to
                        // always know if its own market or not? since it can be itself?
                    }
                }
            }
        } else { // we're interacting with a non-fleet entity, meaning theres a chance it might be a station
            val dugUpEntity = niko_MPC_dialogUtils.digForSatellitesInEntity(interactionTarget)
            if (interactionTarget.hasTag(Tags.STATION)) { //we interacted with dugupentity's station
                // ^ means it must have a fleet, so let's get it
                targetFleet = interactionTarget.getStationFleet()
                // we're only getting this info to pass it to the spawn method
                // you may ask "why pass it"? good question i havent thought it through but it works lol
                if (targetFleet != null) battle = targetFleet.battle
            }
            if (dugUpEntity.hasSatellites()) { // to avoid an error message
                entityToExpandRadiusFrom = dugUpEntity //lets make it so the station's entity knows to deploy satellites
            }
        }
        spawnSatelliteFleetsOnPlayerIfAble(interactionTarget, targetFleet, battle, entityToExpandRadiusFrom)
    }

    private fun SectorEntityToken.shouldSkip(): Boolean {
        return (isSatelliteEntity())
    }

    /* this is such a bad idea
    // order is this, then reportFleetSpawned/pick autoresolve/reportbattleoccured
    override fun pickTacticalAIModule(fleet: CampaignFleetAPI?, ai: ModularFleetAIAPI?): PluginPick<TacticalModulePlugin>? {

        if (fleet == null) return null

        if (!fleet.isCombiningFromBattle()) return null
        return PluginPick(niko_MPC_satelliteFleetAITacticalModule(fleet as CampaignFleet, ai), CampaignPlugin.PickPriority.MOD_SET)
    }

    private fun BattleAPI.isCombining(): Boolean {
        val combiningCheck = (combinedOne == null || combinedTwo == null)
        return combiningCheck
    }

    private fun CampaignFleetAPI.isCombiningFromBattle(): Boolean { // this is so fucking rickety and can fail so easily
        return (fleetData.isOnlySyncMemberLists
                && !isAIMode)
    }

    override fun pickBattleAutoresolverPlugin(battle: BattleAPI?): PluginPick<BattleAutoresolverPlugin>? {
        if (battle == null) return null
        return super.pickBattleAutoresolverPlugin(battle)
    } */

    private fun spawnSatelliteFleetsOnPlayerIfAble(interactionTarget: SectorEntityToken, fleet: CampaignFleetAPI?, battle: BattleAPI?, entityToAlwaysTryToSpawnFrom: SectorEntityToken?)
    : List<CampaignFleetAPI> {

        // we only spawn things for PROTECTION here not ATTACK. this means we want these fleets to be NEAR the entity,
        // but not actively attacking, this is so that if a friendly player decides to attack the market, they will
        // still have to go through the satellites

        //tldr this method exists so if the player enters a battle the satellites will interfere
        val spawnedFleets: MutableList<CampaignFleetAPI> = ArrayList()
        val sector = Global.getSector() ?: return spawnedFleets
        val playerFleet: CampaignFleetAPI? = sector.playerFleet
        var locationToScanFrom: LocationAPI? = interactionTarget.containingLocation
        var coordinatesToScanFrom: Vector2f? = interactionTarget.location
        if (locationToScanFrom == null || coordinatesToScanFrom == null) {
            locationToScanFrom = fleet?.containingLocation
            coordinatesToScanFrom = fleet?.location
            if (locationToScanFrom == null || coordinatesToScanFrom == null) {
                locationToScanFrom = playerFleet?.containingLocation
                coordinatesToScanFrom = playerFleet?.location
            }
        }
        if (locationToScanFrom == null || coordinatesToScanFrom == null) {
            niko_MPC_debugUtils.log.info("unable to find a suitable containing location or coordinates for campaign plugin satellite fleet spawn." +
                    " entity: $interactionTarget, ${interactionTarget.name}")
            return spawnedFleets
        }

        val entitiesWithinRange = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellites(coordinatesToScanFrom, locationToScanFrom)
        if (entityToAlwaysTryToSpawnFrom != null && entityToAlwaysTryToSpawnFrom.hasSatellites()) {
            entitiesWithinRange += entityToAlwaysTryToSpawnFrom
        }
        for (entityInRange: SectorEntityToken in entitiesWithinRange) {
            for (handler: niko_MPC_satelliteHandlerCore in ArrayList(entityInRange.getSatelliteHandlers())) {
                val spawnedFleet: CampaignFleetAPI? = handler.interfereForCampaignPlugin(fleet, playerFleet, battle)
                if (spawnedFleet != null) spawnedFleets += spawnedFleet
            }
        }
        return spawnedFleets
    }

    override fun pickFleetInflater(fleet: CampaignFleetAPI?, params: Any?): PluginPick<FleetInflater>? {
        if (fleet == null) return null
        if (params is DefaultFleetInflaterParams) {
            if (fleet.faction.id == niko_MPC_ids.derelictOmegaConstructorFactionId) {
                return PluginPick(MPC_derelictOmegaDerelictInflater(params), CampaignPlugin.PickPriority.MOD_SET)
            }
        }
        return null
    }

    override fun pickAICoreOfficerPlugin(commodityId: String?): PluginPick<AICoreOfficerPlugin>? {
        if (commodityId == null) return null
        if (commodityId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            return PluginPick(MPC_slavedOmegaCoreOfficerPlugin(), CampaignPlugin.PickPriority.MOD_SET)
        }
        return null
    }

    override fun pickAICoreAdminPlugin(commodityId: String?): PluginPick<AICoreAdminPlugin>? {
        if (commodityId == null) return null
        if (commodityId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            return PluginPick(MPC_slavedOmegaCoreAdminPlugin(), CampaignPlugin.PickPriority.MOD_SET)
        }
        return null
    }
}
