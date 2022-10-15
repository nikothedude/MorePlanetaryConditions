package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI

object niko_MPC_battleUtils {
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
    fun getStationFleetOfBattle(battle: BattleAPI): CampaignFleetAPI? {
        for (potentialStationFleet in battle.stationSide) {
            if (potentialStationFleet.isStationMode) {
                return potentialStationFleet // fun fact about battles, there can only ever be one fleet with isstationmode set to true in a battle
                // so, this is risk-free
            }
        }
        return null
    }
}