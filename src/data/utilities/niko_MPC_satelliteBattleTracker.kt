package data.utilities

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import java.lang.ref.WeakReference

/**
 * Save-specific global list that stores a hashmap of battleAPI->(satellitehandler->battleside).
 */
class niko_MPC_satelliteBattleTracker {
    var battles: MutableMap<BattleAPI, MutableMap<niko_MPC_satelliteHandlerCore, BattleSide>?> = HashMap()

    /**
     * Associates a hashmap entry of (battle->(handler->side)). The side is stored for future reference.
     *
     *
     * If the hashmap doesnt contain battle, or if the value of battle is null, instantiates a new hashmap and assigns (battle->Hashmap()).
     * @param battle The battle to associate with.
     * @param handler The satellite handler we are associating.
     * @param side The stored side of the battle.
     */
    fun associateSatellitesWithBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandlerCore, side: BattleSide) {
        if (!battles.containsKey(battle) || battles[battle] == null) battles[battle] = HashMap()
        val currentBattles = battles[battle]
        currentBattles!![handler] = side
        niko_MPC_debugUtils.log.info("satellite battle tracker associated $handler with $battle, $side")
    }

    fun getSatellitesInfluencingBattle(battle: BattleAPI): Set<niko_MPC_satelliteHandlerCore> {
        return if (battles[battle] == null) HashSet() else battles[battle]!!.keys
    }

    fun getSideOfSatellitesForBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandlerCore): BattleSide? {
        return if (battles[battle] == null) null else battles[battle]!![handler]
    }

    fun areSatellitesInvolvedInBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandlerCore): Boolean {
        return if (battles.containsKey(battle)) {
            battles[battle]!!.containsKey(handler)
        } else false
    }

    fun areAnySatellitesInvolvedInBattle(battle: BattleAPI): Boolean {
        return battles[battle] != null && battles[battle]!!.isNotEmpty()
    }

    fun scanBattleForSatellites(battle: BattleAPI): HashMap<niko_MPC_satelliteHandlerCore, CampaignFleetAPI> {
        val handlerToFleetMap = HashMap<niko_MPC_satelliteHandlerCore, CampaignFleetAPI>()
        for (fleet in battle.bothSides) {
            val foundhandler = fleet.getSatelliteEntityHandler()
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
                val handlerToSideMap = HashMap<niko_MPC_satelliteHandlerCore, BattleSide>()
                handlerToSideMap[handler] = battle.pickSide(fleet)
                battles[battle] = handlerToSideMap
            }
        }
    }

    fun removeBattle(battle: BattleAPI) {
        battles.remove(battle)
    }

    fun removeHandlerFromBattle(battle: BattleAPI, handler: niko_MPC_satelliteHandlerCore) {
        if (battles[battle] != null) {
            battles[battle]!!.remove(handler)
        }
        niko_MPC_debugUtils.log.info("tracker removed $handler from $battle")
    }

    fun removeHandlerFromAllBattles(handler: niko_MPC_satelliteHandlerCore) {
        for (battle in battles.keys) {
            removeHandlerFromBattle(battle, handler)
        }
    }
}