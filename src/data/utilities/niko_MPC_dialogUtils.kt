package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import data.utilities.niko_MPC_debugUtils.assertEntityHasSatellites
import data.utilities.niko_MPC_fleetUtils.despawnSatelliteFleet
import data.utilities.niko_MPC_satelliteUtils.incrementSatelliteGracePeriod

object niko_MPC_dialogUtils {
    @JvmStatic
    fun createSatelliteFleetFocus(
        satelliteFleet: CampaignFleetAPI?, satelliteFleets: List<CampaignFleetAPI?>?,
        dialog: InteractionDialogAPI?, entityFocus: SectorEntityToken, memoryMap: Map<String?, MemoryAPI?>?,
        isFightingFriendly: Boolean
    ): Boolean {
        if (dialog == null) return false
        if (satelliteFleet == null) return false
        val entity = dialog.interactionTarget
        dialog.interactionTarget = satelliteFleet
        val config = FIDConfig()
        config.leaveAlwaysAvailable = true
        config.showCommLinkOption = false
        config.showEngageText = false
        config.showFleetAttitude = false
        config.showTransponderStatus = false
        config.showWarningDialogWhenNotHostile = false
        config.alwaysAttackVsAttack = true
        config.impactsAllyReputation = true
        config.impactsEnemyReputation = true
        config.pullInAllies = true
        config.pullInEnemies = true
        config.pullInStations = true
        config.lootCredits = true
        if (isFightingFriendly) {
            config.lootCredits = false
            config.impactsAllyReputation = false
            config.impactsEnemyReputation = false
        }
        val entityMarket = entityFocus.market
        if (entityMarket != null) {
            if (entityMarket.isPlanetConditionMarketOnly) {
                config.impactsEnemyReputation = false
                config.impactsAllyReputation = false
            }
        }
        config.firstTimeEngageOptionText = "Engage the automated defenses"
        config.afterFirstTimeEngageOptionText = "Re-engage the automated defenses"
        config.noSalvageLeaveOptionText = "Continue"
        config.dismissOnLeave = false
        config.printXPToDialog = true
        val plugin = FleetInteractionDialogPluginImpl(config)
        val originalPlugin = dialog.plugin
        config.delegate = object : BaseFIDDelegate() {
            override fun notifyLeave(dialog: InteractionDialogAPI) {
                if (!assertEntityHasSatellites(entityFocus)) return
                despawnSatelliteFleet(satelliteFleet, true)
                dialog.plugin = originalPlugin
                dialog.interactionTarget = entity
                if (plugin.context is FleetEncounterContext) {
                    val context = plugin.context as FleetEncounterContext
                    if (context.didPlayerWinEncounterOutright()) {
                        //todo: is the below needed
                        incrementSatelliteGracePeriod(
                            Global.getSector().playerFleet,
                            niko_MPC_ids.satellitePlayerVictoryIncrement,
                            entityFocus
                        )
                        FireBest.fire(null, dialog, memoryMap, "niko_MPC_DefenseSatellitesDefeated")
                    } else {
                        dialog.dismiss()
                    }
                } else {
                    dialog.dismiss()
                }
            }
        }
        dialog.plugin = plugin
        plugin.init(dialog)
        return true
    }

    /**
     * Gets entity's market. If it isn't null, we get the entity of market, then set entity to that.
     * @param entity The entity to dig through the memory/variables of.
     * @return Whatever entity is the primaryentity of entity's market. If it has no market, returns entity.
     */
    @JvmStatic
    fun digForSatellitesInEntity(entity: SectorEntityToken): SectorEntityToken {
        var mutableEntity = entity
        val entityMarket = mutableEntity.market
        if (entityMarket != null && entityMarket.primaryEntity !== mutableEntity) {
            val marketEntity = entityMarket.primaryEntity
            if (marketEntity != null) mutableEntity = marketEntity
        }
        return mutableEntity
    }
}