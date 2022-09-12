package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.scripts.everyFrames.niko_MPC_fleetReachedEntityChecker;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteUtils;

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

        for (CampaignFleetAPI fleet : battle.getBothSides()) {
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) {
                if (!niko_MPC_debugUtils.ensureEntityHasSatellites(fleet)) return;
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(fleet);
                params.getInfluencedBattles().remove(battle);

                niko_MPC_fleetUtils.safeDespawnFleet(fleet);
            }
        }
    }
}
