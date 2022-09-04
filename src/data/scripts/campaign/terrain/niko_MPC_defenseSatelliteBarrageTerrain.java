package data.scripts.campaign.terrain;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;

public class niko_MPC_defenseSatelliteBarrageTerrain extends BaseRingTerrain {

    @Override
    //Main advance function; only calls our superimplementation, since all effects are handled in applyEffect() instead
    public void advance(float amount) {
        super.advance(amount);
    }

    //Render function for the effect; we don't actually render anything, so this is currently unused
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        /*-Do nothing-*/
    }

    //Runs once per affected fleet in the area, with "days" being the campaign-days representation of the more ubiquitous "amount"
    @Override
    public void applyEffect(SectorEntityToken entity, float days) {

    }

    //AI flags for the terrain. This terrain has no flags, since we don't want allies to fear it at all
    public boolean hasAIFlag(Object flag) {
        return false;
    }
}
