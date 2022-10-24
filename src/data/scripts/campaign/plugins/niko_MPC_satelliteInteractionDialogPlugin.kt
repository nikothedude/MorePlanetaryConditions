package data.scripts.campaign.plugins

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet

//todo: consider removing, now that i have it so that satellites flip to the other side of the map on pursuits
class niko_MPC_satelliteInteractionDialogPlugin : FleetInteractionDialogPluginImpl() {
    override fun fleetWantsToDisengage(fleet: CampaignFleetAPI, other: CampaignFleetAPI): Boolean {
        val result = super.fleetWantsToDisengage(fleet, other)
        val battle = context.battle
        if (battle != null) {
            val fleetsForFirst = battle.getSideFor(fleet)
            for (potentialSatellite in fleetsForFirst) {
                if (potentialSatellite.isSatelliteFleet()) {
                    return false
                }
            }
        }
        return result
    }

    override fun fleetWantsToFight(fleet: CampaignFleetAPI, other: CampaignFleetAPI): Boolean {
        var result = super.fleetWantsToFight(fleet, other)
        val battle = context.battle
        if (battle != null) {
            val fleetsForFirst = battle.getSideFor(fleet)
            for (potentialSatellite in fleetsForFirst) {
                if (potentialSatellite.isSatelliteFleet()) {
                    result = super.fleetWantsToFight(potentialSatellite, other)
                    break
                }
            }
        }
        return result
    }

    override fun fleetHoldingVsStrongerEnemy(fleet: CampaignFleetAPI, other: CampaignFleetAPI): Boolean {
        var result = super.fleetHoldingVsStrongerEnemy(fleet, other)
        val battle = context.battle
        if (battle != null) {
            val fleetsForFirst = battle.getSideFor(fleet)
            for (potentialSatellite in fleetsForFirst) {
                if (potentialSatellite.isSatelliteFleet()) {
                    result = super.fleetHoldingVsStrongerEnemy(potentialSatellite, other)
                    break
                }
            }
        }
        return result
    }
}