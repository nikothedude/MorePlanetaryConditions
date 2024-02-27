package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import data.compatability.baseNikoEventStage
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.compatability.overgrownNanoforgeIntelStage
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeIndustryManipulationIntel
import data.utilities.niko_MPC_debugUtils
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console
import java.lang.Exception

class niko_MPC_convertStagesToEnum : BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (!context.isInCampaign) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }

        if (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] == null) {
            Console.showMessage("No handlers detected, command aborted.")
            return BaseCommand.CommandResult.SUCCESS
        }
        val handlers = (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>)

        var convertedAny = false

        try {
            for (handler: overgrownNanoforgeIndustryHandler in handlers) {
                if (handler.manipulationIntel != null) {
                    if (convertToEnum(handler.manipulationIntel!!)) convertedAny = true
                }
                if (convertToEnum(handler.intelBrain.spreadingIntel)) convertedAny = true
                for (junkHandler: overgrownNanoforgeJunkHandler in handler.junkHandlers) {
                    if (junkHandler.manipulationIntel != null) {
                        if (convertToEnum(handler.manipulationIntel!!)) convertedAny = true
                    }
                }
            }
        } catch (ex: Exception) {
            niko_MPC_debugUtils.log.error(ex)
            return BaseCommand.CommandResult.ERROR
        }

        if (convertedAny) {
            Console.showMessage("Deprecated objects detected, converted to enum.")
        } else {
            Console.showMessage("No deprecated objects detected, no action taken.")
        }
        return BaseCommand.CommandResult.SUCCESS
    }
}

    private fun convertToEnum(intel: baseOvergrownNanoforgeIntel): Boolean {
        var needToRegenerateStages = false
        for (stage: EventStageData in intel.stages) {
            if (stage.id !is baseNikoEventStage) continue
            val stages = intel.stages
            val stagesCopy = stages.toSet()
            for (iteratedStage in stagesCopy) {
                val stageId = iteratedStage.id
                if (stages.contains(stageId)) {
                    needToRegenerateStages = true
                    break
                }
            }
            if (needToRegenerateStages) {
                intel.stages.clear()
                intel.addInitialStages()
            }
        }
        return needToRegenerateStages
    }
