package data;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.HashSet;
import java.util.Set;

import static data.utilities.niko_MPC_planetUtils.getMarketsWithSatellites;
import static data.utilities.niko_MPC_satelliteUtils.getInstanceOfSatelliteTracker;
import static data.utilities.niko_MPC_scriptUtils.*;

public class niko_MPC_modPlugin extends BaseModPlugin {

    @Override
    public void onNewGame() {
        super.onNewGame();

        // The code below requires that Nexerelin is added as a library (not a dependency, it's only needed to compile the mod).
//        boolean isNexerelinEnabled = Global.getSettings().getModManager().isModEnabled("nexerelin");

//        if (!isNexerelinEnabled || SectorManager.getManager().isCorvusMode()) {
//                    new MySectorGen().generate(Global.getSector());
            // Add code that creates a new star system (will only run if Nexerelin's Random (corvus) mode is disabled).
//        }
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        addSatelliteTrackerIfNoneIsPresent();
 //       updateSatelliteTrackerMarkets();
    }
    @Override
    public void onGameLoad(boolean newGame) {
        addSatelliteTrackerIfNoneIsPresent();
 //       updateSatelliteTrackerMarkets();
    }
}

