package data.utilities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class niko_MPC_orbitUtils {

    public static void addOrbitPointingDownWithRelativeOffset(CustomCampaignEntityAPI satellite, SectorEntityToken entity) {
        addOrbitPointingDownWithRelativeOffset(satellite, entity, 0, (entity.getRadius()) + 15f);
    }

    public static void addOrbitPointingDownWithRelativeOffset(CustomCampaignEntityAPI satellite, SectorEntityToken entity, float orbitAngle) {
        addOrbitPointingDownWithRelativeOffset(satellite, entity, orbitAngle, (entity.getRadius()) + 15f);
    }

    public static void addOrbitPointingDownWithRelativeOffset(CustomCampaignEntityAPI satellite, SectorEntityToken entity, float orbitAngle, float orbitRadius) {
        float orbitDays = 15f; //todo: placeholder
        //DO NOT IGNORE THIS COMMENT
        //entity.getCircularOrbitPeriod() will return 0 if the entity does not orbit! THIS WILL CAUSE A JSONEXCEPTION ON SAVE! DO NOT! ENTER 0!

        satellite.setCircularOrbitPointingDown(entity, orbitAngle, orbitRadius, orbitDays);
    }
}
