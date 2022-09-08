package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;

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
        params = null;
    }

    @Override
    public boolean isDone() {
        return (params == null || params.getGracePeriod() <= 0);
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        params.adjustGracePeriod(amount * -1);
    }
}
