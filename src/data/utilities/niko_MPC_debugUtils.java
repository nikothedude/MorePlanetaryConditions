package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.ui.P;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static data.utilities.niko_MPC_ids.satelliteParamsId;

public class niko_MPC_debugUtils {
    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void displayErrorToCampaign(String errorCode) throws RuntimeException{
        Global.getSector().getCampaignUI().addMessage("More planetary conditions error: " + errorCode);
        log.error(errorCode, new Exception("StackTraceGenerator"));
        Global.getSector().getCampaignUI().addMessage("Please provide the mod author a copy of your logs.");
    }

    public static RuntimeException generateException(String debug) {
        throw new RuntimeException(debug);
    }

    public static void logEntityData(SectorEntityToken entity) {
        MarketAPI market = entity.getMarket();
        String marketName = null;
        String marketId = null;
        if (market != null) {
            marketName = market.getName();
            marketId = market.getId();
        }

        log.debug("Now logging debug info of: " + entity.getName() + ". " +
                "Entity market: " + entity.getMarket() + ", " + marketName + ", " + marketId + ". " +
                "Entity location: " + entity.getContainingLocation().getName() + ", is star system: " + (entity.getContainingLocation() instanceof StarSystemAPI) + ". ");
    }

    /**
     * Returns false if the entity has satellite params, a tracker, or if the entity has a satellite market.
     */
    public static boolean doEntityHasNoSatellitesTest(SectorEntityToken entity) {
        boolean result = true;
        if (niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because " + niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity).getName() + " was still applied");
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("doEntityHasNoSatellitesTest getEntitySatelliteMarket failure");
            }
            result = false;
        }
        if (niko_MPC_satelliteUtils.defenseSatellitesApplied(entity) || entity.getMemoryWithoutUpdate().get(satelliteParamsId) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because defenseSatellitesApplied returned true");
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("doEntityHasNoSatellitesTest defenseSatellitesApplied failure");
                result = false;
            }

        }
        if (!result) {
            logEntityData(entity);
        }
        return result;
    }

    public static boolean ensureEntityHasSatellites(SectorEntityToken entity) {
        boolean result = true;

        niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
        if (params == null || niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity) == null) {
            displayErrorToCampaign("ensureEntityHasSatellitesFailure");
            logEntityData(entity);
            result = false;
        }
    return result;
    }
}
