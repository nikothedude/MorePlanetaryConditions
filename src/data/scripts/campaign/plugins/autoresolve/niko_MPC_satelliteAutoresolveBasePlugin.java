/*package data.scripts.campaign.plugins.autoresolve;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteParams;

public abstract class niko_MPC_satelliteAutoresolveBasePlugin extends BattleAutoresolverPluginImpl {

    private List<SectorEntityToken> entitiesSupportingSideOne;
    private List<SectorEntityToken> entitiesSupportingSideTwo;

    private boolean modifiedFleetDataForSideOne = false;
    private boolean modifiedFleetDataForSideTwo = false;

    public niko_MPC_satelliteAutoresolveBasePlugin(BattleAPI battle, List<SectorEntityToken> entitiesSupportingSideOne, List<SectorEntityToken> entitiesSupportingSideTwo) {
        super(battle);

        this.entitiesSupportingSideOne = entitiesSupportingSideOne;
        this.entitiesSupportingSideTwo = entitiesSupportingSideTwo;
    }

    @Override
    protected FleetAutoresolveData computeDataForFleet(CampaignFleetAPI fleet) {
        if (!fleetSideHasHadOutcomeModified(fleet)) {
            if (battle.getSnapshotSideOne().contains(fleet)) {
                for (SectorEntityToken entity : entitiesSupportingSideOne) {
                    niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
                    HashMap<String, Float> weightedVariants = params.weightedVariantIds;
                    WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
                    for (Map.Entry<String, Float> entry : weightedVariants.entrySet()) {
                        picker.add(entry.getKey(), entry.getValue()); //add the keys and values to the picker in hashmap format, since
                    } //the picker pick() uses the value as chance, key as value
                    for (int i = 0; i < params.maxBattleSatellites; i++) {
                        fleet.getFleetData().addFleetMember((picker.pick())); //todo: i made this while barely functioning please review
                    }
                }
            }
            FleetAutoresolveData data = super.computeDataForFleet(fleet);
        }




        return data;
    }

    private boolean fleetSideHasHadOutcomeModified(CampaignFleetAPI fleet) {
        boolean result = false;
        if (battle.getSnapshotSideOne().contains(fleet)) {
            if (modifiedFleetDataForSideOne) {
                result = true;
            }
        }
        else {
            if (modifiedFleetDataForSideTwo) {
                result = true;
            }
        }
        return result;
    }
}
*/