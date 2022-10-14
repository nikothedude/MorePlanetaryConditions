package data.utilities

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken

object niko_MPC_orbitUtils {

    @JvmStatic
    fun addOrbitPointingDownWithRelativeOffset(satellite: CustomCampaignEntityAPI, entity: SectorEntityToken, orbitAngle: Float = 0f,
                                               orbitRadius: Float = entity.radius + 15f) {
        val orbitDays = 15f //placeholder
        //DO NOT IGNORE THIS COMMENT
        //entity.getCircularOrbitPeriod() will return 0 if the entity does not orbit! THIS WILL CAUSE A JSONEXCEPTION ON SAVE! DO NOT! ENTER 0!
        satellite.setCircularOrbitPointingDown(entity, orbitAngle, orbitRadius, orbitDays)
    }
}