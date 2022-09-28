package data.utilities;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Save-specific global list that stores a hashmap of battleAPI->(satellitehandler->battleside). Will eventually replace the
 * list on handler.
 */
public class niko_MPC_satelliteBattleTracker {

    public Map<BattleAPI, Map<niko_MPC_satelliteHandler, BattleAPI.BattleSide>> battles = new HashMap<>();

    /**
     * Associates a hashmap entry of (battle->(handler->side)). The side is stored for future reference.
     * <p>
     * If the hashmap doesnt contain battle, or if the v alue of battle is null, instantiates a new hashmap and assigns (battle->Hashmap()).
     * @param battle The battle to associate with.
     * @param handler The satellite handler we are associating.
     * @param side The stored side of the battle.
     */
    public void associateSatellitesWithBattle(BattleAPI battle, niko_MPC_satelliteHandler handler, BattleAPI.BattleSide side) {
        if (!battles.containsKey(battle) || battles.get(battle) == null) battles.put(battle, new HashMap<niko_MPC_satelliteHandler, BattleAPI.BattleSide>());

        Map<niko_MPC_satelliteHandler, BattleAPI.BattleSide> currentBattles = battles.get(battle);
        currentBattles.put(handler, side);
    }

    public Map<BattleAPI, Map<niko_MPC_satelliteHandler, BattleAPI.BattleSide>> getBattles() {
        return battles;
    }

    public Set<niko_MPC_satelliteHandler> getSatellitesInfluencingBattle(BattleAPI battle) {

        return (battles.get(battle) == null ? new HashSet<niko_MPC_satelliteHandler>() : battles.get(battle).keySet());
    }

    public BattleAPI.BattleSide getSideOfSatellitesForBattle(BattleAPI battle, niko_MPC_satelliteHandler handler) {
        return (battles.get(battle) == null ? null : battles.get(battle).get(handler));
    }

    public boolean areSatellitesInvolvedInBattle(BattleAPI battle, niko_MPC_satelliteHandler handler) {
        if (battles.containsKey(battle)) {
            if (battles.get(battle).containsKey(handler)) {
                return true;
            }
            else {
                /*updateBattleAssociationWithScan(battle);
                return battles.get(battle).containsKey(handler);*/
                return false;
            }
        }
        return false;
    }

    public boolean areAnySatellitesInvolvedInBattle(BattleAPI battle) {
        return (battles.get(battle) != null && !battles.get(battle).isEmpty());
    }

    public HashMap<niko_MPC_satelliteHandler, CampaignFleetAPI> scanBattleForSatellites(BattleAPI battle) {
        HashMap<niko_MPC_satelliteHandler, CampaignFleetAPI> handlerToFleetMap = new HashMap<>();
        for (CampaignFleetAPI fleet : battle.getBothSides()) {
            niko_MPC_satelliteHandler foundhandler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(fleet);
            if (foundhandler != null) {
                handlerToFleetMap.put(foundhandler, fleet);
            }
        }
        return handlerToFleetMap;
    }

    public void updateBattleAssociationWithScan(BattleAPI battle) {
        HashMap<niko_MPC_satelliteHandler, CampaignFleetAPI> handlerToFleetMap = scanBattleForSatellites(battle);

        for (Map.Entry<niko_MPC_satelliteHandler, CampaignFleetAPI> entry : handlerToFleetMap.entrySet()) {
            niko_MPC_satelliteHandler handler = entry.getKey();
            CampaignFleetAPI fleet = entry.getValue();

            if (!areSatellitesInvolvedInBattle(battle, handler)) {
                HashMap<niko_MPC_satelliteHandler, BattleAPI.BattleSide> handlerToSideMap = new HashMap<>();
                handlerToSideMap.put(handler, battle.pickSide(fleet));
                battles.put(battle, handlerToSideMap);
            }
        }
    }

    public void removeBattle(BattleAPI battle) {
        battles.remove(battle);
    }

    public void removeHandlerFromBattle(BattleAPI battle, niko_MPC_satelliteHandler handler) {
        if (battles.get(battle) != null) {
            battles.get(battle).remove(handler);
        }
    }

    public void removeHandlerFromAllBattles(niko_MPC_satelliteHandler handler) {
        for (BattleAPI battle : battles.keySet()) {
            removeHandlerFromBattle(battle, handler);
        }
    }

}
