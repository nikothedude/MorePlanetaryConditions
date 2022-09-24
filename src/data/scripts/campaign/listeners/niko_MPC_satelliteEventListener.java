package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteUtils;
import sound.F;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class niko_MPC_satelliteEventListener extends BaseCampaignEventListener {
    public niko_MPC_satelliteEventListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        super.reportBattleOccurred(primaryWinner, battle);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        super.reportBattleFinished(primaryWinner, battle);
        Set<niko_MPC_satelliteParams> paramsFound = new HashSet<>();

        for (CampaignFleetAPI fleet : battle.getBothSides()) {
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) {
                if (!niko_MPC_debugUtils.ensureEntityHasSatellites(fleet)) return;
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(fleet);
                paramsFound.add(params);

                params.getInfluencedBattles().remove(battle);
                niko_MPC_fleetUtils.safeDespawnFleet(fleet);
            }
        }

        for (niko_MPC_satelliteParams params : paramsFound) {
            if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(params, primaryWinner)) { //anyone that joins this fleet's side is probably hostile
                for (CampaignFleetAPI hostileFleet : battle.getSideFor(primaryWinner)) {
                    float graceIncrement = 10f;
                    params.adjustGracePeriod(hostileFleet, graceIncrement);
                }
            }
        }

    }
}
