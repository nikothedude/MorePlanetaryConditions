package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;

public class niko_NPC_debugUtils {
    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void displayErrorToCampaign(String errorCode) {
        Global.getSector().getCampaignUI().addMessage("More planetary conditions error: " + errorCode);
        Global.getSector().getCampaignUI().addMessage("Please provide the mod author a copy of your logs.");
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
}
