package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import static data.utilities.niko_MPC_fleetUtils.safeDespawnFleet;
import static data.utilities.niko_MPC_ids.isSatelliteFleetId;

public class niko_MPC_campaignResumedDeleteScript implements EveryFrameScriptWithCleanup {

    public boolean done = false;
    public SectorEntityToken entity;

    public niko_MPC_campaignResumedDeleteScript(SectorEntityToken entity) {
        this.entity = entity;
    }

    @Override
    public void cleanup() {
        entity = null;
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (entity instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
            if (fleet.getBattle() != null) {
                return;
            }
            safeDespawnFleet(fleet);
        }
        else {
            entity.getContainingLocation().removeEntity(entity);
            entity.setExpired(true);
        }
        entity.removeScript(this);
        done = true;
    }
}
