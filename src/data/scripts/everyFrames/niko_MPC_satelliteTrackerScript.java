package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

import static data.utilities.niko_MPC_generalUtils.deleteMemoryKey;
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

    public float maximumSatelliteStrength = 500f; //todo: arbitrary
    public final float minimumSatelliteStrength = 0f;
    public float currentOverallSatelliteStrength;
    public float satelliteNaturalRegenRate = 1f;

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

        currentOverallSatelliteStrength = maximumSatelliteStrength;

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
        increaseOverallSatelliteStrengthIfNotAtMax(satelliteNaturalRegenRate);
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

    public void increaseOverallSatelliteStrengthIfNotAtMax(float increment) {
        setCurrentOverallSatelliteStrength(Math.min(getCurrentSatelliteArrayStrength() + increment, maximumSatelliteStrength));
    }

    public boolean satellitesWantToBlockFleet(CampaignFleetAPI fleet) {

        if (market.isPlanetConditionMarketOnly()) { //uncolonized planets are always hostile
            return true;
        }

        FactionAPI fleetFaction = fleet.getFaction();
        FactionAPI satelliteFaction = Global.getSector().getFaction(getSatelliteFactionId());
        return satelliteFaction.isHostileTo(fleetFaction) || (!(fleet.isTransponderOn()));
    }

    public boolean satellitesCapableOfBlockingFleet(CampaignFleetAPI fleet) {
        return (getCurrentSatelliteArrayStrength() >= fleet.getEffectiveStrength()); //todo: finish
    }

    @Getter
    public float getCurrentSatelliteArrayStrength() {
        return currentOverallSatelliteStrength;
    }

    @Setter
    public void setCurrentOverallSatelliteStrength(float increment) {
        currentOverallSatelliteStrength = increment; //todo: does the getter pass by value, or ref? im doing this because i assume it passes by value
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

    @Getter
    public String getSatelliteFactionId() {
        return satelliteFactionId;
    }

}
