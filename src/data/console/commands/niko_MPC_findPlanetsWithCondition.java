package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class niko_MPC_findPlanetsWithCondition implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");

        if (tmp.length != 1)
        {
            return CommandResult.BAD_SYNTAX;
        }

        String conditionId = tmp[0];

        List<StarSystemAPI> systems = Global.getSector().getStarSystems();
        for (StarSystemAPI system : systems) {
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.hasCondition(conditionId)) {
                    boolean displayConditionName = (planet.getMarket() != null && planet.getMarket().hasCondition(conditionId));
                    String toDisplay = conditionId;
                    if (displayConditionName) {
                        toDisplay = planet.getMarket().getCondition(conditionId).getName();
                    }
                    Console.showMessage(planet.getName() + " has " + toDisplay + ", location " +
                            system.getName());
                }
            }
        }
        return CommandResult.SUCCESS;
    }
}
