package data.scripts.campaign.fleets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.fleets.SeededFleetManager;

public class niko_MPC_droneReplicatorSeededFleetManager extends SeededFleetManager {
    public niko_MPC_droneReplicatorSeededFleetManager(StarSystemAPI system, float inflateRangeLY) {
        super(system, inflateRangeLY);
    }

    @Override
    protected CampaignFleetAPI spawnFleet(long seed) {
        return null;
    }
}
