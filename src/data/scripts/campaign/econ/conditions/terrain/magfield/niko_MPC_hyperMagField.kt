package data.scripts.campaign.econ.conditions.terrain.magfield

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin

class niko_MPC_hyperMagField: MagneticFieldTerrainPlugin() {

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)
        if (name == "Magnetic Field") {
            name = "Ultra-magnetic Field"
        }
    }

    override fun getFlareArcMax(): Float {
        return 360f
    }

    override fun getFlareArcMin(): Float {
        return 360f
    }

    override fun getFlareSmallArcMax(): Float {
        return 360f
    }

    override fun getFlareSmallArcMin(): Float {
        return 340f
    }

    override fun getFlareProbability(): Float {
        return 0.6f
    }

    override fun getFlareSkipLargeProbability(): Float {
        return 0f
    }

    override fun getFlareFadeOutMax(): Float {
        return 20f
    }

    override fun getFlareFadeOutMin(): Float {
        return 15f
    }

    override fun getFlareSmallFadeOutMax(): Float {
        return flareFadeOutMax
    }

    override fun getFlareSmallFadeOutMin(): Float {
        return flareFadeOutMin
    }

    override fun getNameForTooltip(): String = "Ultra-Magnetic Field"

    override fun getTerrainName(): String {
        return if (flareManager.isInActiveFlareArc(Global.getSector().playerFleet)) {
            "Ultra-Magnetic Storm"
        } else super.getTerrainName()
    }

}