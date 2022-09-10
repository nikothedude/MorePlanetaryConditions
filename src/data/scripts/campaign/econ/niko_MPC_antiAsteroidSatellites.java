package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.industries.niko_MPC_defenseSatelliteLuddicSuppressor;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.scripts.everyFrames.niko_MPC_satelliteRemovalScript;
import data.utilities.niko_MPC_satelliteUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.HashMap;

import static data.utilities.niko_MPC_ids.luddicPathSuppressorStructureId;
import static data.utilities.niko_MPC_planetUtils.getMaxPhysicalSatellitesBasedOnEntitySize;
import static data.utilities.niko_MPC_satelliteUtils.defenseSatellitesApplied;
import static data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteParams;
import static data.utilities.niko_MPC_debugUtils.displayErrorToCampaign;
import static data.utilities.niko_MPC_debugUtils.logEntityData;

public class niko_MPC_antiAsteroidSatellites extends BaseHazardCondition {

    private static final Logger log = Global.getLogger(niko_MPC_antiAsteroidSatellites.class);

    static {
        log.setLevel(Level.ALL);
    }

    /**
     * A hashmap containing variantId, plus weight, in float, to be picked, when a satellite is spawned.
     */
    public HashMap<String, Float> weightedVariantIds = new HashMap<>();
    public int maxPhysicalSatellites = 0;
    public int maxBattleSatellites = 0;
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
    public float baseGroundDefenseMult = 1.5f;

    public niko_MPC_antiAsteroidSatellites() {
        weightedVariantIds.put("rampart_Standard", 5f);
    }

    @Override
    public void apply(String id) {
        SectorEntityToken primaryEntity = market.getPrimaryEntity();
        if (primaryEntity != null) {
            // important for ensuring the same density of satellites for each entity. they will all have the same ratio of satellite to radius
            maxPhysicalSatellites = getMaxPhysicalSatellitesBasedOnEntitySize(primaryEntity);
            maxBattleSatellites = niko_MPC_satelliteUtils.getMaxBattleSatellites(primaryEntity); //todo: placeholder
            if (!defenseSatellitesApplied(primaryEntity)) { // if our entity is not supposed to have satellites
                initializeSatellitesOntoHolder(); // we should make it so that it should
            } else if (niko_MPC_satelliteUtils.marketsDesynced(primaryEntity, market)) { // if it is, check if its tracking market correctly
                ensureOldMarketHasNoReferencesFailsafe();
                niko_MPC_satelliteUtils.syncMarket(primaryEntity, market); // if not, set its memory key to market
            }
            else marketApplicationOrderTest(); //if markets arent desynced, make sure the order didnt get fucked up. todo: remove later
        }
        handleConditionAttributes(id, market); //whenever we apply or re-apply this condition, we first adjust our numbered bonuses and malices
    }

    /**
     * Exists mainly so that I can put custom behavior here.
     */
    public void initializeSatellitesOntoHolder() {
        float orbitDistance = getSatelliteOrbitDistance(market.getPrimaryEntity());
        float interferenceDistance = getSatelliteInterferenceDistance(market.getPrimaryEntity(), orbitDistance);
        float barrageDistance = getSatelliteBarrageDistance(market.getPrimaryEntity());
        niko_MPC_satelliteParams params = new niko_MPC_satelliteParams(
                satelliteId,
                satelliteFactionId,
                maxPhysicalSatellites,
                maxBattleSatellites,
                orbitDistance,
                interferenceDistance,
                barrageDistance,
                weightedVariantIds);

        niko_MPC_satelliteUtils.initializeSatellitesOntoEntity(market.getPrimaryEntity(), market, params);
    }

    private float getSatelliteBarrageDistance(SectorEntityToken primaryEntity) {
        return (primaryEntity.getRadius()+500);
    }

    private float getSatelliteOrbitDistance(SectorEntityToken entity) {
        return getSatelliteOrbitDistance(entity, false);
    }

    private float getSatelliteOrbitDistance(SectorEntityToken entity, boolean useParams) {
        if (useParams) {
            return getEntitySatelliteParams(entity).satelliteOrbitDistance;
        }
        float extraRadius = 15f;
        return entity.getRadius() + extraRadius;
    }

    private float getSatelliteInterferenceDistance(SectorEntityToken entity) {
        return getSatelliteInterferenceDistance(entity, getEntitySatelliteParams(entity).satelliteOrbitDistance);
    }

    private float getSatelliteInterferenceDistance(SectorEntityToken primaryEntity, float orbitDistance) {
        return orbitDistance;
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
        float groundDefenseMult = getGroundDefenseMult();
        float stabilityIncrement = getStabilityBonus();

        market.getAccessibilityMod().modifyFlat(id, accessibilityIncrement, getName());
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, groundDefenseMult, getName());
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

    public float getGroundDefenseMult() {
        return baseGroundDefenseMult;
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

        market.getPrimaryEntity().addScript(new niko_MPC_satelliteRemovalScript(market.getPrimaryEntity(), condition.getId())); //adds a script that will check the next frame if the market has no condition,
        // and will remove the satellites and such if it doesnt. whatever the case, it removes itself next frame
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
                "%s defense rating",
                10f,
                Misc.getHighlightColor(),
                ("+" + getGroundDefenseMult() + "x")
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

    public void marketApplicationOrderTest() {
        SectorEntityToken entity = market.getPrimaryEntity();

        if ((entity.getMarket() != market)) {
            log.debug(market.getName() + " tried applying itself when it wasn't recognized as" + entity + "'s market, which is" +
                    entity.getMarket().getName() + ". Debug info: System-" + entity.getStarSystem().getName() + " Market" +
                    "condition market status" + market.isPlanetConditionMarketOnly() + entity.getMarket().isPlanetConditionMarketOnly());
            logEntityData(entity);
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("marketApplicationOrderTest failure");
            }
        }
    }

    public void ensureOldMarketHasNoReferencesFailsafe() {
        SectorEntityToken entity = market.getPrimaryEntity();

        if (entity.getMarket() != market && entity.getMarket().hasIndustry(luddicPathSuppressorStructureId)) {
           /* log.debug(entity.getName() + "'s " + entity.getMarket() + " failed the reference failsafe in " + entity.getStarSystem().getName());
            logEntityData(entity);
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("ensureOldMarketHasNoReferencesFailsafe failure");
            } */ //commented out since i got what i want from it: it DOES fire, meaning this IS needed
            entity.getMarket().removeIndustry(luddicPathSuppressorStructureId, null, false);
        }
    }
}
