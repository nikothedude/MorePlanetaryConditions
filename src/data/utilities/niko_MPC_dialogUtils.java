package data.utilities;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.ui.P;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_dialogUtils {

    public static boolean createSatelliteFleetFocus(final CampaignFleetAPI satelliteFleet, final List<CampaignFleetAPI> satelliteFleets, InteractionDialogAPI dialog, final Map<String, MemoryAPI> memoryMap) {
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

                MarketAPI market = entity.getMarket();
                niko_MPC_satelliteTrackerScript script = null;
                if (market != null) {
                    script = getInstanceOfSatelliteTracker(market);
                }

                for (CampaignFleetAPI fleet : satelliteFleets) {
                    fleet.getMemoryWithoutUpdate().clear();
                    fleet.clearAssignments();
                    fleet.deflate();
                }

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.isBattleOver()) {
                        for (CampaignFleetAPI fleet : satelliteFleets) {
                            fleet.despawn(); //battles over, go home
                        }
                    } else {
                        if (script != null) {
                            script.influencedBattles.add(context.getBattle());
                        }
                    }
                    if (context.didPlayerWinEncounterOutright()) {
                        if (script != null) {
                            script.setSatelliteGracePeriod(20f);
                        }
                        FireBest.fire(null, dialog, memoryMap, "BeatDefenseSatellitesContinue");
                    } else {
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }

            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = true;
                bcc.objectivesAllowed = true;
                bcc.enemyDeployAll = true;
            }

            @Override
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
            }

        };

        dialog.setPlugin(plugin);
        plugin.init(dialog);

        return true;
    }
}
