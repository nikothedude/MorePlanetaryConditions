package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;

import java.util.List;
import java.util.Map;

public class niko_MPC_dialogUtils {

    public static boolean createSatelliteFleetFocus(final CampaignFleetAPI satelliteFleet, final List<CampaignFleetAPI> satelliteFleets,
                                                    InteractionDialogAPI dialog, final SectorEntityToken entityFocus, final Map<String, MemoryAPI> memoryMap,
                                                    boolean isFightingFriendly) {
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

        if (isFightingFriendly) {
            config.lootCredits = false;
            config.impactsAllyReputation = false;
            config.impactsEnemyReputation = false;
        }

        MarketAPI entityMarket = entityFocus.getMarket();
        if (entityMarket != null) {
            if (entityMarket.isPlanetConditionMarketOnly()) {
                config.impactsEnemyReputation = false;
                config.impactsAllyReputation = false;
            }
        }

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
                if (!niko_MPC_debugUtils.assertEntityHasSatellites(entityFocus)) return;

                niko_MPC_fleetUtils.despawnSatelliteFleet(satelliteFleet, true);

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.didPlayerWinEncounterOutright()) {
                        //todo: is the below needed
                        niko_MPC_satelliteUtils.incrementSatelliteGracePeriod(Global.getSector().getPlayerFleet(), niko_MPC_ids.satellitePlayerVictoryIncrement, entityFocus);
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

    public static SectorEntityToken digForSatellitesInEntity(SectorEntityToken entity) {

        MarketAPI entityMarket = entity.getMarket();
        if (entityMarket != null && entityMarket.getPrimaryEntity() != entity) {
            SectorEntityToken marketEntity = entityMarket.getPrimaryEntity();
            if (marketEntity != null) entity = marketEntity;
        }

        return entity;
    }
}
