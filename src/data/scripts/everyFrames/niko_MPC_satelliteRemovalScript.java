package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import data.utilities.niko_MPC_debugUtils;

import java.util.Objects;

import static data.utilities.niko_MPC_satelliteUtils.purgeSatellitesFromEntity;

public class niko_MPC_satelliteRemovalScript implements EveryFrameScriptWithCleanup {
    public boolean done = false;
    public SectorEntityToken entity;
    public String conditionId;

    public niko_MPC_satelliteRemovalScript(SectorEntityToken entity, String conditionId) {
        this.entity = entity;
        this.conditionId = conditionId;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
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
        MarketAPI market = entity.getMarket();
        boolean shouldRemove = true;
        if (market != null) {
            for (MarketConditionAPI condition : market.getConditions()) {
                if (Objects.equals(condition.getId(), conditionId)) { //compare each condition and see if its ours
                    shouldRemove = false; //if it is, we dont need to remove the satellites
                    break;
                }
            }
        }
        else shouldRemove = false; //todo: to tell the truth ive got no idea of waht to do if the entity has no market. that should never happen ever

        if (shouldRemove) { //if we should remove it, we completely remove all parts of the satellite framework from the entity
            if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return;
            purgeSatellitesFromEntity(entity);
        }
        prepareForGarbageCollection(); //the point of this script is simply to check on the next frame if the condition is still present
        done = true; //and ONLY the next frame. this works because unapply() is always called on condition removal of a market,
        // meaning that no matter what, if a condition is removed, this will be added, and will check on the next frame if it was a removal or reapply.
        // since its only for the next frame, we should ALWAYS remove it, even if a condition wasnt changed, to avoid unneccessary performance loss
    }

    private void prepareForGarbageCollection() {
        if (entity != null) {
            entity.removeScript(this);
            entity = null;
        }
        conditionId = null;
    }
}
