package data.utilities

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.combat.WeaponAPI
import data.utilities.niko_MPC_miscUtils.isStationFleet

object niko_MPC_battleUtils {

    fun WeaponAPI.isPD(): Boolean {
        return (hasAIHint(WeaponAPI.AIHints.PD) || hasAIHint(WeaponAPI.AIHints.PD_ALSO))
    }
    @JvmStatic
    fun BattleAPI.getContainingLocation(): LocationAPI? {
        if (Global.getCurrentState() != GameState.CAMPAIGN && !Global.getCombatEngine().isInCampaign) {
            niko_MPC_debugUtils.log.info("$this not in campaign, returning null for getContainingLocation()")
            return null //todo: is this a bad idea
        }
        var containingLocation: LocationAPI? = null
        val sector: SectorAPI? = Global.getSector()
        val playerFleet: CampaignFleetAPI? = sector?.playerFleet
        if (isPlayerInvolved && playerFleet != null) {
            containingLocation = Global.getSector()?.playerFleet?.containingLocation
        } else {
            for (fleet in bothSides) { //have to do this, because some fleet dont HAVE a containing location
                if (fleet.containingLocation != null) { //ideally, this will only iterate once or twice before finding a location
                    containingLocation = fleet.containingLocation
                    break //we found a location, no need to check everyone else
                }
            }
        }
        return containingLocation
    }

    @JvmStatic
    fun BattleAPI.getStationFleet(): CampaignFleetAPI? {
        val stationFleets = getStationFleets()
        return stationFleets.firstOrNull()
    }

    @JvmStatic
    private fun BattleAPI.getStationFleets(): List<CampaignFleetAPI> {
        val stationFleets = ArrayList<CampaignFleetAPI>()
        for (potentialStationFleet in stationSide) {
            if (potentialStationFleet.isStationFleet()) {
                stationFleets += potentialStationFleet
            }
        }
        if (stationFleets.size > 1) {
            niko_MPC_debugUtils.displayError("$this had more than 1 station fleet during getStationFleets()")
        }
        return stationFleets
    }

    @JvmStatic
    fun isSideValid(side: BattleAPI.BattleSide?): Boolean {
        return side != BattleAPI.BattleSide.NO_JOIN && side != null
    }
}