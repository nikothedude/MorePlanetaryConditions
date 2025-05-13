package data.scripts.campaign.magnetar.crisis.cargoPicker

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

abstract class MPC_CargoPickerListener(
    val dialog: InteractionDialogAPI
): CargoPickerListener {

    abstract val title: String
    open val confirmText: String = "Confirm"
    open val cancelText: String = "Cancel"
    var width: Float = 0f

    companion object {
        fun addItemLossText(cargo: CargoAPI, stack: CargoStackAPI, q: Int, text: TextPanelAPI) {
            val type = stack.type
            val id = stack.commodityId

            var data: SpecialItemData? = null
            if (type == CargoItemType.SPECIAL) {
                data = stack.specialDataIfSpecial
            }

            if (type == CargoItemType.RESOURCES) {
                if (q > 0) {
                    AddRemoveCommodity.addCommodityGainText(id, q, text)
                } else if (q < 0) {
                    AddRemoveCommodity.addCommodityLossText(id, -q, text)
                }
            }
            if (type == CargoItemType.FIGHTER_CHIP) {
                if (q > 0) {
                    AddRemoveCommodity.addFighterGainText(id, q, text)
                } else if (q < 0) {
                    AddRemoveCommodity.addFighterLossText(id, -q, text)
                }
            }
            if (type == CargoItemType.WEAPONS) {
                if (q > 0) {
                    AddRemoveCommodity.addWeaponGainText(id, q, text)
                } else if (q < 0) {
                    AddRemoveCommodity.addWeaponLossText(id, -q, text)
                }
            }
            if (type == CargoItemType.SPECIAL) {
                if (q > 0) {
                    AddRemoveCommodity.addItemGainText(data, q, text)
                } else if (q < 0) {
                    AddRemoveCommodity.addItemLossText(data, -q, text)
                }
            }
        }
    }

    abstract fun getAvailableCargo(playerCargo: CargoAPI): CargoAPI

    override fun pickedCargo(cargo: CargoAPI?) {
        if (cargo == null) return
        val playerCargo = Global.getSector().playerFleet.cargo

        if (cargo.isEmpty) {
            cancelledCargoSelection()
            return
        }
        cargo.sort()
        for (stack in cargo.stacksCopy) {
            if (stack == null) continue
            removeItem(stack, playerCargo)
        }

        postPick()
    }

    protected open fun postPick() {}

    open fun removeItem(stack: CargoStackAPI, cargo: CargoAPI) {
        addItemLossText(cargo, stack, stack.size.toInt(), dialog.textPanel)
        cargo.removeItems(stack.type, stack.data, stack.size)
    }

    override fun recreateTextPanel(
        panel: TooltipMakerAPI?,
        cargo: CargoAPI?,
        pickedUp: CargoStackAPI?,
        pickedUpFromSource: Boolean,
        combined: CargoAPI?
    ) {
        if (panel == null || cargo == null) return

        val faction = getFaction()
        panel.setParaFontOrbitron()
        panel.addPara(Misc.ucFirst(faction.displayName), faction.baseUIColor, 1f)
        panel.setParaFontDefault()

        panel.addImage(faction.crest, width * 1f, 3f)

        addMiddleDesc(panel, cargo, pickedUp, pickedUpFromSource, combined)
    }

    abstract fun addMiddleDesc(
        panel: TooltipMakerAPI,
        cargo: CargoAPI,
        pickedUp: CargoStackAPI?,
        pickedUpFromSource: Boolean,
        combined: CargoAPI?
    )

    abstract fun getFaction(): FactionAPI
}

