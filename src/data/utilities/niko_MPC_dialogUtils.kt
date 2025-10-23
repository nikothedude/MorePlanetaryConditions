package data.utilities

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

object niko_MPC_dialogUtils {
    @JvmStatic
    fun createSatelliteFleetFocus(
        satelliteFleet: CampaignFleetAPI, satelliteFleets: List<CampaignFleetAPI>,
        dialog: InteractionDialogAPI, entityFocus: SectorEntityToken, memoryMap: Map<String, MemoryAPI>,
        isFightingFriendly: Boolean
    ): Boolean {
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
                val dugUpEntity = digForSatellitesInEntity(entity)
                if (!dugUpEntity.hasSatellites()) niko_MPC_debugUtils.displayError("entity has no satellites in notifyLeave")
                satelliteFleet.satelliteFleetDespawn(true)
                dialog.plugin = originalPlugin
                dialog.interactionTarget = entity
                if (plugin.context is FleetEncounterContext) {
                    val context = plugin.context as FleetEncounterContext
                    if (context.didPlayerWinEncounterOutright()) {
                        /*for (handler: niko_MPC_satelliteHandlerCore in )
                        //todo: is the below needed
                        incrementSatelliteGracePeriod(
                            Global.getSector().playerFleet,
                            niko_MPC_ids.satellitePlayerVictoryIncrement,
                            entityFocus
                        ) */
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

        for (iterFleet in satelliteFleets) {
            val handler: niko_MPC_satelliteHandlerCore = iterFleet.getSatelliteEntityHandler() ?: continue
            val battle = iterFleet.battle
            val tracker: niko_MPC_satelliteBattleTracker? = niko_MPC_satelliteUtils.getSatelliteBattleTracker()
            tracker?.associateSatellitesWithBattle(satelliteFleet.battle, handler, battle.pickSide(satelliteFleet))
        }
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

    @JvmStatic
    fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
        return niko_MPC_reflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
    }
    @JvmStatic
    fun UIPanelAPI.getChildrenNonCopy() : List<UIComponentAPI>  {
        return niko_MPC_reflectionUtils.invoke("getChildrenNonCopy", this) as List<UIComponentAPI>
    }
    @JvmStatic
    fun UIComponentAPI.getParent() : UIPanelAPI {
        return niko_MPC_reflectionUtils.invoke("getParent", this) as UIPanelAPI
    }
}