package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidAssignmentAI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteUtils;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.util.List;

public class niko_MPC_fleetsApproachingSatellitesChecker implements EveryFrameScriptWithCleanup {

    public SectorEntityToken entity;
    public boolean done = false;
    public niko_MPC_satelliteParams satelliteParams;

    private float deltaTime = 0f;

    public niko_MPC_fleetsApproachingSatellitesChecker(niko_MPC_satelliteParams satelliteParams, SectorEntityToken entity) {
        this.satelliteParams = satelliteParams;
        this.entity = entity;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
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

    /**
     * Iterates through every single hostile fleet that's within params' interference distance. If any of them intend
     * to interact with our entity, and we are capable of blocking them, we attack them.
     * @param amount seconds elapsed during the last frame.
     */
    @Override
    public void advance(float amount) {
        deltaTime += amount;
        float thresholdForAdvancement = 0.2f;
        if (deltaTime < thresholdForAdvancement) {
            return;
        }
        else {
            deltaTime = 0;
        }

        List<CampaignFleetAPI> fleetsWithinInterferenceDistance = CampaignUtils.getNearbyFleets(getEntity(), getSatelliteParams().getSatelliteInterferenceDistance());
        for (CampaignFleetAPI fleet : fleetsWithinInterferenceDistance) {
            if (fleet == null) continue; //literally 0 idea how this can be null but okay starsector
            if (fleet == Global.getSector().getPlayerFleet()) continue;
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) continue;
            if (niko_MPC_satelliteUtils.areEntitySatellitesCapableOfBlocking(entity, fleet) && niko_MPC_satelliteUtils.doEntitySatellitesWantToBlock(entity, fleet)) {
                if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(entity, fleet)) {
                    FleetAssignmentDataAPI assignment = fleet.getCurrentAssignment();

                    if ((fleet.getInteractionTarget() == entity) || //this is inconsistant, not everything (notably raids) triggers this
                        (assignment.getTarget() == entity) ||
                        (assignment.getTarget().getOrbitFocus() == entity)) { //raids DO however have the planet as an orbit focus

                        niko_MPC_satelliteUtils.makeEntitySatellitesEngageFleet(entity, fleet);
                    }
                }
            }
        }
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    public niko_MPC_satelliteParams getSatelliteParams() {
        return satelliteParams;
    }

    public void prepareForGarbageCollection() {
        satelliteParams.approachingFleetChecker = null;

        satelliteParams = null;

        if (entity != null) {
            entity.removeScript(this);
            entity = null;
        }
        done = true;
    }
}
