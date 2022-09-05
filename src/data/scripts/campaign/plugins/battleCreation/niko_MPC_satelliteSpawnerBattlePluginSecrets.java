package data.scripts.campaign.plugins.battleCreation;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.plugins.battleCreation.niko_MPC_baseSatelliteSpawnerBattlePlugin;

import java.util.List;

public class niko_MPC_satelliteSpawnerBattlePluginSecrets extends niko_MPC_baseSatelliteSpawnerBattlePlugin {
    public niko_MPC_satelliteSpawnerBattlePluginSecrets(List<SectorEntityToken> entitiesHostileToPlayer, List<SectorEntityToken> entitiesHostileToOpponent) {
        super(entitiesHostileToPlayer, entitiesHostileToOpponent);
    }
}
