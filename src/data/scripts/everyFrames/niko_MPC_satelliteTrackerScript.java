package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

import static data.utilities.niko_MPC_generalUtils.deleteMemoryKey;
import static data.utilities.niko_MPC_planetUtils.getMarketsWithSatellites;
import static data.utilities.niko_MPC_planetUtils.getSatellitesInOrbitOfMarket;
import static data.utilities.niko_MPC_satelliteUtils.*;
import static data.utilities.niko_MPC_scriptUtils.addNewSatelliteTracker;
import static data.utilities.niko_MPC_scriptUtils.satelliteTrackerId;

public class niko_MPC_satelliteTrackerScript implements EveryFrameScript {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteTrackerScript.class);

    static {
        log.setLevel(Level.ALL);
    }

    public MarketAPI market;
    public SectorEntityToken entity;
    public List<CustomCampaignEntityAPI> satellites;
    public boolean done = false;

    private boolean marketNeedsSatellitesAdded;
    private boolean marketHasFuckedUpAndWasLogged = false;

    public int maxPhysicalSatellites; //named physical as i may have a few fucking thousand in description or smthn
    // Variables below are used in instantiating a new satellite instance.
    public String satelliteId = "niko_MPC_derelict_anti_asteroid_satellite";
    public String satelliteFactionId = "derelict";

    /**
     * Instantiates a new satellite tracker.
     *
     * @param market                The market this satellite tracker will track, used for conditions and determining when to remove the satellites.
     * @param satellites            A list containing CustomCampaignEntityAPI instances. Iterated through each advance(), each instance being checked and updated.
     * @param maxPhysicalSatellites
     * @param satelliteId
     * @param satelliteFactionId
     */

    public niko_MPC_satelliteTrackerScript(MarketAPI market,  List<CustomCampaignEntityAPI> satellites,
                                           int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        this(market, market.getPrimaryEntity(), satellites, maxPhysicalSatellites, satelliteId, satelliteFactionId);
    }

    public niko_MPC_satelliteTrackerScript(MarketAPI market, SectorEntityToken entity,
                                           int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        this(market, entity, new ArrayList<CustomCampaignEntityAPI>(), maxPhysicalSatellites, satelliteId, satelliteFactionId);
    }

    public niko_MPC_satelliteTrackerScript(MarketAPI market,
                                           int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        this(market, market.getPrimaryEntity(), new ArrayList<CustomCampaignEntityAPI>(), maxPhysicalSatellites, satelliteId, satelliteFactionId);
    }

    public niko_MPC_satelliteTrackerScript(MarketAPI market, SectorEntityToken entity, List<CustomCampaignEntityAPI> satellites,
                                           int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        this.market = market;
        this.entity = entity;
        this.satellites = satellites;
        this.maxPhysicalSatellites = maxPhysicalSatellites;
        this.satelliteId = satelliteId;
        this.satelliteFactionId = satelliteFactionId;

        marketNeedsSatellitesAdded = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {

        //log.debug(market.getId() + " iterated");

        migrateMarketsIfEntityHasNewMarket();

        if (deleteSatellitesAndSelfIfMarketIsNull()) {
            return;
        }
        if (deleteSatellitesAndSelfIfMarketHasNoCondition()) {
            return;
        }
        updateSatelliteStatus();
    }

    public void migrateMarketsIfEntityHasNewMarket() {
        MarketAPI entityMarket = getEntity().getMarket();

        if (!(entityMarket.getId().equals(market.getId()))) {
            migrateSatellitesToNewMarket(market, entityMarket, this);
        }
    }

    public static void migrateSatellitesToNewMarket(MarketAPI oldMarket, MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        setMarket(market, script);
        deleteMemoryKey(oldMarket.getMemoryWithoutUpdate(), satelliteTrackerId);
        addNewSatelliteTracker(market, script);
    }

    public void updateSatelliteStatus() throws RuntimeException{
        if (marketNeedsSatellitesAdded) {
            addSatellitesToMarket(market, maxPhysicalSatellites, satelliteId, satelliteFactionId); //todo: document
            marketNeedsSatellitesAdded = false;
        }

        Iterator<CustomCampaignEntityAPI> iterator = getSatelliteTrackerTrackedSatellites(this).iterator();

        while(iterator.hasNext()) {
            CustomCampaignEntityAPI satellite = iterator.next();
            if (Float.valueOf(satellite.getFacing()).isNaN()) { //fixme: debug code
                if (!(marketHasFuckedUpAndWasLogged)) {
                    PlanetAPI planet = (PlanetAPI) market.getPrimaryEntity();
                    log.debug("niko_MPC_ERROR: " + market.getName() + ", type " + planet.getTypeId() + ", had a satellite with NaN facing in " + market.getStarSystem().getName());
                    marketHasFuckedUpAndWasLogged = true;
                }
                removeSatellite(satellite);
                regenerateOrbitSpacing(market);
                iterator.remove();
            }
        }
    }

    public boolean deleteSatellitesAndSelfIfMarketIsNull() {
        if (getMarket() == null) {
            deleteSatellitesAndSelf();
            return true;
        }
        return false;
    }

    public boolean deleteSatellitesAndSelfIfMarketHasNoCondition() {
        boolean hasCondition = false;

        for (String id : satelliteConditionIds) {
            if (getMarket().hasCondition(id)) {
                hasCondition = true;
            }
        }
        if (!(hasCondition)) {
            deleteSatellitesAndSelf();
            return true;
        }
        return false;
    }


    public void deleteSatellitesAndSelf() {
        Iterator<CustomCampaignEntityAPI> iterator = satellites.iterator();

        while(iterator.hasNext()) { //iterator to avoid a concurrentmodificationexception
            CustomCampaignEntityAPI satellite = iterator.next();
            removeSatellite(satellite);
            iterator.remove();//todo: methodize. this one might need a refactor
        }
        done = true;

        deleteMemoryKey(market.getMemoryWithoutUpdate(), satelliteTrackerId);

        getEntity().removeScript(this); // we aren't needed anymore
    }

    @Getter
    public static List<CustomCampaignEntityAPI> getSatelliteTrackerTrackedSatellites(niko_MPC_satelliteTrackerScript script) {
        return script.satellites;
    }

    @Getter
    public MarketAPI getMarket() {
        return market;
    }

    @Setter
    public static void setMarket(MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        script.market = market;
    }

    @Getter
    public SectorEntityToken getEntity() {
        return entity;
    }

}
