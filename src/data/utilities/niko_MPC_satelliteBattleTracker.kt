package data.utilities

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import data.scripts.campaign.misc.niko_MPC_satelliteHandler
import data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteHandler

/**
 * Save-specific global list that stores a hashmap of battleAPI->(satellitehandler->battleside).
 */
class niko_MPC_satelliteBattleTracker {
    var battles: MutableMap<BattleAPI, MutableMap<niko_MPC_satelliteHandler, BattleSide>?> = HashMap()

    /**
     * Associates a hashmap entry of (battle->(handler->side)). The side is stored for future reference.
     *
     *
     * If the hashmap doesnt contain battle, or if the v alue of battle is null, instantiates a new hashmap and assigns (battle->Hashmap()).
     * @param battle The battle to associate with.
     * @param handler The satellite handler we are associating.
     * @param side The stored side of the battle.
     */
    fun associateSatellitesWithBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandler, side: BattleSide) {
        if (!battles.containsKey(battle) || battles[battle] == null) battles[battle] = HashMap()
        val currentBattles = battles[battle]
        currentBattles!![handler] = side
    }

    fun getBattles(): Map<BattleAPI, MutableMap<niko_MPC_satelliteHandler, BattleSide>?> {
        return battles
    }

    fun getSatellitesInfluencingBattle(battle: BattleAPI): Set<niko_MPC_satelliteHandler> {
        return if (battles[battle] == null) HashSet() else battles[battle]!!.keys
    }

    fun getSideOfSatellitesForBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandler): BattleSide? {
        return if (battles[battle] == null) null else battles[battle]!![handler]
    }

    fun areSatellitesInvolvedInBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandler): Boolean {
        return if (battles.containsKey(battle)) {
            if (battles[battle]!!.containsKey(handler)) {
                true
            } else {
                /*updateBattleAssociationWithScan(battle);
                return battles.get(battle).containsKey(handler);*/
                false
            }
        } else false
    }

    fun areAnySatellitesInvolvedInBattle(battle: BattleAPI): Boolean {
        return battles[battle] != null && !battles[battle]!!.isEmpty()
    }

    fun scanBattleForSatellites(battle: BattleAPI): HashMap<niko_MPC_satelliteHandler, CampaignFleetAPI> {
        val handlerToFleetMap = HashMap<niko_MPC_satelliteHandler, CampaignFleetAPI>()
        for (fleet in battle.bothSides) {
            val foundhandler = getEntitySatelliteHandler(fleet)
            if (foundhandler != null) {
                handlerToFleetMap[foundhandler] = fleet
            }
        }
        return handlerToFleetMap
    }

    fun updateBattleAssociationWithScan(battle: BattleAPI) {
        val handlerToFleetMap = scanBattleForSatellites(battle)
        for ((handler, fleet) in handlerToFleetMap) {
            if (!areSatellitesInvolvedInBattle(battle, handler)) {
                val handlerToSideMap = HashMap<niko_MPC_satelliteHandler, BattleSide>()
                handlerToSideMap[handler] = battle.pickSide(fleet)
                battles[battle] = handlerToSideMap
            }
        }
    }

    fun removeBattle(battle: BattleAPI) {
        battles.remove(battle)
    }

    fun removeHandlerFromBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandler) {
        if (battles[battle] != null) {
            battles[battle]!!.remove(handler)
        }
    }

    fun removeHandlerFromAllBattles(handler: niko_MPC_satelliteHandler) {
        for (battle in battles.keys) {
            removeHandlerFromBattle(battle, handler)
        }
    }
}