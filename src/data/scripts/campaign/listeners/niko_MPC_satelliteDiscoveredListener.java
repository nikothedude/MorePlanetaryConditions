package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.niko_MPC_satelliteUtils;

public class niko_MPC_satelliteDiscoveredListener implements DiscoverEntityListener {

    @Override
    public void reportEntityDiscovered(SectorEntityToken entity) {
        String satelliteTag = "niko_MPC_satellite";
        if (!entity.getTags().contains(satelliteTag)) return;

        niko_MPC_satelliteHandler handler = niko_MPC_satelliteUtils.getEntitySatelliteHandlerAlternate(entity);
        if (handler == null) return;

        for (CustomCampaignEntityAPI satellite : handler.getSatellites()) {
            satellite.setSensorProfile(9999999999f);
        }
    }
}
