package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI

object niko_MPC_battleUtils {
    @JvmStatic
    fun getContainingLocationOfBattle(battle: BattleAPI): LocationAPI? {
        var containingLocation: LocationAPI? = null
        if (battle.isPlayerInvolved) {
            containingLocation = Global.getSector().playerFleet.containingLocation
        } else {
            for (fleet in battle.bothSides) { //have to do this, because some fleet dont HAVE a containing location
                if (fleet.containingLocation != null) { //ideally, this will only iterate once or twice before finding a location
                    containingLocation = fleet.containingLocation
                    break //we found a location, no need to check everyone else
                }
            }
        }
        return containingLocation
    }

    @JvmStatic
    fun getStationsFleetOfBattle(battle: BattleAPI): List<CampaignFleetAPI> {
        val stationFleets = ArrayList<CampaignFleetAPI>()
        for (potentialStationFleet in battle.stationSide) {
            if (potentialStationFleet.isStationMode) {
                stationFleets += potentialStationFleet
            }
        }
        return stationFleets
    }

    @JvmStatic
    fun isSideValid(side: BattleAPI.BattleSide?): Boolean {
        return side != BattleAPI.BattleSide.NO_JOIN && side != null
    }
}