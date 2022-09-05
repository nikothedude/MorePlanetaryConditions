package data.scripts.campaign.plugins.autoresolve;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;

import java.util.List;

public abstract class niko_MPC_satelliteAutoresolveBasePlugin extends BattleAutoresolverPluginImpl {

    private List<SectorEntityToken> entitiesSupportingSideOne;
    private List<SectorEntityToken> entitiesSupportingSideTwo;

    public niko_MPC_satelliteAutoresolveBasePlugin(BattleAPI battle, List<SectorEntityToken> entitiesSupportingSideOne, List<SectorEntityToken> entitiesSupportingSideTwo) {
        super(battle);

        this.entitiesSupportingSideOne = entitiesSupportingSideOne;
        this.entitiesSupportingSideTwo = entitiesSupportingSideTwo;
    }

    @Override
    protected FleetAutoresolveData computeDataForFleet(CampaignFleetAPI fleet) {
        FleetAutoresolveData fleetData = super.computeDataForFleet(fleet);
        fleetData.fightingStrength

        return fleetData;
    }
}
