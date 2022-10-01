package data.utilities;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.exceptions.niko_MPC_stackTraceGenerator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.fs.starfarer.api.GameState.TITLE;
import static data.utilities.niko_MPC_ids.satelliteHandlerId;

public class niko_MPC_debugUtils {
    private static final Logger log = Global.getLogger(niko_MPC_debugUtils.class);

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
    public static void displayError(@NotNull String errorCode, boolean highPriority, boolean crash) throws RuntimeException {
        if (niko_MPC_settings.SHOW_ERRORS_IN_GAME) {
            GameState state = Global.getCurrentState();
            if (state == GameState.CAMPAIGN) {
                displayErrorToCampaign(errorCode, highPriority);
            } else if (state == GameState.COMBAT) {
                displayErrorToCombat(errorCode, highPriority);
            } else if (state == TITLE) {
                displayErrorToTitle(errorCode, highPriority);
            }
        }

        log.error("Error code:", new niko_MPC_stackTraceGenerator(errorCode));

        if (crash) {
            throw new RuntimeException("A critical error has occurred in More Planetary Conditions, and for one reason" +
                    " or another, the mod author has decided that this error is severe enough to warrant a crash." +
                    " Error code: " + errorCode);
        }
    }

    private static void displayErrorToCampaign(String errorCode, boolean highPriority) {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        campaignUI.addMessage("###### MORE PLANETARY CONDITIONS ERROR ######");
        campaignUI.addMessage(errorCode);
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f);
            campaignUI.addMessage("The above error has been deemed high priority by the mod author. It is likely" +
                            " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                            + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                    Color.RED);
        }
        campaignUI.addMessage("Please provide the mod author a copy of your logs. These messages can be disabled in settings.");
    }

    private static void displayErrorToCombat(String errorCode, boolean highPriority) {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatUIAPI combatUI = engine.getCombatUI();

        combatUI.addMessage(1, "Please provide the mod author with a copy of your logs.  These messages can be disabled in settings.");
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f);
            combatUI.addMessage(1, "The above error has been deemed high priority by the mod author. It is likely" +
                            " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                            + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                    Color.RED);
        }
        combatUI.addMessage(1, errorCode);
        combatUI.addMessage(1, "###### MORE PLANETARY CONDITIONS ERROR ######");
    }
    
    private static void displayErrorToTitle(String errorCode, boolean highPriority) {
        return;
    }

    public static void logEntityData(SectorEntityToken entity) {
        if (entity != null) {
            MarketAPI market = entity.getMarket();
            String marketName = null;
            String marketId = null;
            if (market != null) {
                marketName = market.getName();
                marketId = market.getId();
            }
            log.debug("Now logging debug info of: " + entity.getName() + ". " +
                    "Entity market: " + marketName + ", " + marketId + ". " +
                    "Entity location: " + entity.getContainingLocation().getName() + ", is star system: " + (entity.getContainingLocation() instanceof StarSystemAPI) + ". ");
        }
        else {
            log.debug("Cannot log debug info of entity-it is null.");
        }
    }

    /**
     * Returns false if the entity has satellite params, a tracker, or if the entity has a satellite market.
     */
    public static boolean doEntityHasNoSatellitesTest(SectorEntityToken entity) {
        boolean result = true;
        if (niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because " + niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity).getName() + " was still applied");
            if (Global.getSettings().isDevMode()) {
                displayError("doEntityHasNoSatellitesTest getEntitySatelliteMarket failure on " + entity);
            }
            result = false;
        }
        if (niko_MPC_satelliteUtils.defenseSatellitesApplied(entity) || entity.getMemoryWithoutUpdate().get(satelliteHandlerId) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because defenseSatellitesApplied returned true");
            if (Global.getSettings().isDevMode()) {
                displayError("doEntityHasNoSatellitesTest defenseSatellitesApplied failure on " + entity);
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
    public static boolean assertEntityHasSatellites(SectorEntityToken entity) {
        boolean result = true;
        if (entity == null) return false;

        niko_MPC_satelliteHandler params = niko_MPC_satelliteUtils.getEntitySatelliteHandler(entity);
        if (params == null) {
            displayError("assertEntityHasSatellitesFailure on " + entity + ", entity name: " + entity.getName());
            logEntityData(entity);
            result = false;
        }
    return result;
    }
}
