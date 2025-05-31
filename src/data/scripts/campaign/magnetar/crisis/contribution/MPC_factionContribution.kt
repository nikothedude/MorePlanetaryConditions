package data.scripts.campaign.magnetar.crisis.contribution

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.ucFirst
import java.awt.Color

data class MPC_factionContribution(
    val factionId: String,
    val fleetMultIncrement: Float,
    /** Changes the effectiveness of sabotage actions. */
    val sabotageMultIncrement: Float,
    val removeContribution: Script?,
    val removeNextAction: Boolean = false,
    val requireMilitary: Boolean = false,
    val contributorExists: ContributorExistsScript? = ContributorExistsScript(factionId, requireMilitary),
    val repOnRemove: Float? = null,
    val baseMarketEmbargoValue: Float = 1f,
    val contributionId: String = factionId,
    val factionName: String = Global.getSector().getFaction(factionId).displayName.ucFirst(),
    val bulletString: String = factionName,
    var addBenefactorInfo: Boolean = true,
    var bulletColor: Color = Global.getSector().getFaction(factionId).baseUIColor,
    var benefactorSuffix: String? = null,
    var custom: Any? = null
) {
    open class ContributorExistsScript(val factionId: String, val requireMilitary: Boolean) {
        open fun run(): Boolean {
            return BaseHostileActivityFactor.checkFactionExists(factionId, requireMilitary)
        }

    }

    fun onRemoved(intel: MPC_IAIICFobIntel, becauseFactionDead: Boolean, dialog: InteractionDialogAPI? = null) {
        if (removeNextAction) {
            intel.removeNextAction()
        }
        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
        if (IAIIC != null) {
            removeContribution?.run()
            intel.removeBlueprintFunctions.add { removeContribution }
        }

        val reason = if (becauseFactionDead) MPC_changeReason.FACTION_DIED else MPC_changeReason.PULLED_OUT
        val data = MPC_factionContributionChangeData(this, reason, true)
        if (dialog != null) {
            intel.sendUpdate(data, dialog.textPanel)
        } else {
            intel.sendUpdateIfPlayerHasIntel(data, false)
        }

        if (becauseFactionDead) return

        if (repOnRemove != null) {
            val ourFac = Global.getSector().getFaction(factionId)

            if (IAIIC != null && ourFac != null) {
                ourFac.setRelationship(niko_MPC_ids.IAIIC_FAC_ID, repOnRemove * 0.01f)
            }
        }
    }

    fun remove(dialog: InteractionDialogAPI?, becauseFactionDead: Boolean) {
        val intel = MPC_IAIICFobIntel.get() ?: return
        intel.removeContribution(this, becauseFactionDead, dialog)
    }

    fun getStringifiedFleetsize(): String {
        return "${(fleetMultIncrement * 100).toInt()}%"
    }

    fun getStringifiedSabotagePotential(): String {
        return "${(sabotageMultIncrement * 100).toInt()}%"
    }

    class benefactorData(
        val id: String,
        var name: String = Global.getSector().getFaction(id).displayName,
        val color: Color = Global.getSector().getFaction(id).baseUIColor,
        var suffix: String? = null
    ) {
        fun addBullet(info: TooltipMakerAPI) {
            var base = name
            if (suffix != null) base += " (${suffix})"

            info.addPara(base, color, 0f)
        }
    }
}