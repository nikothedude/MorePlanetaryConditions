package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;

import java.util.Iterator;
import java.util.Map;

public class niko_MPC_gracePeriodDecrementer implements EveryFrameScriptWithCleanup {

    public niko_MPC_satelliteParams params;

    public niko_MPC_gracePeriodDecrementer(niko_MPC_satelliteParams params) {
        this.params = params;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
    }

    public void prepareForGarbageCollection() {
        params.gracePeriodDecrementer = null;
        params = null;
    }

    @Override
    public boolean isDone() {
        return (params == null);
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
            if (fleet == null || fleet.isExpired()) {
                iterator.remove();
                continue;
            }
            params.adjustGracePeriod(fleet, -(amount));
            entry.setValue(entry.getValue());
        }
    }
}
