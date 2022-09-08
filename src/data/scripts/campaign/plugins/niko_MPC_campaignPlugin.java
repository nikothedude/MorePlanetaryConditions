package data.scripts.campaign.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.BattleAutoresolverPlugin;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin { //todo: add to modplugin

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    @Override
    public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) {
        /*HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToFight = getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(battle);
        if (entitiesWillingToFight.size() > 0) {
            List<SectorEntityToken> entitiesOnSideOne = getEntitiesOnSide(battle, BattleAPI.BattleSide.ONE, entitiesWillingToFight);
            List<SectorEntityToken> entitiesOnSideTwo = getEntitiesOnSide(battle, BattleAPI.BattleSide.TWO, entitiesWillingToFight);

            return new PluginPick<BattleAutoresolverPlugin>(new niko_MPC_satelliteAutoresolvePlugin(battle, entitiesOnSideOne, entitiesOnSideTwo), PickPriority.MOD_SET);
        } */
    return null; //todo: hatred.
    }
}
