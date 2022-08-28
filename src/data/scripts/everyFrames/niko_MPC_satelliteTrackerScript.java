package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
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

    /**
     * Every single market that supposedly has satellites in orbit. Is iterated through every advance() call.
     */
    public MarketAPI market;
    public SectorEntityToken entity;
    public List<CustomCampaignEntityAPI> satellites;
    public boolean done = false;

    //public niko_MPC_satelliteTrackerScript(MarketAPI market) {
     //   this(market, new ArrayList<CustomCampaignEntityAPI>());
   // }

    /**
     * Instantiates a new satellite tracker.
     *
     * @param market     The market this satellite tracker will track, used for conditions and determining when to remove the satellites.
     * @param satellites A list containing CustomCampaignEntityAPI instances. Iterated through each advance(), each instance being checked and updated.
     */
    public niko_MPC_satelliteTrackerScript(MarketAPI market, List<CustomCampaignEntityAPI> satellites) {
        this(market, market.getPrimaryEntity(), satellites);
    }

    /**
     * Instantiates a new satellite tracker.
     * @param market The market this satellite tracker will track, used for conditions and determining when to remove the satellites.
     * @param satellites A list containing CustomCampaignEntityAPI instances. Iterated through each advance(), each instance being checked and updated.
     */
    public niko_MPC_satelliteTrackerScript(MarketAPI market, SectorEntityToken entity, List<CustomCampaignEntityAPI> satellites) {
        this.market = market;
        this.entity = entity;
        this.satellites = satellites;
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
        market.getMemoryWithoutUpdate().set(satellitesInOrbitMemKeyId, script.satellites);
        addNewSatelliteTracker(market, script);

        MemoryAPI oldMarketMemory = oldMarket.getMemoryWithoutUpdate();
        deleteMemoryKey(oldMarketMemory, satellitesInOrbitMemKeyId);
    }

    public void updateSatelliteStatus() {
        Iterator<CustomCampaignEntityAPI> iterator = getSatelliteTrackerTrackedSatellites(this).iterator();

        while(iterator.hasNext()) {
            CustomCampaignEntityAPI satellite = iterator.next();

            //do stuff
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

        MemoryAPI marketMemory = getMarket().getMemoryWithoutUpdate();
        deleteMemoryKey(marketMemory, satellitesInOrbitMemKeyId);
        deleteMemoryKey(marketMemory, satelliteTrackerId);

        done = true;
        getEntity().removeScript(this); // we aren't needed anymore
    }

    /**
     * Only used with lists, no iterator support.
     * @param satellite
     * @param script
     */
    public static void deleteSatelliteAndRemoveFromList(CustomCampaignEntityAPI satellite, niko_MPC_satelliteTrackerScript script) {
        removeSatellite(satellite);
        getSatelliteTrackerTrackedSatellites(script).remove(satellite);
    }

    public void removeSatellitesFromMarketAndRemoveFromList(MarketAPI market) {
        removeSatellitesFromMarket(market, getSatellitesInOrbitOfMarket(market));
        getMarketsWithSatellites().remove(market);
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
