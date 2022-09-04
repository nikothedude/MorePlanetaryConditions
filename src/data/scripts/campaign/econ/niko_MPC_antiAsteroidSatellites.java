package data.scripts.campaign.econ;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.industries.niko_MPC_defenseSatelliteLuddicSuppressor;

import java.util.HashMap;

import static data.utilities.niko_MPC_ids.luddicPathSuppressorStructureId;
import static data.utilities.niko_MPC_planetUtils.getMaxPhysicalSatellitesBasedOnEntitySize;
import static data.utilities.niko_MPC_scriptUtils.addSatelliteTrackerIfNoneIsPresent;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {

    /**
     * A hashmap containing variantId, plus weight, in float, to be picked, when a satellite is spawned.
     */
    public HashMap<String, Float> weightedVariantIds = new HashMap<>();
    public int maxPhysicalSatellites = 15;
    /**
     * The internal Id that will be applied to satellite entities, not fleets. Always is appended by its position in the satellite list.
     */
    public String satelliteId = "niko_MPC_derelict_anti_asteroid_satellite";

    //todo: do i need this
    public String satelliteFactionId = "derelict";

    // These variables handle the condition's shit itself
    public float baseAccessibilityIncrement = -0.15f; //also placeholder
    public float baseGroundDefenseIncrement = 500;
    public float baseStabilityIncrement = 1;

    public niko_MPC_antiAsteroidSatellites() {
        weightedVariantIds.put("niko_MPC_derelictSatellite_Artillery", 5f);
    }

    @Override
    public void apply(String id) {
        if (market.getPrimaryEntity() == null) return; //todo: figure out if this actually has no consequences

        // important for ensuring the same density of satellites for each entity. they will all have the same ratio of satellite to radius
        maxPhysicalSatellites = getMaxPhysicalSatellitesBasedOnEntitySize(market.getPrimaryEntity());

        handleConditionAttributes(id, market); //whenever we apply or re-apply this condition, we first adjust our numbered bonuses and malices
        //note: this method is also what applies the luddic path suppressing industry

        // if we need to add a new tracker
        addSatelliteTrackerIfNoneIsPresent(market, market.getPrimaryEntity(), maxPhysicalSatellites, getSatelliteId(), satelliteFactionId, weightedVariantIds); // we add one
    }

    /**
     * Where all the condition attributes (accessability, hazard, etc) are handled. This is NOT where the satellite tracker is handled.
     * @param id
     * @param market
     */
    public void handleConditionAttributes(String id, MarketAPI market) {
        if (market.hasCondition("meteor_impacts")) {
            market.suppressCondition("meteor_impacts"); //these things just fuck those things up
        }

        if (!(market.hasIndustry(luddicPathSuppressorStructureId))) { // when we apply, we check to see if our luddic path suppression is active
            market.addIndustry(luddicPathSuppressorStructureId); //if it isnt, we make it active
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

        int convertedAccessibilityBonus = (int) (getAccessibilityBonus() * 100);  //times 100 to convert out of decimal

        tooltip.addPara(
                "%s accessibility",
                10f,
                Misc.getHighlightColor(),
                (convertedAccessibilityBonus + "%")
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
