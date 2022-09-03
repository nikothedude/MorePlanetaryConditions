package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.util.List;

public class niko_MPC_satelliteTrackerAreaScanner implements EveryFrameScript {

    public SectorEntityToken entity;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {

        List<CampaignFleetAPI> a = CampaignUtils.getNearbyEntitiesWithRep(entity, 4, "d", CampaignUtils.IncludeRep.AT, RepLevel.COOPERATIVE,) {
            return;
        }

        List<CampaignFleetAPI> testList = CampaignUtils.getNearbyFleets(entity, 50f) //todo: placeholder float also check if we need to always see

        CampaignFleetAPI fleet = CampaignUtils.getNearestHostileFleet(entity);

        if (fleet.getBattle() != null) {

        }
    }
}
