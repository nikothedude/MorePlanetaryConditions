package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.bounty.MagicBountyCampaignPlugin;
import data.scripts.campaign.listeners.niko_MPC_satelliteEventListener;
import data.scripts.campaign.plugins.niko_MPC_campaignPlugin;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_memoryUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.ArrayList;
import java.util.List;

public class niko_MPC_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws RuntimeException {
        boolean isMagicLibEnabled = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!isMagicLibEnabled) {
            throw(new RuntimeException("MagicLib is required for more planetary conditions!"));
        }
        boolean isLazyLibEnabled = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!isLazyLibEnabled) {
            throw(new RuntimeException("LazyLib is required for more planetary conditions!"));
        }
    }


    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        Global.getSector().addTransientListener(new niko_MPC_satelliteEventListener(false));

        Global.getSector().registerPlugin(new niko_MPC_campaignPlugin());

        MemoryAPI globalMemory = Global.getSector().getMemory();

        if (!globalMemory.contains(niko_MPC_ids.satelliteBattleTrackerId)) {
            niko_MPC_memoryUtils.createNewSatelliteTracker();
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        super.onNewGameAfterEconomyLoad();

        List<SectorEntityToken> entitiesWithSatellites = niko_MPC_satelliteUtils.getAllDefenseSatellitePlanets();

        for (SectorEntityToken entity : entitiesWithSatellites) {
            LocationAPI containingLocation = entity.getContainingLocation();
            if (containingLocation.getTags().contains(Tags.THEME_CORE)) { //we dont want to spawn in core worlds
                MarketAPI market = entity.getMarket();
                if (market != null) {
                    List<MarketConditionAPI> conditionsCopy = new ArrayList<>(market.getConditions());
                    for (MarketConditionAPI condition : conditionsCopy) {
                        if (niko_MPC_ids.satelliteConditionIds.contains(condition.getId())) {
                            market.removeCondition(condition.getId());
                        }
                    }
                }
                else {
                    niko_MPC_satelliteUtils.purgeSatellitesFromEntity(entity);
                }
            }
        }
    }
}

