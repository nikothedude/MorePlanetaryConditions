package data.scripts.campaign.econ.conditions.terrain.magfield

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTScanFactor
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import data.scripts.campaign.listeners.niko_MPC_saveListener
import data.scripts.campaign.terrain.niko_MPC_scannableTerrain
import data.scripts.everyFrames.niko_MPC_HTFactorTracker

class niko_MPC_hyperMagField: MagneticFieldTerrainPlugin(), niko_MPC_saveListener, niko_MPC_scannableTerrain {

    companion object {
        const val HYPERSPACE_TOPOGRAPHY_POINTS = 30
    }

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)
        if (name == "Magnetic Field") {
            name = "Ultra-Magnetic Field"
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

    override fun onGameLoad() {
        return
    }

    override fun onScanned(
        factorTracker: niko_MPC_HTFactorTracker,
        playerFleet: CampaignFleetAPI,
        sensorBurstAbility: AbilityPlugin
    ) {

        if (!containsEntity(playerFleet)) return

        val id = entity.id

        if (factorTracker.scanned.contains(id)) {
            factorTracker.reportNoDataAcquired("Ultra-Magnetic Field already scanned")
        } else {
            HyperspaceTopographyEventIntel.addFactorCreateIfNecessary(
                HTScanFactor("Ultra-Magnetic Field scanned", HYPERSPACE_TOPOGRAPHY_POINTS), null
            )
            factorTracker.scanned.add(id)
        }
    }

}