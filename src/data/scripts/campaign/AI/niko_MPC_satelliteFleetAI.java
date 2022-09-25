package data.scripts.campaign.AI;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.campaign.ai.CampaignFleetAI;
import com.fs.starfarer.campaign.ai.ModularFleetAI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

public class niko_MPC_satelliteFleetAI extends ModularFleetAI {

    public niko_MPC_satelliteFleetAI(CampaignFleet campaignFleet) {
        super(campaignFleet);
    }

    @Override
    public boolean wantsToJoin(BattleAPI battleAPI, boolean b) {
        return true;
    }
}
