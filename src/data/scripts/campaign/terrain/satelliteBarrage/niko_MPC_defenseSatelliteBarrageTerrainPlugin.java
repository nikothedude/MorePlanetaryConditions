/*package data.scripts.campaign.terrain.satelliteBarrage;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.RingRenderer;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_satelliteUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static data.utilities.niko_MPC_ids.defenseSatelliteImpactId;
import static data.utilities.niko_MPC_ids.defenseSatelliteImpactReasonString;
import static data.utilities.niko_MPC_satelliteUtils.getCurrentSatelliteFactionId;
import static data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlerOfEntity;

// unused, for now
public class niko_MPC_defenseSatelliteBarrageTerrainPlugin extends BaseRingTerrain {
    public static class barrageAreaParams {

        public SectorEntityToken relatedEntity;
        public float outerSize;
        public String name;
        public barrageAreaParams(float outerSize, String name, SectorEntityToken relatedEntity) {
            this.outerSize = outerSize;
            this.name = name;
            this.relatedEntity = relatedEntity;
        }

    }

    public barrageAreaParams customParams;
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        if (param instanceof barrageAreaParams) {
            customParams = (barrageAreaParams) param;
            super.init(terrainId, entity, new RingParams(customParams.outerSize, customParams.outerSize/2f, entity));
            name = customParams.name;
            params.relatedEntity = customParams.relatedEntity;
            if (name == null) {
                name = "Bombardment Zone";
            }
        }
    }

    private static final Logger log = Global.getLogger(niko_MPC_defenseSatelliteBarrageTerrainPlugin.class);

    static {
        log.setLevel(Level.ALL);
    }

    @Override
    //Main advance function; only calls our superimplementation, since all effects are handled in applyEffect() instead
    public void advance(float amount) {
        super.advance(amount);
    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        -Do nothing-
    }

    private transient RingRenderer ringRenderer;
    public void renderOnMap(float factor, float alphaMult) {
        if (params == null) return;
        if (ringRenderer == null) {
            ringRenderer = new RingRenderer("systemMap", "map_asteroid_belt");
        }
        Color color = Misc.getHighlightColor();
        float bandWidth;
        bandWidth = 130f;
        ringRenderer.render(entity.getLocation(),
                customParams.outerSize - bandWidth * 0.5f,
                customParams.outerSize + bandWidth * 0.5f,
                color,
                false, factor, 0.6f);
    }

    //Runs once per affected fleet in the area, with "days" being the campaign-days representation of the more ubiquitous "amount"
    @Override
    publicvoid applyEffect(SectorEntityToken entity, float days) {
        if (entityHasNoSatelliteParams()) return;

        if (entity instanceof CampaignFleetAPI) {
            niko_MPC_satelliteHandlerCore params = getSatelliteHandlerOfEntity(getRelatedEntity());
            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(getRelatedEntity(), fleet)) {
                if (getThreatLevel(fleet) > Math.random()) {
                    impactFleet(fleet);
                }
            }
        }
    }

    private SectorEntityToken getOrbitTarget() { /*
        return (getEntity().getOrbitFocus());
    }

    public void impactFleet(CampaignFleetAPI fleet) {
        List<FleetMemberAPI> fleetMembers = fleet.getFleetData().getMembersListCopy();
        Random rand = new Random();
        FleetMemberAPI ship = fleetMembers.get(rand.nextInt(fleetMembers.size()));
        int mult = 5;
        int volumeMult = 5;

        Misc.applyDamage(ship, null, mult, true, defenseSatelliteImpactId, defenseSatelliteImpactReasonString,
                true, null, ship.getShipName() + " is hit by long-ranged artillery");
        Global.getSoundPlayer().playSound("hit_heavy", 1f, 1f * volumeMult, fleet.getLocation(), Misc.ZERO);

        Vector2f velocity = fleet.getVelocity();
        velocity = new Vector2f(velocity);
        velocity.scale(0.7f);

        float glowSize = 100f + 100f * mult/5 + 50f * (float) Math.random();
        Color color = new Color(255, 165, 100, 255);

        Misc.addHitGlow(fleet.getContainingLocation(), fleet.getLocation(), velocity, glowSize, color);
    }

    public float getIntensityAtPoint(Vector2f point) {
        float maxDist = params.bandWidthInEngine;
        float dist = Misc.getDistance(point, params.relatedEntity.getLocation());

        if (dist > maxDist) return 0f;

        float intensity = 1f - (dist / maxDist);
        return Math.min(Math.max(intensity, 0f), 1f); //returns intensity if its more or equal to 0 and less or equal to 1
    }

    public double getThreatLevel(CampaignFleetAPI fleet,  boolean useFleetSize) {
        return getThreatLevel(fleet, 0.05f, useFleetSize);
    }

    public double getThreatLevel(CampaignFleetAPI fleet, float mult) {
        return getThreatLevel(fleet, mult, true);
    }

    public double getThreatLevel(CampaignFleetAPI fleet) {
        return getThreatLevel(fleet, 0.05f, true);
    }

    public double getThreatLevel(CampaignFleetAPI fleet, float mult, boolean useFleetSize) {
        float probMult = 1f;
        if (useFleetSize) {
            probMult = Misc.getFleetRadiusTerrainEffectMult(fleet);
        }
        float anchorPoint = 3f;
        float currentBurn = fleet.getCurrBurnLevel();
        float speedMult = 5f;
        if (currentBurn != 0) {
            speedMult = (anchorPoint/currentBurn); // at 4 and above, start getting hit less
            // at 2 and below, start getting hit more
        }

        float sensorAnchorPoint = 800f;
        float currentSensorProfile = fleet.getSensorProfile();
        StatBonus profileMod = fleet.getDetectedRangeMod();
        float flatModifier = profileMod.getFlatBonus();
        float multModifier = profileMod.getMult();
        float percentModifier = profileMod.getPercentMod();
        if (percentModifier == 0) {
            percentModifier = 1; //sanity
        }
        float modifiedSensorProfile = (((currentSensorProfile * percentModifier) + flatModifier) * multModifier); //same math as done in statbonus recompute
        double sensorMult = 0.005;
        if (currentSensorProfile != 0) {
            sensorMult = Math.min((modifiedSensorProfile/sensorAnchorPoint), 1); //at <800 profile, start getting hit less
            // never increases the chance
        }
        return ((((getIntensityAtPoint(fleet.getLocation()) * mult)*probMult)*speedMult)*sensorMult);
    }

    public boolean entityHasNoSatelliteParams() {
        if (getSatelliteHandlerOfEntity(getRelatedEntity()) == null) {
            niko_MPC_debugUtils.displayError("ensureEntityHasSatelliteParams failure");
            niko_MPC_satelliteUtils.removeSatelliteBarrageTerrain(getRelatedEntity(), getEntity());

            niko_MPC_debugUtils.logDataOf(getRelatedEntity());

            return true;
        }
        return false;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        float small = 5f;
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();
        Color fuel = Global.getSettings().getColor("progressBarFuelColor");
        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getPositiveHighlightColor();

        tooltip.addTitle(name);
        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).getText1(), pad);

        float nextPad = pad;
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad);
            nextPad = small;
        }

        float displayableIntensity = getStringifiedIntensity(Global.getSector().getPlayerFleet());
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        niko_MPC_satelliteHandlerCore params = getSatelliteHandlerOfEntity(getRelatedEntity());
        String factionName = Global.getSector().getFaction(getCurrentSatelliteFactionId(params)).getDisplayName();

        tooltip.addPara("Chance for random ships in the fleet to be hit by long-ranged artillery fire from the " +
                "orbiting satellites, causing heavy damage and minor CR loss.", nextPad);
        tooltip.addPara("Any ships entering combat within the barrage radius will be peppered by long-ranged artillery" +
                " throughout the engagement.", nextPad,
                highlight,
                "peppered by long-ranged artillery throughout the engagement");
        tooltip.addPara("In addition, the satellites will prevent any fleets inhospitable or with a disabled transponder" +
                " from docking without a manual override, and will physically join combat with any allied fleet physically" +
                " adjacent to the planet.", nextPad,
                highlight,
                "prevent any fleets inhospitable or with a disabled transponder from docking", "physically join combat with any allied fleet physically" +
                        " adjacent to the planet.");

        if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(getRelatedEntity(), playerFleet)) {
            tooltip.addPara("Your tactical officer currently estimates the threat of the satellites to be " +
                            displayableIntensity + " on the Korsh-Hoffman scale.", nextPad,
                    bad,
                    String.valueOf(displayableIntensity));
        }
        else {
            tooltip.addPara("You are currently non-hostile to the " + factionName
            + ", so you are not being fired upon by the satellites.", nextPad,
                    good,
                    factionName);
        }
    }

    public float getStringifiedIntensity(CampaignFleetAPI fleet) {
        float intensity = (float) getThreatLevel(fleet);
        intensity *= 10000f;
        intensity = Math.round(intensity);
        return (intensity); // ex .553 = 55.3
    }

    //AI flags for the terrain. This terrain has no flags, since we don't want allies to fear it at all
    public boolean hasAIFlag(Object flag, CampaignFleetAPI fleet) {
        if (entityHasNoSatelliteParams()) return false;
        niko_MPC_satelliteHandlerCore params = getSatelliteHandlerOfEntity(getRelatedEntity());
        if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(getRelatedEntity(), fleet)) {
            return (flag == TerrainAIFlags.CR_DRAIN ||
                    flag == TerrainAIFlags.EFFECT_DIMINISHED_WITH_RANGE);
        }

        return false;
    }

    //The category of effects; determines which effects to stack with. Null means stacking with everything, even itself
    @Override
    public String getEffectCategory() {
        return null;
    }

} */
