package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import data.scripts.campaign.listeners.niko_MPC_saveListener
import data.utilities.niko_MPC_debugUtils
// listener because an EFS is stupid expensive with setvariant, and this SEEMS to work, so until it breaks, we use this
class MPC_variantFixerListener(
    val fleet: CampaignFleetAPI
): niko_MPC_saveListener {

    val variants: MutableMap<FleetMemberAPI, ShipVariantAPI> = HashMap()

    fun begin() {
        Global.getSector().listenerManager.addListener(this)
        refreshVariants()
    }

    private fun refreshVariants(nuke: Boolean = false) {
        if (nuke) {
            variants.clear()
        }
        for (member in fleet.fleetData.membersListCopy) {
            if (nuke || variants[member] == null) {
                variants[member] = member.variant.clone()
            }
        }
    }

    fun stop() {
        Global.getSector().listenerManager.removeListener(this)
        variants.clear()
    }

    override fun afterGameSave() {
        fixVariants()
    }

    override fun onGameSaveFailed() {
        fixVariants()
    }

    override fun onGameLoad() {
        fixVariants()
    }

    private fun fixVariants() {
        if (fleet.isExpired || fleet.fleetData.membersListCopy.isEmpty()) {
            stop()
            return
        }

        for (member in fleet.fleetData.membersListCopy) {
            var variant = variants[member]
            if (variant == null) {
                refreshVariants()
                variant = variants[member]
                if (variant == null) {
                    niko_MPC_debugUtils.displayError("failed to update variant during variant fixer, fleet name: ${fleet.name}")
                    stop()
                    return
                }
            }
            if (variant == member.variant) continue // actually a bit of an expensive operation
            member.setVariant(variants[member], false, true)
        }
    }

    override fun beforeGameSave() {
        return
    }
}
