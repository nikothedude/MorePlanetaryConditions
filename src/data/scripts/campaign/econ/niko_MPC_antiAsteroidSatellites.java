package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.round;
import static data.utilities.niko_MPC_generalUtils.instantiateMemoryKey;
import static data.utilities.niko_MPC_planetUtils.*;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {
    //fixme: core planets are having the sattelites added, but they dont have the condition. what? probs being applied then unapplied. for some reason

    private static final Logger log = Global.getLogger(niko_MPC_antiAsteroidSatellites.class);

    static {
        log.setLevel(Level.ALL);
    }

    private boolean maxPhysicalSatellitesOverridden = false;

    /**
     * The name of the condition, ideally will be the name of it on the condition tooltip.
     */
    protected String conditionName = "Anti-Asteroid Satellites";

    /**
     * The maximum amount of physical satellites, e.g. the ones you see in campaign, the entity we are orbiting can have.
     * Calculated using (INSERT METHOD HERE) during application.
     * Todo: Maybe add support for resizing planets? Might be inefficient.
     * Todo: Maybe move this variable onto market memory?
     */
    public int maxPhysicalSatellites = 10; //named physical as i may have a few fucking thousand in description or smthn
    // Variables below are used in instantiating a new satellite instance.
    public String defaultSatelliteName = "Domain-Era Derelict Anti-Asteroid Satellite"; //todo: maybe make this an enum or smthn, make it modular to subtypes
    public String defaultSatelliteId = "niko_MPC_derelict_anti_asteroid_satellite";
    public String satelliteType = "derelict";
    public String defaultSatelliteFaction = "derelict";
    // These variables handle the condition's shit itself
    public float baseHazardIncrement = 0f; //placeholder
    public float baseAccessibilityIncrement = -15f; //also placeholder
    public float baseGroundDefenseIncrement = 500;
    public float baseStabilityIncrement = 1;
    public int baseLuddicPathInterestIncrement = -3;
    public String baseSatelliteDescriptionId = "niko_MPE_defenseSatellite";

    public niko_MPC_antiAsteroidSatellites(int numberOfSatellitesToSet) {
        this.maxPhysicalSatellites = numberOfSatellitesToSet;
        maxPhysicalSatellitesOverridden = true;
    }

    public niko_MPC_antiAsteroidSatellites() {
    }

    @Override
    public void apply(String id) { //maybe dont use this since this will be called very often, we might want sattelite damage to exist
        if (market.getPrimaryEntity() == null) return; //todo: figure out if this actually has no consequences

        if (!maxPhysicalSatellitesOverridden) // we dont want to change a specified value, given in the constructor
            maxPhysicalSatellites = getMaxPhysicalSatellitesBasedOnEntitySize(market.getPrimaryEntity());

        handleHazardAndAccessibilityChanges(id, market); //whenever we apply or re-apply this condition, we first adjust our numbered bonuses and malices

        // todo: can we use hasCondition()?
        MemoryAPI marketMemory = (market.getMemoryWithoutUpdate());
        if (!(marketMemory.contains("$niko_MPC_defenseSatellitesInOrbit")) || (marketMemory.get("$niko_MPC_defenseSatellitesInOrbit") == null)) { // if the market doesnt think we exist
            instantiateMemoryKey(marketMemory, "$niko_MPC_defenseSatellitesInOrbit", new ArrayList<CustomCampaignEntityAPI>()); //lets tell it we do
            addSatellitesToMarket(market, maxPhysicalSatellites); // and since we probably have none, lets add some satellites
        }

        handleSatelliteStatusOfMarket(market); //todo: consider consolidating addSatellitesToMarket into this method
    }

    public void handleSatelliteStatusOfMarket(MarketAPI market) { //placeholder name
        return; //todo: placeholder

    }

    public void addSatellitesToMarket(MarketAPI market, int amountOfSatellitesToAdd) {
        StarSystemAPI system = market.getStarSystem();
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(market, false);
        }
        regenerateOrbitSpacing(market); //only needs to be done once, after all the satellites are added
    }

    public void addSatellite(MarketAPI market) {
        addSatellite(market, true);
    }

    public void addSatellite(MarketAPI market, boolean regenerateOrbit) {
        StarSystemAPI system = market.getStarSystem();

        List<CustomCampaignEntityAPI> satellitesInOrbit; //var instantiated here to save horizontal space
        satellitesInOrbit = getSatellitesInOrbitOfMarket(market);

        int satelliteNumber = ((satellitesInOrbit.size()) + 1); //the number of the satellite we are adding, used for tracking it

        String orderedSatelliteId = (getSatelliteId() + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc

        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = system.addCustomEntity(orderedSatelliteId, defaultSatelliteName, defaultSatelliteId, defaultSatelliteFaction);
        addOrbitAroundSectorEntity(satellite, market.getPrimaryEntity()); //then add the satellite to the planet we are orbiting

        satellitesInOrbit.add(satellite); //returns the same number as appendSatelliteNumberToId

        satellite.setCustomDescriptionId(baseSatelliteDescriptionId);

        if (regenerateOrbit)
            regenerateOrbitSpacing(market);
    }

    public void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity) {// todo: why am i using this when i initialize a satellite? why not just use the offsets instantly instead of regenning?
        addOrbitAroundSectorEntity(satellite, entity, (entity.getCircularOrbitAngle()));
    }

    public void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity, float orbitAngle) {
        float orbitRadius = (entity.getRadius()); //todo: placeholder math
        float orbitDays = (entity.getCircularOrbitPeriod()); //my understanding is that this method returns how many days it takes for this object to do a complete orbit

        satellite.setCircularOrbitPointingDown(entity, orbitAngle, orbitRadius, orbitDays);
        //todo: pointingdown will require the sprite to be tuned for the cannons and guns and shit to face away from the planet

    }

    /**
     * Removes all satellites orbiting this market.
     * @param market The target market.
     */
    public void removeSatellitesFromMarket(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfMarket(market);
        removeSatellitesFromMarket(market, satellitesInOrbit);
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from market's orbit, taking satellites from listToUse.
     * @param market The target market.
     * @param listToUse The list from which satellites will be taken.
     */
    public void removeSatellitesFromMarket(MarketAPI market, List<CustomCampaignEntityAPI> listToUse) {

        List<CustomCampaignEntityAPI> listToUseCopy = new ArrayList<>(listToUse);
        // we make a copy and manipualte the values in this copy to avoid a concurrentmodificationexception
        // the result is hopefully the same, and it seems it is, from testing, although fixme: when satellites are removed, the arraylist still has a size of 1?

        for (CustomCampaignEntityAPI satellite : listToUseCopy) {
            removeSatellite(market, satellite, false);
        }

        regenerateOrbitSpacing(market);
        if (listToUseCopy == market.getMemoryWithoutUpdate().get("$niko_MPC_defenseSatellitesInOrbit")) { //todo: sloppy code, methodize it
            market.getMemoryWithoutUpdate().set("$niko_MPC_defenseSatellitesInOrbit", null); //we need this for reapplying fixme: throws a error. fix it
            market.getMemoryWithoutUpdate().unset("$niko_MPC_defenseSatellitesInOrbit");
        }
    }

    public void removeSatellite(MarketAPI market, CustomCampaignEntityAPI satellite) {
        removeSatellite(market, satellite, true);
    }

    public void removeSatellite(MarketAPI market, CustomCampaignEntityAPI satellite, boolean regenerateOrbit) {

        Misc.fadeAndExpire(satellite); //both this and removeEntity dont cause the NaN

        satellite.getContainingLocation().removeEntity(satellite);
        getSatellitesInOrbitOfMarket(market).remove(satellite);

        if (regenerateOrbit) {
            regenerateOrbitSpacing(market);
        }
    }

    public void regenerateOrbitSpacing(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbitOfMarket = getSatellitesInOrbitOfMarket(market);

        float optimalOrbitAngleOffset = getOptimalOrbitalAngleForSatellites(market.getPrimaryEntity(), satellitesInOrbitOfMarket);
        float orbitAngle = 0;
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc, so its safe to not add a buffer to the calculation in the optimalangle method
        for (CustomCampaignEntityAPI satellite : satellitesInOrbitOfMarket) { //iterates through each orbitting satellite and offsets them
            if (orbitAngle >= 360) {
                if (Global.getSettings().isDevMode()) {
                    Global.getSector().getCampaignUI().addMessage("A satellite on " + market + " was given a orbit offset of " + orbitAngle + "."); //debug code
                    log.debug("A satellite on " + market + " was given a orbit offset of " + orbitAngle + ".");
                    removeSatellite(market, satellite, false); //we dont want these weirdos overlapping
                }
            }
            addOrbitAroundSectorEntity(satellite, market.getPrimaryEntity(), orbitAngle);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    private String appendSatelliteNumberToId(MarketAPI market, String idToAppendTo) {
        int currentAmountOfSatellites = getNumSatellitesInOrbitOfMarket(market);
        int satelliteNumber = (currentAmountOfSatellites + 1); //this makes the id of the satellite the same as the actual number of the satellite it is
        return (idToAppendTo + (" " + satelliteNumber));  //at zero satellites, the sat is given "id 1", at 1 sats, the sat is given "id 2", etc
    }

    public void handleHazardAndAccessibilityChanges(String id, MarketAPI market) {
        if (market.hasCondition("meteor_impacts")) {
            market.suppressCondition("meteor_impacts"); //these things just fuck those things up
        }

        //maths to handle the changing values go in the getters
        float hazardIncrement = getHazardBonus();
        float accessibilityIncrement = getAccessibilityBonus();
        float groundDefenseIncrement = getGroundDefenseBonus();
        float stabilityIncrement = getStabilityBonus();

        market.getHazard().modifyFlat(id, hazardIncrement, getName());
        market.getAccessibilityMod().modifyFlat(id, accessibilityIncrement, getName());
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, groundDefenseIncrement, getName());
        market.getStability().modifyFlat(id, stabilityIncrement, getName());

    }
    public String getSatelliteId() {
        return defaultSatelliteId;
    }

    public float getHazardBonus() {
        return baseHazardIncrement;
    }

    public float getAccessibilityBonus() {
        return baseAccessibilityIncrement;
    }

    public float getGroundDefenseBonus() {
        return baseGroundDefenseIncrement;
    }

    public float getStabilityBonus() {
        return baseStabilityIncrement;
    }

    public int getLuddicPathInterestBonus() {
        return baseLuddicPathInterestIncrement;
    }

    public String getName() {
        return conditionName;
    }

    @Override
    public void unapply(String id) { //todo: i need to figure out how to ACTUALLY remove this condition if it comes to it, since apply and unapply is called during reapply
        super.unapply(id);

        market.getStability().unmodify(id);
        market.getAccessibilityMod().unmodify(id);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);

        market.unsuppressCondition("meteor_impacts");

        removeSatellitesFromMarket(market);
        // more stuff
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) { //fixme: things like hazard rating arent using percents and have a .0 trail since float
        super.createTooltipAfterDescription(tooltip, expanded);

        tooltip.addPara(
                "%s hazard rating",
                10f,
                Misc.getHighlightColor(),
                ("+" + getHazardBonus())
        );

        tooltip.addPara(
                "%s stability",
                10f,
                Misc.getHighlightColor(),
                ("+" + getStabilityBonus())
        );

        tooltip.addPara(
                "%s accessibility",
                10f,
                Misc.getHighlightColor(),
                (getAccessibilityBonus() + "%")
        );

        tooltip.addPara(
                "%s defense rating",
                10f,
                Misc.getHighlightColor(),
                ("+" + getGroundDefenseBonus())
        );

        tooltip.addPara(
                "Effective Luddic Path interest reduced by %s.",
                10f,
                Misc.getHighlightColor(),
                String.valueOf((getLuddicPathInterestBonus()))
        );

        tooltip.addPara(
                "Danger from asteroid impacts %s.",
                10f,
                Misc.getHighlightColor(),
                "nullified"
        );

        tooltip.addPara(
                "Orbital defenses %s due to satellite presence.",
                10f,
                Misc.getHighlightColor(),
                "enhanced"
        );
    }

}
