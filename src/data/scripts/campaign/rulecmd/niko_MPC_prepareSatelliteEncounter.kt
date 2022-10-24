package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_dialogUtils.createSatelliteFleetFocus
import data.utilities.niko_MPC_dialogUtils.digForSatellitesInEntity
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToFight
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

class niko_MPC_prepareSatelliteEncounter : BaseCommandPlugin() {
    override fun execute(ruleId: String, dialog: InteractionDialogAPI?, params: List<Misc.Token>, memoryMap: Map<String, MemoryAPI>): Boolean {
        if (dialog == null) return false

        var entity = dialog.interactionTarget
        entity = digForSatellitesInEntity(entity)
        if (!entity.hasSatellites()) return false

        val playerFleet = Global.getSector().playerFleet
        val entitiesWillingToFight = getNearbyEntitiesWithSatellitesWillingToFight(playerFleet)
        entitiesWillingToFight += entity
        val satelliteFleets: MutableList<CampaignFleetAPI> = ArrayList()
        var focusedSatellite: CampaignFleetAPI? = null

        for (satelliteEntity in entitiesWillingToFight) {
            for (handler: niko_MPC_satelliteHandlerCore in satelliteEntity.getSatelliteHandlers()) {
                val dialogFleet: CampaignFleetAPI? = handler.spawnSatelliteFleetForDialog(entity.containingLocation, entity.location) ?: handler.satelliteFleetForPlayerDialog
                if (dialogFleet != null) {
                    focusedSatellite = dialogFleet
                    satelliteFleets += dialogFleet
                }
            }
        }
        if (focusedSatellite == null) return false
        var isFightingFriendly = false
        for (satelliteFleet in satelliteFleets) {
            if (satelliteFleet.faction.isPlayerFaction) {
                isFightingFriendly = true
                val handler: niko_MPC_satelliteHandlerCore? = satelliteFleet.getSatelliteEntityHandler()
                if (handler != null) {
                    satelliteFleet.setFaction(handler.defaultSatelliteFactionId) //hack-the game doesnt let you fight your own faction, ever
                }
                // ^ possible issue, if this fleet is engaged in combat and is a player fleet it might fuck some shit up
            }
            val fleetMemory = satelliteFleet.memoryWithoutUpdate
            val stillSet = Misc.setFlagWithReason(
                fleetMemory,
                MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                niko_MPC_ids.satelliteFleetHostileReason,
                true,
                9999999f
            )
            if (!stillSet) {
                if (satelliteFleet.ai is ModularFleetAIAPI) {
                    val mAI = satelliteFleet.ai as ModularFleetAIAPI
                    mAI.tacticalModule.forceTargetReEval()
                }
            }
            Misc.setFlagWithReason(
                fleetMemory,
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
                niko_MPC_ids.satelliteFleetHostileReason,
                true,
                999999999f
            )
        }
        createSatelliteFleetFocus(focusedSatellite, satelliteFleets, dialog, entity, memoryMap, isFightingFriendly)
        return true
    }
}