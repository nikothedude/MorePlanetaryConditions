package data.scripts.entityPlugins;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import data.utilities.niko_MPC_settings;

public class niko_MPC_antiAsteroidSatellitesPlugin extends BaseCustomEntityPlugin {

    /**
     * The amount of XP the entity will give upon discovery.
     */
    public float discoveryXp = 5f;
    /**
     * The sensor profile of the entity, duh.
     */
    public float sensorProfile = 3f;

    @Override
    public void init(SectorEntityToken entity, Object params) {
        super.init(entity, params);

        entity.setDiscoverable(true); //nothing special needed to ensure we always see it once discovered,
                                      //it seems thats default behavior
        entity.setSensorProfile(sensorProfile);

        entity.setDiscoveryXP(discoveryXp);
    }

}
