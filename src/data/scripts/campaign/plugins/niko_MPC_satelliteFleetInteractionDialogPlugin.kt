package data.scripts.campaign.plugins

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet

// cant be used as for some godforsaken reason we cant just implement goddamn fleet interacton dialogs
// wed need to override marketcmd
class niko_MPC_satelliteFleetInteractionDialogPlugin : FleetInteractionDialogPluginImpl() {
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