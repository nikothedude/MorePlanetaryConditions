package data.scripts.campaign.magnetar.crisis.cargoPicker

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.AICores
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_knightCargoPicker.Companion.ACCEPTED_THREAT_WPNS
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_knightCargoPicker.Companion.OMEGA_OP_TO_POINTS_MULT
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_knightCargoPicker.Companion.POINTS_NEEDED
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_knightCargoPicker.Companion.SPECIAL_ITEM_POINTS
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_knightCargoPicker.Companion.THREAT_OP_TO_POINTS_MULT
import kotlin.math.ceil
import kotlin.math.roundToInt

class MPC_patherAICorePicker(dialog: InteractionDialogAPI, val memoryMap: MutableMap<String, MemoryAPI>?) : MPC_CargoPickerListener(dialog) {

    companion object {
        const val COST_TO_POINTS_MULT = 0.01f
        const val POINTS_NEEDED = 100f
    }

    override val title: String = "Select AI cores to cosign to eternal oblivion"

    override fun getAvailableCargo(playerCargo: CargoAPI): CargoAPI {
        val copy = Global.getFactory().createCargo(false)
        for (stack in playerCargo.stacksCopy) {
            val spec = stack.resourceIfResource
            if (spec != null && spec.demandClass == "ai_cores") {
                copy.addFromStack(stack)
            }
        }
        return copy
    }

    override fun addMiddleDesc(
        panel: TooltipMakerAPI,
        cargo: CargoAPI,
        pickedUp: CargoStackAPI?,
        pickedUpFromSource: Boolean,
        combined: CargoAPI?
    ) {
        sanitizeMemory()
        val selectedPoints = getPointsSatisfied(pickedUp)
        val satisfied = Global.getSector().memoryWithoutUpdate.getFloat("\$MPC_IAIICCHTorturePatherAIPoints")
        val pointsNeeded = POINTS_NEEDED
        val label = panel.addPara(
            "Points needed: %s/%s",
            10f,
            Misc.getHighlightColor(),
            "$satisfied", "$pointsNeeded"
        )
        label.setHighlightColors(
            (if (satisfied >= pointsNeeded) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()),
            Misc.getHighlightColor()
        )

        panel.addPara(
            "If you turn in the selected items, %s points will be satisfied.", 10f,
            Misc.getHighlightColor(), "$selectedPoints"
        )
    }

    override fun postPick() {
        FireBest.fire(null, dialog, memoryMap, "MPC_IAIICCHTrtrPthrAICoresPostPick")
    }

    fun sanitizeMemory() {
        if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICCHTorturePatherAIPoints"] == null) {
            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICCHTorturePatherAIPoints"] = 0
        }
    }

    private fun getPointsSatisfied(cargo: CargoStackAPI?): Int {
        if (cargo == null) return 0

        val spec = cargo.resourceIfResource
        if (spec != null) {
            return ((spec.basePrice * COST_TO_POINTS_MULT) * cargo.size).roundToInt()
        }

        AICores()

        return 0
    }

    override fun getFaction(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_PATH)
    }

    override fun cancelledCargoSelection() {

    }

}