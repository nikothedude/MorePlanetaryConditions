package data.scripts.campaign.econ;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.ArrayList;
import java.util.List;

import static data.utilities.niko_MPC_generalUtils.getScriptsOfClass;
import static data.utilities.niko_MPC_generalUtils.instantiateMemoryKey;
import static data.utilities.niko_MPC_planetUtils.getMaxPhysicalSatellitesBasedOnEntitySize;
import static data.utilities.niko_MPC_planetUtils.getNumSatellitesInOrbitOfMarket;
import static data.utilities.niko_MPC_satelliteUtils.*;
import static data.utilities.niko_MPC_scriptUtils.addSatelliteTrackerIfNoneIsPresent;
import static data.utilities.niko_MPC_scriptUtils.addScriptIfScriptIsUnique;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {
    //fixme: core planets are having the sattelites added, but they dont have the condition. what? probs being applied then unapplied. for some reason

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
            addSatellitesToMarketPrefab(market, maxPhysicalSatellites); // and since we probably have none, lets add some satellites
        }

        addSatelliteTrackerIfNoneIsPresent(); //required to be here, since all methods in the modplugin are before the sector exists, or after planetgen
        getInstanceOfSatelliteTracker().getMarketsWithSatellites().add(market);

        handleSatelliteStatusOfMarket(market); //todo: consider consolidating addSatellitesToMarket into this method
    }

    public void addSatellitesToMarketPrefab(MarketAPI market, int amountOfSatellitesToAdd) {
        addSatellitesToMarket(market, amountOfSatellitesToAdd, defaultSatelliteId, defaultSatelliteName, defaultSatelliteFaction);
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
