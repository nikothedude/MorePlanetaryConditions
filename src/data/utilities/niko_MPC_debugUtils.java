package data.utilities;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.Arrays;

import static data.utilities.niko_MPC_ids.satelliteParamsId;

public class niko_MPC_debugUtils {
    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void displayError(String errorCode) {
        displayError(errorCode, false, false);
    }

    public static void displayError(String errorCode, boolean highPriority) throws RuntimeException {
        displayError(errorCode, highPriority, false);
    }

    /**
     * Displays the given errorCode in whatever game layer the game is currently in. Also logs it, as well as prints
     * the stack trace in log.
     * @param errorCode The errorCode to display and log.
     * @param highPriority If true, a very loud and visible message will be displayed to the user.
     * @param crash If true, crashes the game.
     * @throws RuntimeException
     */
    public static void displayError(String errorCode, boolean highPriority, boolean crash) throws RuntimeException {
        GameState state = Global.getCurrentState();
        if (state == GameState.CAMPAIGN) {
            displayErrorToCampaign(errorCode, highPriority);
        }
        else if (state == GameState.COMBAT) {
            displayErrorToCombat(errorCode, highPriority);
        }
        log.error(errorCode);

        if (crash) {
            throw new RuntimeException("A critical error has occurred in More Planetary Conditions, and for one reason" +
                    " or another, the mod author has decided that this error is severe enough to warrant a crash." +
                    " Error code: " + errorCode);
        }
        else {
            RuntimeException exception = new RuntimeException("StackTraceGenerator");
            log.debug(Arrays.toString(exception.getStackTrace())); //for debugging
        }
    }

    private static void displayErrorToCampaign(String errorCode, boolean highPriority) {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        campaignUI.addMessage(errorCode);
        campaignUI.addMessage("Please provide the mod author a copy of your logs.");
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f);
            campaignUI.addMessage("The above error has been deemed high priority by the mod author. It is likely" +
                            " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                            + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                    Color.RED);
        }
        campaignUI.addMessage("###### MORE PLANETARY CONDITIONS ERROR ######");

    }

    private static void displayErrorToCombat(String errorCode, boolean highPriority) {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatUIAPI combatUI = engine.getCombatUI();

        combatUI.addMessage(1, "Please provide the mod author with a copy of your logs.");
        combatUI.addMessage(1, errorCode);
        combatUI.addMessage(1, "###### MORE PLANETARY CONDITIONS ERROR ######");

        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f);
            combatUI.addMessage(1, "The above error has been deemed high priority by the mod author. It is likely" +
                            " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                            + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                    Color.RED);
        }
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
                displayError("doEntityHasNoSatellitesTest getEntitySatelliteMarket failure");
            }
            result = false;
        }
        if (niko_MPC_satelliteUtils.defenseSatellitesApplied(entity) || entity.getMemoryWithoutUpdate().get(satelliteParamsId) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because defenseSatellitesApplied returned true");
            if (Global.getSettings().isDevMode()) {
                displayError("doEntityHasNoSatellitesTest defenseSatellitesApplied failure");
                result = false;
            }

        }
        if (!result) {
            logEntityData(entity);
        }
        return result;
    }

    /**
     * Checks getEntitySatelliteParams(entity) and looks to see if it isnt null.
     * @param entity The entity to check.
     * @return False if entity is null or the params are null. If params are null, an error will be displayed.
     */
    public static boolean ensureEntityHasSatellites(SectorEntityToken entity) {
        boolean result = true;
        if (entity == null) return false;

        niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
        if (params == null) {
            displayError("ensureEntityHasSatellitesFailure");
            logEntityData(entity);
            result = false;
        }
    return result;
    }
}
