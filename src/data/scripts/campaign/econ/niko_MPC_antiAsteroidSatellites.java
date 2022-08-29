package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.industries.niko_MPC_defenseSatelliteLuddicSuppressor;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.ArrayList;

import static data.utilities.niko_MPC_generalUtils.instantiateMemoryKey;
import static data.utilities.niko_MPC_generalUtils.luddicPathSuppressorStructureId;
import static data.utilities.niko_MPC_planetUtils.*;
import static data.utilities.niko_MPC_satelliteUtils.*;
import static data.utilities.niko_MPC_scriptUtils.*;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {
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

    private boolean maxPhysicalSatellitesOverridden = false;
    public int maxPhysicalSatellites;
    public String satelliteId = "niko_MPC_derelict_anti_asteroid_satellite";
    public String satelliteFactionId = "derelict";

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

        handleConditionStats(id, market); //whenever we apply or re-apply this condition, we first adjust our numbered bonuses and malices
        if (!(market.hasIndustry(luddicPathSuppressorStructureId))) {
            market.addIndustry(luddicPathSuppressorStructureId);
        }

        // if we needed to add a new tracker
        addSatelliteTrackerIfNoneIsPresent(market, market.getPrimaryEntity(), maxPhysicalSatellites, satelliteId, satelliteFactionId); //fixme: something in this causes a error
    }

    private String appendSatelliteNumberToId(MarketAPI market, String idToAppendTo) {
        int currentAmountOfSatellites = getNumSatellitesInOrbitOfMarket(market);
        int satelliteNumber = (currentAmountOfSatellites + 1); //this makes the id of the satellite the same as the actual number of the satellite it is
        return (idToAppendTo + (" " + satelliteNumber));  //at zero satellites, the sat is given "id 1", at 1 sats, the sat is given "id 2", etc
    }

    public void handleConditionStats(String id, MarketAPI market) {
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
        return satelliteId;
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
        if (market.hasIndustry(luddicPathSuppressorStructureId)) {
            market.removeIndustry(luddicPathSuppressorStructureId, null, false);
        }

        market.unsuppressCondition("meteor_impacts");

        // more stuff
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) { //fixme: things like hazard rating arent using percents and have a .0 trail since float
        super.createTooltipAfterDescription(tooltip, expanded);

        float patherInterestReductionAmount = 0f;

        if (market.hasIndustry(luddicPathSuppressorStructureId)) {
            niko_MPC_defenseSatelliteLuddicSuppressor industry = (niko_MPC_defenseSatelliteLuddicSuppressor) market.getIndustry(luddicPathSuppressorStructureId);

            patherInterestReductionAmount = industry.getPatherInterest();
        }

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
                "Danger from asteroid impacts %s.",
                10f,
                Misc.getHighlightColor(),
                "nullified"
        );

        tooltip.addPara(
                "Effective Luddic Path interest reduced by %s.",
                10f,
                Misc.getHighlightColor(),
                Float.toString(patherInterestReductionAmount)
        );

        tooltip.addPara(
                "Orbital defenses %s due to satellite presence.",
                10f,
                Misc.getHighlightColor(),
                "enhanced"
        );
    }

}
