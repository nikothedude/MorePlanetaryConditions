package data.scripts.campaign.plugins;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import data.utilities.niko_MPC_fleetUtils;

import java.util.List;

public class niko_MPC_satelliteInteractionDialogPlugin extends FleetInteractionDialogPluginImpl {

    @Override
    protected boolean fleetWantsToDisengage(CampaignFleetAPI fleet, CampaignFleetAPI other) {
        boolean result = super.fleetWantsToDisengage(fleet, other);
        BattleAPI battle = context.getBattle();

        if (battle != null) {
            List<CampaignFleetAPI> fleetsForFirst = battle.getSideFor(fleet);
            for (CampaignFleetAPI potentialSatellite : fleetsForFirst) {
                if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(potentialSatellite)) {
                    return false;
                }
            }
        }
        return result;
    }

    @Override
    protected boolean fleetWantsToFight(CampaignFleetAPI fleet, CampaignFleetAPI other) {
        boolean result = super.fleetWantsToFight(fleet, other);

        BattleAPI battle = context.getBattle();

        if (battle != null) {
            List<CampaignFleetAPI> fleetsForFirst = battle.getSideFor(fleet);
            for (CampaignFleetAPI potentialSatellite : fleetsForFirst) {
                if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(potentialSatellite)) {
                    result = super.fleetWantsToFight(potentialSatellite, other);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    protected boolean fleetHoldingVsStrongerEnemy(CampaignFleetAPI fleet, CampaignFleetAPI other) {
        boolean result = super.fleetHoldingVsStrongerEnemy(fleet, other);

        BattleAPI battle = context.getBattle();

        if (battle != null) {
            List<CampaignFleetAPI> fleetsForFirst = battle.getSideFor(fleet);
            for (CampaignFleetAPI potentialSatellite : fleetsForFirst) {
                if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(potentialSatellite)) {
                    result = super.fleetHoldingVsStrongerEnemy(potentialSatellite, other);
                    break;
                }
            }
        }
        return result;
    }
}
