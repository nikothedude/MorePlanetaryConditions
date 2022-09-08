package data.scripts.everyFrames.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.campaign.terrain.satelliteBarrage.niko_MPC_defenseSatelliteBarrageTerrainPlugin;
import data.utilities.niko_MPC_satelliteUtils;

// suicidal everyframe that runs once, adds barrage terrain effects, then dies
public class niko_MPC_satelliteBarrageTerrainCombatEffectAdder extends BaseEveryFrameCombatPlugin {

    @Override
    public void init(CombatEngineAPI engine) {
        if (Global.getCurrentState() != GameState.COMBAT) return;
        super.init(engine);

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        for (CampaignTerrainAPI terrain : playerFleet.getContainingLocation().getTerrainCopy()) {
            if (terrain.getPlugin() instanceof niko_MPC_defenseSatelliteBarrageTerrainPlugin) { //todo: not locating the terrain since its looking for the terrain itself not the plugin
                niko_MPC_defenseSatelliteBarrageTerrainPlugin barrageTerrain = (niko_MPC_defenseSatelliteBarrageTerrainPlugin) terrain.getPlugin();

                BattleAPI.BattleSide entitySide = niko_MPC_satelliteUtils.getSideForSatellites(terrain.getOrbit().getFocus(), playerFleet.getBattle());
                if (entitySide != BattleAPI.BattleSide.NO_JOIN) {
                    engine.addPlugin(new niko_MPC_defenseSatelliteBarrageTerrainCombatEffect(barrageTerrain, entitySide));
                }
            }
        }
        engine.removePlugin(this); // the only purpose this script has is to add the effects of the terrain
    }
}
