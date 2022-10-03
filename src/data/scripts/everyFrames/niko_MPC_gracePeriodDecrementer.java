package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;

import java.util.Iterator;
import java.util.Map;

public class niko_MPC_gracePeriodDecrementer implements EveryFrameScriptWithCleanup {

    public niko_MPC_satelliteHandler params;
    public boolean done = false;

    public niko_MPC_gracePeriodDecrementer(niko_MPC_satelliteHandler params) {
        this.params = params;

        init();
    }

    private void init() {
        if (params == null) {
            prepareForGarbageCollection();
        }
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
    }

    public void prepareForGarbageCollection() {
        params.gracePeriodDecrementer = null;
        params = null;

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
        Iterator<Map.Entry<CampaignFleetAPI, Float>> iterator = params.getGracePeriods().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CampaignFleetAPI, Float> entry = iterator.next();
            CampaignFleetAPI fleet = entry.getKey();
            if (fleet == null || fleet.isExpired() || fleet.hasTag(Tags.FADING_OUT_AND_EXPIRING)) { // to prevent memory leaks, we have to account for if the fleet is null or expired
                iterator.remove(); // we dont remove on 0, because honestly theres no real reason to, we already remove them if they are deleted
                continue;
            }
            params.adjustGracePeriod(fleet, -(amount)); // for each fleet tracked, decrement the grace period by amount
        }
    }
}
