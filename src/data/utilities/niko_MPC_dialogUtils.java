package data.utilities;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;

import java.util.Map;

public class niko_MPC_dialogUtils {

    public static boolean createSatelliteFleetFocus(final CampaignFleetAPI satelliteFleet, InteractionDialogAPI dialog, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        if (satelliteFleet == null) return false;

        final SectorEntityToken entity = dialog.getInteractionTarget();

        dialog.setInteractionTarget(satelliteFleet);
        final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = true;
        config.impactsEnemyReputation = true;
        config.pullInAllies = true;
        config.pullInEnemies = true;
        config.pullInStations = true;
        config.lootCredits = true;

        config.firstTimeEngageOptionText = "Engage the automated defenses";
        config.afterFirstTimeEngageOptionText = "Re-engage the automated defenses";
        config.noSalvageLeaveOptionText = "Continue";

        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);
        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();

        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return;

                satelliteFleet.setLocation(10000000, 10000000);
                satelliteFleet.despawn();

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.didPlayerWinEncounterOutright()) {
                        niko_MPC_satelliteUtils.incrementSatelliteGracePeriod(5f, entity);
                        FireBest.fire(null, dialog, memoryMap, "niko_MPC_DefenseSatellitesDefeated");
                    } else {
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }
        };
        dialog.setPlugin(plugin);
        plugin.init(dialog);

        return true;
    }
}
