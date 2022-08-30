package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.industries.niko_MPC_defenseSatelliteLuddicSuppressor;

import static data.utilities.niko_MPC_ids.luddicPathSuppressorStructureId;
import static data.utilities.niko_MPC_planetUtils.getMaxPhysicalSatellitesBasedOnEntitySize;
import static data.utilities.niko_MPC_scriptUtils.addSatelliteTrackerIfNoneIsPresent;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {

    /**
     * The maximum amount of physical satellites, e.g. the ones you see in campaign, the entity we are orbiting can have.
     * Calculated using getMaxPhysicalSatellitesBasedOnEntitySize during application.
     * Todo: Maybe add support for resizing planets? Might be inefficient.
     * Todo: Maybe move this variable onto market memory?
     */

    private boolean maxPhysicalSatellitesOverridden = false;
    public int maxPhysicalSatellites;
    public String satelliteId = "niko_MPC_derelict_anti_asteroid_satellite";
    public String satelliteFactionId = "derelict";

    // These variables handle the condition's shit itself
    public float baseAccessibilityIncrement = -15f; //also placeholder
    public float baseGroundDefenseIncrement = 500;
    public float baseStabilityIncrement = 1;

    public niko_MPC_antiAsteroidSatellites(int numberOfSatellitesToSet) {
        this.maxPhysicalSatellites = numberOfSatellitesToSet;
        maxPhysicalSatellitesOverridden = true;
    }

    public niko_MPC_antiAsteroidSatellites() {
    }

    @Override
    public void apply(String id) {
        if (market.getPrimaryEntity() == null) return; //todo: figure out if this actually has no consequences

        if (!maxPhysicalSatellitesOverridden) // we dont want to change a specified value, given in the constructor
            maxPhysicalSatellites = getMaxPhysicalSatellitesBasedOnEntitySize(market.getPrimaryEntity());

        handleConditionStats(id, market); //whenever we apply or re-apply this condition, we first adjust our numbered bonuses and malices
        if (!(market.hasIndustry(luddicPathSuppressorStructureId))) { // when we apply, we check to see if our luddic path suppression is active
            market.addIndustry(luddicPathSuppressorStructureId); //if it isnt, we make it active
        }

        // if we need to add a new tracker
        addSatelliteTrackerIfNoneIsPresent(market, market.getPrimaryEntity(), maxPhysicalSatellites, getSatelliteId(), satelliteFactionId); // we add one
    }

    public void handleConditionStats(String id, MarketAPI market) {
        if (market.hasCondition("meteor_impacts")) {
            market.suppressCondition("meteor_impacts"); //these things just fuck those things up
        }

        //maths to handle the changing values go in the getters
        float accessibilityIncrement = getAccessibilityBonus();
        float groundDefenseIncrement = getGroundDefenseBonus();
        float stabilityIncrement = getStabilityBonus();

        market.getAccessibilityMod().modifyFlat(id, accessibilityIncrement, getName());
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, groundDefenseIncrement, getName());
        market.getStability().modifyFlat(id, stabilityIncrement, getName());
    }

    public MarketAPI getMarket() {
        return market;
    }
    public String getSatelliteId() {
        return satelliteId;
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

    public float getLuddicPathInterestBonus() {
        if (getMarket().hasIndustry(luddicPathSuppressorStructureId)) {
            niko_MPC_defenseSatelliteLuddicSuppressor industry = (niko_MPC_defenseSatelliteLuddicSuppressor) market.getIndustry(luddicPathSuppressorStructureId);
            return industry.getPatherInterest();
        }
        return 0;
    }

    @Override
    public void unapply(String id) {
    //    super.unapply(id);
        getMarket().getAccessibilityMod().unmodify(id);
        getMarket().getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
        getMarket().getStability().unmodify(id);
        if (getMarket().hasIndustry(luddicPathSuppressorStructureId)) {
            getMarket().removeIndustry(luddicPathSuppressorStructureId, null, false);
        }
        getMarket().unsuppressCondition("meteor_impacts");
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        float patherInterestReductionAmount = 0f;

        if (market.hasIndustry(luddicPathSuppressorStructureId)) {
            niko_MPC_defenseSatelliteLuddicSuppressor industry = (niko_MPC_defenseSatelliteLuddicSuppressor) market.getIndustry(luddicPathSuppressorStructureId);

            patherInterestReductionAmount = industry.getPatherInterest();
        }

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
