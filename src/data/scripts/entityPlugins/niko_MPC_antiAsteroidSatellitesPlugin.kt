package data.scripts.entityPlugins

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin

class niko_MPC_antiAsteroidSatellitesPlugin : BaseCustomEntityPlugin() {
    /**
     * The amount of XP the entity will give upon discovery.
     */
    var discoveryXp = 5f

    /**
     * The sensor profile of the entity, duh.
     */
    var sensorProfile = 3f
    override fun init(entity: SectorEntityToken?, params: Any?) {
        super.init(entity, params)
        if (entity == null) return
        entity.isDiscoverable = true //nothing special needed to ensure we always see it once discovered,
        //it seems thats default behavior
        entity.sensorProfile = sensorProfile
        entity.discoveryXP = discoveryXp
    }
}