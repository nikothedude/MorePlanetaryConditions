package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.campaign.fleet.Battle;
import data.scripts.campaign.plugins.autoresolve.niko_MPC_satelliteAutoresolveBasePlugin;
import data.scripts.campaign.plugins.autoresolve.niko_MPC_satelliteAutoresolvePlugin;
import data.scripts.campaign.plugins.battleCreation.niko_MPC_satelliteSpawnerBattlePlugin;
import data.scripts.campaign.plugins.battleCreation.niko_MPC_satelliteSpawnerBattlePluginSecrets;

import java.util.*;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;
import static data.utilities.niko_MPC_satelliteUtils.*;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin { //todo: add to modplugin

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        if (opponent instanceof CampaignFleetAPI) {
            CampaignFleetAPI opposingFleet = (CampaignFleetAPI) opponent;
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            BattleAPI battle = playerFleet.getBattle();

            HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToFight = getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(battle);
            if (entitiesWillingToFight.size() > 0) {
                List<SectorEntityToken> entitiesOnPlayerSide = getEntitiesOnSide(battle, battle.pickSide(playerFleet), entitiesWillingToFight);
                List<SectorEntityToken> entitiesOnOtherSide = getEntitiesOnSide(battle, battle.pickSide(opposingFleet), entitiesWillingToFight);

                int totalEntitiesHostile = (entitiesOnPlayerSide.size() + entitiesOnOtherSide.size());

                if (totalEntitiesHostile > 0) {
                    if (Global.getSettings().getModManager().isModEnabled("secretsofthefrontieralt")) {
                        return new PluginPick<BattleCreationPlugin>(new niko_MPC_satelliteSpawnerBattlePluginSecrets(entitiesOnPlayerSide, entitiesOnOtherSide), PickPriority.MOD_SET);
                        //todo: implement
                    } else {
                        return new PluginPick<BattleCreationPlugin>(new niko_MPC_satelliteSpawnerBattlePlugin(entitiesOnPlayerSide, entitiesOnOtherSide), PickPriority.MOD_SET);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) {
        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToFight = getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(battle);
        if (entitiesWillingToFight.size() > 0) {
            List<SectorEntityToken> entitiesOnSideOne = getEntitiesOnSide(battle, BattleAPI.BattleSide.ONE, entitiesWillingToFight);
            List<SectorEntityToken> entitiesOnSideTwo = getEntitiesOnSide(battle, BattleAPI.BattleSide.TWO, entitiesWillingToFight);

            return new PluginPick<BattleAutoresolverPlugin>(new niko_MPC_satelliteAutoresolvePlugin(battle, entitiesOnSideOne, entitiesOnSideTwo), PickPriority.MOD_SET);
        }
    return null;
    }
}
