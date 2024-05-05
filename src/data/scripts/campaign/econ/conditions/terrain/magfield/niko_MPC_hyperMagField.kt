package data.scripts.campaign.econ.conditions.terrain.magfield

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import data.scripts.campaign.listeners.niko_MPC_saveListener

class niko_MPC_hyperMagField: MagneticFieldTerrainPlugin(), niko_MPC_saveListener {

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
        return 340f
    }

    override fun getFlareSmallArcMax(): Float {
        return 30f
    }

    override fun getFlareSmallArcMin(): Float {
        return 10f
    }

    override fun getFlareProbability(): Float {
        return 0.2f
    }

    override fun getFlareSkipLargeProbability(): Float {
        return 0.2f // ranges from 0 to 1, lower means more large flares will spawn
    }

    override fun getFlareFadeOutMax(): Float {
        //return 20f
        return 20f
    }

    override fun getFlareFadeOutMin(): Float {
        //return 15f
        return 15f
    }

    override fun getFlareSmallFadeOutMax(): Float {
        return 1f
    }

    override fun getFlareSmallFadeOutMin(): Float {
        return 0.5f
    }

    override fun getNameForTooltip(): String = "Ultra-Magnetic Field"

    override fun getTerrainName(): String {
        return if (flareManager.isInActiveFlareArc(Global.getSector().playerFleet)) {
            "Ultra-Magnetic Storm"
        } else super.getTerrainName()
    }

    override fun readResolve(): Any {
        return super.readResolve()
    }

    override fun beforeGameSave() {
        return
    }

}