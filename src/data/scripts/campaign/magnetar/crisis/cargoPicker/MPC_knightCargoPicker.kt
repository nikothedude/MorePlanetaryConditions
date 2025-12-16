package data.scripts.campaign.magnetar.crisis.cargoPicker

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.ui.w
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_omegaWeaponPicker.Companion.OP_TO_POINTS_MULT
import kotlin.math.ceil
import kotlin.math.roundToInt

class MPC_knightCargoPicker(dialog: InteractionDialogAPI, val memoryMap: MutableMap<String, MemoryAPI>?) : MPC_CargoPickerListener(dialog) {
    override val title: String = "Select technology to turn in"

    companion object {
        const val OMEGA_OP_TO_POINTS_MULT = 0.2f
        const val THREAT_OP_TO_POINTS_MULT = 0.1f
        const val SPECIAL_ITEM_POINTS = 35f
        const val POINTS_NEEDED = 100f

        val ACCEPTED_THREAT_WPNS = listOf(
            "voidblaster",
            "swarm_launcher",
            "seeker_fragment",
            "devouring_swarm",
            "voltaic_discharge",
        )
    }

    override fun getAvailableCargo(playerCargo: CargoAPI): CargoAPI {
        val copy = Global.getFactory().createCargo(false)
        for (stack in playerCargo.stacksCopy) {
            val spec = stack.weaponSpecIfWeapon
            if (spec != null) {
                if (spec.hasTag(Tags.OMEGA) && !spec.hasTag("sw_techmining") /*superweapons blacklist*/) {
                    copy.addFromStack(stack)
                }
                if (spec.weaponId in ACCEPTED_THREAT_WPNS) {
                    copy.addFromStack(stack)
                }
            }

            val specialSpec = stack.specialItemSpecIfSpecial
            if (specialSpec != null) {
                if (specialSpec.id != Items.CORRUPTED_NANOFORGE && specialSpec.id != Items.PRISTINE_NANOFORGE)
                copy.addFromStack(stack)
            }
        }
        return copy
    }

    override fun removeItem(stack: CargoStackAPI, cargo: CargoAPI) {
        super.removeItem(stack, cargo)
        val satisifed = getPointsSatisfied(stack)
        sanitizeMemory()
        val curr = Global.getSector().memoryWithoutUpdate.getFloat("\$MPC_IAIICChurchKnightPoints")
        Global.getSector().memoryWithoutUpdate["\$MPC_IAIICChurchKnightPoints"] = curr + satisifed
    }

    override fun postPick() {
        FireBest.fire(null, dialog, memoryMap, "MPC_IAIICCHKnghtItmsTrnedIn")
    }

    fun sanitizeMemory() {
        if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICChurchKnightPoints"] == null) {
            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICChurchKnightPoints"] = 0
        }
    }

    private fun getPointsSatisfied(cargo: CargoStackAPI?): Int {
        if (cargo == null) return 0
        var points = 0
        val size = cargo.size

        val wpnSpec = cargo.weaponSpecIfWeapon
        if (wpnSpec != null) {
            if (wpnSpec.hasTag(Tags.OMEGA)) {
                return ceil((wpnSpec.getOrdnancePointCost(null) * OMEGA_OP_TO_POINTS_MULT) * size).roundToInt()
            }
            else if (wpnSpec.weaponId in ACCEPTED_THREAT_WPNS) {
                return  ceil((wpnSpec.getOrdnancePointCost(null) * THREAT_OP_TO_POINTS_MULT) * size).roundToInt()
            }
        }

        val specialSpec = cargo.specialItemSpecIfSpecial
        if (specialSpec != null) {
            return SPECIAL_ITEM_POINTS.toInt()
        }

        return points
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
        val satisfied = Global.getSector().memoryWithoutUpdate.getFloat("\$MPC_IAIICChurchKnightPoints")
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
            "If you turn in the selected items, %s points will be satisfied. If a weapon, points are based off its OP cost.", 10f,
            Misc.getHighlightColor(), "$selectedPoints"
        )

    }

    override fun getFaction(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH)
    }

    override fun cancelledCargoSelection() {

    }
}