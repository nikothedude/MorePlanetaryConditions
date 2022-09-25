package data.scripts.campaign.AI;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.campaign.ai.CampaignFleetAI;
import com.fs.starfarer.campaign.ai.ModularFleetAI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

public class niko_MPC_satelliteFleetAI extends ModularFleetAI {

    public niko_MPC_satelliteFleetAI(CampaignFleet campaignFleet) {
        super(campaignFleet);
    }

    @Override
    public boolean wantsToJoin(BattleAPI battle, boolean considerPlayTransponderStatus) {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(getFleet())) return true;

        niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(getFleet());
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

        if (tracker.areSatellitesInvolvedInBattle(battle, params)) {
            return false;
        }

        return true;
    }

   @Override
   public EncounterOption pickEncounterOption(FleetEncounterContextPlugin context, CampaignFleetAPI otherFleet, boolean pureCheck) {

        CampaignFleetAPI satelliteFleet = getFleet(); //todo: this fucking sucks
        BattleAPI battle = satelliteFleet.getBattle();
        if (battle != null) {
            float effectiveHostileStrength = 0;
            for (CampaignFleetAPI hostileFleet : battle.getOtherSideFor(satelliteFleet)) {
                effectiveHostileStrength += hostileFleet.getEffectiveStrength();
            }
            if (satelliteFleet.getEffectiveStrength() < (effectiveHostileStrength)) {
                return EncounterOption.HOLD_VS_STRONGER;
            }
            else return EncounterOption.HOLD;
        }
        else { //todo: not sure if this is how it really works?
            if ((satelliteFleet.getEffectiveStrength() < (otherFleet.getEffectiveStrength()))) {
                return EncounterOption.HOLD_VS_STRONGER;
            }
        }
        return EncounterOption.HOLD;
    }

    @Override
    public EncounterOption pickEncounterOption(FleetEncounterContextPlugin context, CampaignFleetAPI otherFleet) {
        return pickEncounterOption(context, otherFleet, false);
    }
}
