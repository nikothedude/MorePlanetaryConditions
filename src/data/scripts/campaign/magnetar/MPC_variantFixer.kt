package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_debugUtils

class MPC_variantFixer(
    val fleet: CampaignFleetAPI
): niko_MPC_baseNikoScript() {

    val variants: MutableMap<FleetMemberAPI, ShipVariantAPI> = HashMap()

    override fun startImpl() {
        refreshVariants()
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        variants.clear()
        Global.getSector().removeScript(this)
    }


    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        if (fleet.isExpired) {
            delete()
            return
        }
        for (member in fleet.fleetData.membersListCopy) {
            var variant = variants[member]
            if (variant == null) {
                refreshVariants()
                variant = variants[member]
                if (variant == null) {
                    niko_MPC_debugUtils.displayError("failed to update variant during variant fixer, fleet name: ${fleet.name}")
                    delete()
                    return
                }
            }
            member.setVariant(variants[member], false, true)
        }
    }

    private fun refreshVariants() {
        variants.clear()
        for (member in fleet.fleetData.membersListCopy) {
            variants[member] = member.variant.clone()
        }
    }
}