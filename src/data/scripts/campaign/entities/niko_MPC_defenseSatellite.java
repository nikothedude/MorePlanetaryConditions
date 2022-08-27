package data.scripts.campaign.entities;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.campaign.CampaignEntity;
import com.fs.starfarer.campaign.CustomCampaignEntity;

public class niko_MPC_defenseSatellite extends CustomCampaignEntity { //maybe abstract it and split types into specific classes
    public int satelliteNumber; // the 1st sat has an id of 1, 2nd has 2, etc
    public String descriptionId = "niko_MPE_defenseSatellite";
    public niko_MPC_defenseSatellite(String s, String s1, String s2, String s3, float v, float v1, float v2, CampaignEntity campaignEntity, Object o) {
        super(s, s1, s2, s3, v, v1, v2, campaignEntity, o);
    }

    public void setUpSatelliteAroundSectorEntity(SectorEntityToken entity, int satelliteNumber) {
        this.addOrbitAroundSectorEntity(entity); //add an orbit around the holder of the market

        this.satelliteNumber = satelliteNumber;
    }

    public void addOrbitAroundSectorEntity(SectorEntityToken entity) {// todo: why am i using this when i initialize a satellite? why not just use the offsets instantly instead of regenning?
        addOrbitAroundSectorEntity(entity, (entity.getCircularOrbitAngle()));
    }

    public void addOrbitAroundSectorEntity(SectorEntityToken entity, float orbitAngle) {
        float orbitRadius = (entity.getRadius()); //todo: placeholder math
        float orbitDays = (entity.getCircularOrbitPeriod()); //my understanding is that this method returns how many days it takes for this object to do a complete orbit

        this.setCircularOrbitPointingDown(entity, orbitAngle, orbitRadius, orbitDays);
        //todo: pointingdown will require the sprite to be tuned for the cannons and guns and shit to face away from the planet

    }

}
