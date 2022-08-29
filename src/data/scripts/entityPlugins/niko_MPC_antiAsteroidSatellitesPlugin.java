package data.scripts.entityPlugins;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;

public class niko_MPC_antiAsteroidSatellitesPlugin extends BaseCustomEntityPlugin {

    public float discoveryXp = 5f;
    public float sensorProfile = 1f;

    @Override
    public void init(SectorEntityToken entity, Object params) { //todo: i can make a custom init/constructor since this is its own class
        super.init(entity, params);

        entity.setDiscoverable(true); //nothing special needed to ensure we always see it once discovered,
                                      //it seems thats default behavior
        entity.setSensorProfile(sensorProfile);

        entity.setDiscoveryXP(discoveryXp);
    }

}
