package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.getPhaseShipPercent
import data.utilities.niko_MPC_settings.indEvoEnabled

class niko_MPC_marketBlockedByArtyStation: BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: MutableList<Misc.Token>,
        memoryMap: MutableMap<String, MemoryAPI>
    ): Boolean {
        if (!indEvoEnabled) return false

        if (dialog.interactionTarget.market?.isPlanetConditionMarketOnly == false) return false

        val memory = dialog.interactionTarget?.memoryWithoutUpdate ?: return false

        if (memory["\$IndEvo_ArtilleryStation"] != true) return false
        if (memory["\$defenderFleetDefeated"] == true) return false

        return true
    }
}