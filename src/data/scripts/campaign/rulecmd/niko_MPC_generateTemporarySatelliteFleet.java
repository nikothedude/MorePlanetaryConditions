package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_fleetUtils.generateTemporarySatelliteFleet;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_generateTemporarySatelliteFleet extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        final SectorEntityToken entity = dialog.getInteractionTarget();
        final MarketAPI market = entity.getMarket();
        if (market != null) {
            LocationAPI playerContainingLocation = Global.getSector().getPlayerFleet().getContainingLocation();
            Vector2f playerCoordinates = Global.getSector().getPlayerFleet().getLocation();

            boolean switchInteractionFocus = false;
            if (params.size() > 0) {
                switchInteractionFocus = params.get(0).getBoolean(memoryMap);
            }
            final CampaignFleetAPI satelliteFleet = generateTemporarySatelliteFleet(market, playerContainingLocation, playerCoordinates);
            if (switchInteractionFocus) {
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
                        // nothing in there we care about keeping; clearing to reduce savefile size
                        satelliteFleet.getMemoryWithoutUpdate().clear();
                        // there's a "standing down" assignment given after a battle is finished that we don't care about
                        satelliteFleet.clearAssignments();
                        satelliteFleet.deflate();

                        dialog.setPlugin(originalPlugin);
                        dialog.setInteractionTarget(entity);

                        //Global.getSector().getCampaignUI().clearMessages();

                        if (plugin.getContext() instanceof FleetEncounterContext) {
                            FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                            if (context.didPlayerWinEncounterOutright()) {

                                entity.removeScriptsOfClass(FleetAdvanceScript.class);
                                FireBest.fire(null, dialog, memoryMap, "BeatDefenseSatellitesContinue");

                                niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
                                script.setSatelliteGracePeriod(20f);
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
                };
                dialog.setPlugin(plugin);
                plugin.init(dialog);

                return true;
            }
        }
            return true;
    }
}

