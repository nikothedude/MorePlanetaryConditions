package data.scripts.campaign.terrain

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import data.scripts.everyFrames.niko_MPC_HTFactorTracker

/// A class I use to make my terrain scannable via hyperspace topography
fun interface niko_MPC_scannableTerrain {
    fun onScanned(
        factorTracker: niko_MPC_HTFactorTracker,
        playerFleet: CampaignFleetAPI,
        sensorBurstAbility: AbilityPlugin
    )
}