package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class MPC_toggleableAutomation: BaseHullMod() {

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)

        if (stats == null || id == null) return

        val variant = stats.variant
        val enabled = id == "MPC_toggleAutomationEnabled"

        if (enabled && !variant.hasHullMod(HullMods.AUTOMATED)) {
            variant.addPermaMod(HullMods.AUTOMATED)
            variant.addTag(Tags.AUTOMATED)
            stats.fleetMember?.isFlagship = false
            //stats.variant.addTag("no_auto_penalty")
            stats.fleetMember?.captain = null
        } else if (!enabled && variant.hasHullMod(HullMods.AUTOMATED)) {
            variant.removePermaMod(HullMods.AUTOMATED)
            variant.removeTag(Tags.AUTOMATED)
            stats.fleetMember?.captain = null
            stats.fleetMember?.isFlagship = false
        }

        // sanity
        if (stats.fleetMember?.captain != null && !stats.fleetMember.captain.isDefault) {
            if (enabled) {
                if (!stats.fleetMember.captain.isAICore) {
                    stats.fleetMember?.captain = null
                    stats.fleetMember.isFlagship = false
                }
            } else if (stats.fleetMember.captain.isAICore) {
                stats.fleetMember?.captain = null
                stats.fleetMember.isFlagship = false
            }
        }
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)

        if (tooltip == null || ship == null) return

        val enabled = ship.variant.hasHullMod("MPC_toggleAutomationEnabled")
        val ourString = if (enabled) "Enabled" else "Disabled"
        val ourColor = if (enabled) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
        val enableOrDisable = if (enabled) "disable" else "enable"
        val enableOrDisableColor = if (enabled) Misc.getNegativeHighlightColor() else Misc.getPositiveHighlightColor()

        tooltip.addPara(
            "%s this hullmod to %s automation.",
            10f,
            Misc.getHighlightColor(),
            "Remove", enableOrDisable
        ).setHighlightColors(
            Misc.getHighlightColor(), enableOrDisableColor
        )

        tooltip.addPara(
            "Current status: %s",
            10f,
            ourColor,
            ourString
        )
    }
}