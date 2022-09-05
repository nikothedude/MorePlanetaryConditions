package data.utilities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.List;

import static data.utilities.niko_MPC_satelliteUtils.getSatellitesInOrbitOfEntity;
import static java.lang.Math.round;

public class niko_MPC_planetUtils {

    public static float getOptimalOrbitalOffsetForSatellites(SectorEntityToken entity) {
        return getOptimalOrbitalOffsetForSatellites(getSatellitesInOrbitOfEntity(entity));
    }

    public static float getOptimalOrbitalOffsetForSatellites(List<CustomCampaignEntityAPI> satelliteInOrbitOfEntity) {
        int numOfSatellites = satelliteInOrbitOfEntity.size();

        float optimalAngle = (360 / (float) numOfSatellites); //todo: explain the math

        if (optimalAngle == 360) {
            optimalAngle = 0; //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle;
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity) {
        return getMaxPhysicalSatellitesBasedOnEntitySize(entity, 5);
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity, float radiusDivisor) {
        return ((round((entity.getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

}
