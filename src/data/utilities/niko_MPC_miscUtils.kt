package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Tags.HULLMOD_DMOD
import com.fs.starfarer.api.loading.HullModSpecAPI

object niko_MPC_miscUtils {

    @JvmStatic
    fun SectorEntityToken.isOrbitalStation(): Boolean {
        return (hasTag(Tags.STATION))
    }

    @JvmStatic
    fun SectorEntityToken.getStationFleet(): CampaignFleetAPI? {
        // it seems that if the station fleet is destroyed, the station base fleet takes its place
        return memoryWithoutUpdate.getFleet(MemFlags.STATION_FLEET) ?: memoryWithoutUpdate.getFleet(MemFlags.STATION_BASE_FLEET)
    }

    @JvmStatic
    fun SectorEntityToken.getStationMarket(): MarketAPI? {
        if (market != null) return market
        val stationFleet = getStationFleet()
        if (stationFleet != null) {
            return stationFleet.getStationFleetMarket()
        }
        return null
    }

    @JvmStatic
    fun SectorEntityToken.getStationEntity(): SectorEntityToken? {
        val marketEntity = market?.primaryEntity
        if (marketEntity != null) {
            if (marketEntity == this) niko_MPC_debugUtils.log.info("found self-referencing station entity on $this, ${this.market?.name}")
            return marketEntity
        }
        val fleetEntity = getStationFleet()?.getStationFleetMarket()?.primaryEntity
        if (fleetEntity != null) {
            if (fleetEntity !== orbitFocus) {
                niko_MPC_debugUtils.log.info("$this had a fleet entity of $fleetEntity, and a orbitfocus of $orbitFocus")
            }
            return fleetEntity
        }
        return orbitFocus
    }

    @JvmStatic
    fun CampaignFleetAPI.getStationFleetMarket(): MarketAPI? {
        return memoryWithoutUpdate[MemFlags.STATION_MARKET] as? MarketAPI
    }

    @JvmStatic
    fun CampaignFleetAPI.isStationFleet(): Boolean {
        return (isStationMode || memoryWithoutUpdate.contains(MemFlags.STATION_MARKET))
    }

    @JvmStatic
    fun MarketAPI.getStationsInOrbit(): MutableList<SectorEntityToken> {
        val stations = ArrayList<SectorEntityToken>()
        for (connectedEntity: SectorEntityToken in connectedEntities) {
            if (connectedEntity.isOrbitalStation()){
                stations.add(connectedEntity)
            }
        }
        return stations
    }

    @JvmStatic
    fun SectorEntityToken.isDespawning(): Boolean {
        return (!isAlive || hasTag(Tags.FADING_OUT_AND_EXPIRING))
    }
}
