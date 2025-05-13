package data.scripts.campaign.magnetar.crisis.cargoPicker

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.lang.Math.ceil
import kotlin.math.roundToInt

abstract class MPC_omegaWeaponPicker(
    dialog: InteractionDialogAPI,
    val pointsNeeded: Int
): MPC_CargoPickerListener(dialog) {
    companion object {
        const val OP_TO_POINTS_MULT = 0.1f
    }

    override val title: String = "Select exotic weaponry to turn in"

    override fun getAvailableCargo(playerCargo: CargoAPI): CargoAPI {
        val copy = Global.getFactory().createCargo(false)
        for (stack in playerCargo.stacksCopy) {
            val spec = stack.weaponSpecIfWeapon ?: continue
            if (spec.hasTag(Tags.OMEGA) && !spec.hasTag("sw_techmining") /*superweapons blacklist*/) {
                copy.addFromStack(stack)
            }
        }
        return copy
    }

    override fun cancelledCargoSelection() {

    }

    override fun addMiddleDesc(
        panel: TooltipMakerAPI,
        cargo: CargoAPI,
        pickedUp: CargoStackAPI?,
        pickedUpFromSource: Boolean,
        combined: CargoAPI?
    ) {
        val selectedPoints = getPoints(combined)
        val satisfied = getPointsSatisfied() + selectedPoints
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
            "If you turn in the selected weapons, %s points will be satisfied. Points are based off the ordnance point cost of the weapon.", 10f,
            Misc.getHighlightColor(), "$selectedPoints"
        )
    }

    private fun getPoints(cargo: CargoAPI?): Int {
        if (cargo == null) return 0
        var points = 0
        for (stack in cargo.stacksCopy) {
            val spec = stack.weaponSpecIfWeapon ?: continue
            points += getPointsForWeapon(spec) * stack.size.toInt()
        }
        return points
    }

    override fun removeItem(stack: CargoStackAPI, cargo: CargoAPI) {
        super.removeItem(stack, cargo)
        val spec = stack.weaponSpecIfWeapon ?: return
        satisfyPoints(getPointsForWeapon(spec) * stack.size.toInt())
    }

    abstract fun satisfyPoints(pointsForWeapon: Int)
    abstract fun getPointsSatisfied(): Int

    fun getPointsForWeapon(spec: WeaponSpecAPI): Int {
        return kotlin.math.ceil((spec.getOrdnancePointCost(null) * OP_TO_POINTS_MULT)).roundToInt()
    }
}