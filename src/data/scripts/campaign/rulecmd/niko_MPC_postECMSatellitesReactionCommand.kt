package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.ActionType
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript.MilitaryResponseParams
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_marketUtils.isInhabited

class niko_MPC_postECMSatellitesReactionCommand: BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: MutableList<Misc.Token>,
        memoryMap: MutableMap<String, MemoryAPI>
    ): Boolean {
        val entity = dialog.interactionTarget ?: return false
        val market = entity.market ?: return false
        if (!market.isInhabited()) return false

        if (!market.faction.getCustomBoolean(Factions.CUSTOM_NO_WAR_SIM)) {
            val responseParams = MilitaryResponseParams(
                ActionType.HOSTILE,
                "MPC_ecmSatellites_" + market.id,
                market.faction,
                market.primaryEntity,
                0.1f, // its not really a big deal
                9f // i think this is days
            )
            market.containingLocation.addScript(MilitaryResponseScript(responseParams))
        }

        return true
    }
}