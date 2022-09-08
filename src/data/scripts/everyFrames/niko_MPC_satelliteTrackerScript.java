package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import data.utilities.niko_MPC_ids;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.campaign.CampaignUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_fleetUtils.*;
import static data.utilities.niko_MPC_ids.satelliteConditionIds;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;
import static data.utilities.niko_MPC_satelliteUtils.*;
import static data.utilities.niko_MPC_scriptUtils.setInstanceOfSatelliteTracker;

@Deprecated
public class niko_MPC_satelliteTrackerScript implements EveryFrameScript {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteTrackerScript.class);

    static {
        log.setLevel(Level.ALL);
    }

    public float satelliteGracePeriod;
    public List<BattleAPI> influencedBattles = new ArrayList<>();

    /**
     * The market this script is managing. Every advance(), iterates through every satellite in orbit and updates their status.
     * IS NOT WHAT THIS SCRIPT IS APPLIED TO, THAT IS ENTITY.
     */
    public MarketAPI market;
    public List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();
    /**
     * The entity this script is applied to. By default, it is the entity that holds market.
     */
    public SectorEntityToken entity;
    /**
     * The name that will be applied to satellite fleets.
     */
    public String satelliteFleetName = "Domain-era Anti-Asteroid Satellites";
    /**
     * todo documentation
     */
    public String satelliteFleetType = "derelict_anti_asteroid_satellites";
    /**
     * todo doc
     */
    public float satelliteOrbitRadius;
    /**
     * todo doc
     */
    public int satelliteFleetStrengthIncrement = 5;
    /**
     * A list of CustomCampaignEntityAPI satellites that this script manages. Every advance(), every item in this list
     * is iterated through and updated.
     */
    public HashMap<String, Float> satelliteVariantIds;
    public List<CustomCampaignEntityAPI> satellites;
    /**
     * Returned in isDone().
     */
    public boolean done = false;

    private boolean marketHasFuckedUpAndWasLogged = false;

    /**
     * The maximum amount of satellites our current entity can support in it's orbit.
     */
    public int maxPhysicalSatellites; //named physical as i may have a few fucking thousand in description or smthn
    // Variables below are used in instantiating a new satellite instance.
    /**
     * The Id all of our satellites will be assigned. All Ids will have the satellite's position in the sattelite list
     * appended to the end.
     */
    public String satelliteId = "niko_MPC_derelict_anti_asteroid_satellite";
    /**
     * The factionId that all satellites will be assigned to.
     */
    public String satelliteFactionId = "derelict";

    /////////////////////////
    //                     //
    //     BASIC STUFF     //
    //                     //
    /////////////////////////

    public niko_MPC_satelliteTrackerScript(MarketAPI market, SectorEntityToken entity, List<CustomCampaignEntityAPI> satellites,
                                       int maxPhysicalSatellites, String satelliteId, String satelliteFactionId,
                                       HashMap<String, Float> variantIds) {
    this.market = market;
    this.entity = entity;
    this.satellites = satellites;
    this.maxPhysicalSatellites = maxPhysicalSatellites;
    this.satelliteId = satelliteId;
    this.satelliteFactionId = satelliteFactionId;
    this.satelliteOrbitRadius = (entity.getRadius()) + 15f;

    this.satelliteVariantIds = variantIds;
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

        if (entityHasNewMarket()) {
            migrateMarkets();
        }

        // decrement
        setSatelliteGracePeriod(Math.max((getSatelliteGracePeriod() - amount), 0));

 //       if (Global.getSector().getPlayerFleet().getContainingLocation() != entity.getContainingLocation()) {
 //           return;
 //       }

        if (deleteSatellitesAndSelfIfMarketIsNull() || deleteSatellitesAndSelfIfMarketHasNoCondition()) {
            return;
        }

        updateSatelliteFactionsIfMarketFactionChanged();
        updateSatelliteStatus(false); //todo: do i need this
        spawnSatellitesToOngoingBattles();

        if (Global.getSettings().isDevMode()) {
            doSatelliteNaNFacingTest();
        }
    }

    /////////////////////////
    //                     //
    //   MARKET MIGRATION  //
    //                     //
    /////////////////////////

    /**
     * @return True if entity.getMarket() does not equal this.getMarket().
     */
    public boolean entityHasNewMarket() {
        return (!(getMarket() == getEntity().getMarket()));
    }

    /**
     * Sets this script's factionId to the current entity's market's factionId and updates satellite factions, before
     * deleting the memory key that references this script from the old market (using the stored market variable value).
     * <p>
     * It then sets this script's market value to the entity's current market, and adds this script as a
     * new satellite tracker to that market.
     */
    public void migrateMarkets() {
        MarketAPI newMarket = getEntity().getMarket(); //get our entity's current market, not our own market, this is important

        updateSelfAndSatelliteFactions(newMarket.getFactionId()); //update our own faction as well as the faction of our satellites

        MarketAPI oldMarket = getMarket(); //important, since if we have a new market, the old market may not exist
        if (oldMarket != null) {
            deleteMemoryKey(getMarket().getMemoryWithoutUpdate(), niko_MPC_ids.satelliteTrackerId); //completely purge our references from the old market
        }
        setMarket(newMarket); //set our market to be the new market

        setInstanceOfSatelliteTracker(getMarket(), this); //finally, add our reference to the market so we can be accessed
        //note: we are NOT ACCESSABLE. PERIOD. without this. this is because there is no method for getting a existing script off an entity, only globally
    }

    ///////////////////////////////
    //                           //
    //SATELLITE & STATUS UPDATING//
    //                           //
    ///////////////////////////////

    /**
     * @return true if market's faction is not the same as our current factionId.
     * Will also run updateSelfAndSatelliteFaction on the same condition.
     */
    public boolean updateSatelliteFactionsIfMarketFactionChanged() {
        String marketFactionId = (getMarket().getFactionId());
        if (!(marketFactionId.equals(getSatelliteFactionId()))) { // is the id of our host market the same as our own?
            if (getMarket().isPlanetConditionMarketOnly() && getSatelliteFactionId().equals("derelict")) {
                return false;
            }
            updateSelfAndSatelliteFactions(marketFactionId); // if not, update it
            return true;
        }
        return false;
    }

    /**
     * Sets this script's factionId to the given param, then sets the faction of all satellites to that id as well.
     * @param factionId The id of the faction to set this script's factionId to.
     */
    public void updateSelfAndSatelliteFactions(String factionId) {
        setSatelliteFactionIdWithDefault(factionId); //set our factionid to either derelict or the factionid
        updateSatelliteFactions(); //update the faction of all our satellites with this new faction
    }

    /**
     * Iterates through every item in this.satellites through getSatellites().
     * It then sets their faction, using this script's current factionId.
     * Finally, it gets the current satelliteFleet, if it isn't null, and sets it's faction the same way.
     */
    public void updateSatelliteFactions() {
        for (CustomCampaignEntityAPI satellite : getSatellites()) {
            satellite.setFaction(getSatelliteFactionId());
        }
        for (CampaignFleetAPI satelliteFleet : getSatelliteFleets()) {
            satelliteFleet.setFaction(getSatelliteFactionId());
        }
    }

    /**
     * Documentation todo
     * @param initialUpdate If true, adds initial satellites and fleets to entity. Named as such because it is only true
     *                      on the very first call of this method.
     */
    public void updateSatelliteStatus(boolean initialUpdate) {
        if (initialUpdate) {
          //  addSatellitesToEntity(market, maxPhysicalSatellites, satelliteId, satelliteFactionId); //todo: document
        }
    }

    private void spawnSatellitesToOngoingBattles() {
        List<CampaignFleetAPI> fleetsInRange = CampaignUtils.getNearbyFleets(getEntity(), getSatelliteSpawnRadius()); //todo: probably need to remove the can see check in this method

        for (CampaignFleetAPI fleet : fleetsInRange) {
            BattleAPI battle = fleet.getBattle();
            if (battle == null || influencedBattles.contains(battle)) continue;

            Vector2f coordinates = battle.computeCenterOfMass();
            //         CampaignFleetAPI satelliteFleet = createNewSatelliteFleet(this, getEntity().getContainingLocation(), coordinates.x, coordinates.y, false);
            //      if ((satelliteFleet.getNumMembersFast() == 0) || !battle.join(satelliteFleet)) {
            //         safeDespawnFleet(satelliteFleet);
            continue;
        }
        //   influencedBattles.add(battle);
    }

    ///////////////////////////////
    //                           //
    //    DELETION AND CLEANUP   //
    //                           //
    ///////////////////////////////

    /**
     * Calls deleteAllSatellitesThenSelf() if getmarket() == null.
     * @return True if getMarket() == null. False otherwise.
     */
    public boolean deleteSatellitesAndSelfIfMarketIsNull () {
        if (getMarket() == null) {
            deleteAllSatellitesThenSelf();
            return true;
        }
        return false;
    }

    /**
     * Calls deleteAllSatellitesThenSelf() if market doesn't have a condition with a id matching a string in satelliteConditionIds.
     * @return True if a market condition has an Id that matches any Id in satelliteConditionIds, false otherwise.
     */
    public boolean deleteSatellitesAndSelfIfMarketHasNoCondition () {
        for (MarketConditionAPI condition : getMarket().getConditions()) {
            if (satelliteConditionIds.contains(condition.getId())) {
                return false; //we found a satellite condition! no need to delete
            }
        }
        deleteAllSatellitesThenSelf(); // the loop didnt return, meaning no condition is present
        return true;
    }
    /**
     * Deletes all satellites, the satellite fleet, and then the script itself.
     */
    public void deleteAllSatellitesThenSelf () {
        deleteSatellites();
        deleteSatelliteFleets();
        deleteSelf();
    }
    /**
     * Deletes the memory key on the script's market before setting done to true and removing this script off entity.
     */
    public void deleteSelf () {
        deleteMemoryKey(getMarket().getMemoryWithoutUpdate(), niko_MPC_ids.satelliteTrackerId);
        done = true;
        getEntity().removeScript(this); // we aren't needed anymore
    }
    /**
     * Iterates through all items in satellite, deleting each one and removing it from the list.
     */
    public void deleteSatellites () {
        Iterator<CustomCampaignEntityAPI> iterator = getSatellites().iterator();

        while (iterator.hasNext()) { //iterator to avoid a concurrentmodificationexception
            CustomCampaignEntityAPI satellite = iterator.next();
            removeSatellite(satellite);
            iterator.remove();//todo: methodize. this one might need a refactor
        }
    }
    /**
     * If getSatelliteFleet() does not return null, despawns the fleet and then sets the reference to null.
     */
    public void deleteSatelliteFleets() {
        List<CampaignFleetAPI> satelliteFleets = getSatelliteFleets();
        if (satelliteFleets != null && satelliteFleets.size() != 0) {
            Iterator<CampaignFleetAPI> iterator = satelliteFleets.iterator();

            while (iterator.hasNext()) {
                CampaignFleetAPI fleet = iterator.next();
                fleet.despawn();
                iterator.remove();
            }
        }
    }

    ///////////////////////////////
    //                           //
    //        RULECMD SHIT       //
    //                           //
    ///////////////////////////////

    /**
     * @param fleet The fleet to be checked.
     * @return True if 1. market is uncolonized, 2. market's faction is inhospitable or worse to the fleet, 3. The fleet has their transponder off.
     */
    public boolean satellitesWantToBlockFleet (CampaignFleetAPI fleet){

        if (getMarket().isPlanetConditionMarketOnly()) { //uncolonized planets are always hostile
            return true;
        }

        FactionAPI fleetFaction = fleet.getFaction();
        FactionAPI satelliteFaction = getSatelliteFaction();
        return ((satelliteFaction.isAtBest(fleetFaction, RepLevel.INHOSPITABLE)) || (!(fleet.isTransponderOn())));
    }

    /**
     * @return True if isInsignificant(getSatelliteFleet()) returns false. Else, also returns false.
     */
    public boolean satellitesCapableOfBlockingPlayerFleet () {
        if (getSatelliteGracePeriod() > 0) {
            return false;
        }
        return true;
    }

    /////////////////////////
    //                     //
    // GETTERS AND SETTERS //
    //                     //
    /////////////////////////

    public List<CustomCampaignEntityAPI> getSatellites () {
        return satellites;
    }

    public MarketAPI getMarket () {
        return market;
    }

    public void setMarket (MarketAPI market){
        this.market = market;
    }

    public SectorEntityToken getEntity () {
        return entity;
    }

    public float getSatelliteOrbitRadius() {
        return satelliteOrbitRadius;
    }

    public String getSatelliteFactionId () {
        return satelliteFactionId;
    }

    public FactionAPI getSatelliteFaction () {
        return Global.getSector().getFaction(getSatelliteFactionId());
    }

    public String getSatelliteFleetName () {
        return satelliteFleetName;
    }

    public String getSatelliteFleetType() {
        return satelliteFleetType;
    }

    public void setSatelliteFactionId(String factionId){
        satelliteFactionId = factionId;
    }

    public void setSatelliteFactionIdWithDefault(String factionId){
        if (getMarket().isPlanetConditionMarketOnly()) {
            factionId = "derelict";
        }
        setSatelliteFactionId(factionId);
    }

    public List<CampaignFleetAPI> getSatelliteFleets() {
        return satelliteFleets;
    }

    public HashMap<String, Float> getSatelliteVariantWeightedIds() {
        return satelliteVariantIds;
    }

    public void doneInfluencingBattle(BattleAPI battle) {
        influencedBattles.remove(battle);
    }

    public void setSatelliteGracePeriod(float grace) {
        satelliteGracePeriod = grace;
    }

    public void incrementSatelliteGracePeriod(float grace) {
        satelliteGracePeriod += grace;
    }

    public float getSatelliteGracePeriod() {
        return satelliteGracePeriod;
    }

    public float getSatelliteSpawnRadius() {
        return (getSatelliteOrbitRadius()/1.1f);
    }

    ///////////////////////////////
    //                           //
    //           TESTS           //
    //                           //
    ///////////////////////////////

    public void doSatelliteNaNFacingTest () {
        Iterator<CustomCampaignEntityAPI> iterator = getSatellites().iterator();
        while (iterator.hasNext()) {
            CustomCampaignEntityAPI satellite = iterator.next();
            if (Float.valueOf(satellite.getFacing()).isNaN()) { //fixme: debug code
                if (!(marketHasFuckedUpAndWasLogged)) {
                    PlanetAPI planet = (PlanetAPI) market.getPrimaryEntity();
                    log.debug("niko_MPC_ERROR: " + market.getName() + ", type " + planet.getTypeId() + ", had a satellite with NaN facing in " + market.getStarSystem().getName());
                    Global.getSector().getCampaignUI().addMessage("niko_MPC_ERROR: " + market.getName() + ", type " + planet.getTypeId() + ", had a satellite with NaN facing in " + market.getStarSystem().getName());
                    marketHasFuckedUpAndWasLogged = true;
                }
                removeSatellite(satellite);
                //regenerateOrbitSpacing(market);
                iterator.remove();
            }
        }
    }
}
