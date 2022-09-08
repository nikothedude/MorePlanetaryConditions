/*package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

import static data.utilities.niko_MPC_fleetUtils.spawnTemporarySatelliteFleetsOnFleet;

public class niko_MPC_satelliteSpawnListener extends BaseCampaignEventListener {
    public niko_MPC_satelliteSpawnListener(boolean permaRegister) {
        super(permaRegister);
    }

    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (dialog.getPlugin() instanceof FleetInteractionDialogPluginImpl) {
            FleetInteractionDialogPluginImpl plugin = (FleetInteractionDialogPluginImpl) dialog.getPlugin();
            spawnTemporarySatelliteFleetsOnFleet(Global.getSector().getPlayerFleet()); //todo: happens AFTER The diaogue opens, hmm

        }
    }
}*/
