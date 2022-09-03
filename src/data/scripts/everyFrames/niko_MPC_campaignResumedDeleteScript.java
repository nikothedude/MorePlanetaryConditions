package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

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
        }
        entity.getContainingLocation().removeEntity(entity);
        entity.setExpired(true);

        entity.removeScript(this);
        done = true;
    }
}
