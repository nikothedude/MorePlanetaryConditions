package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.MPC_magnetarCalibrator
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_ids

class MPC_kantaMissionBegin: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        val playerFleet = Global.getSector().playerFleet

        val itemData = WormholeManager.WormholeItemData("MPC_magnetarWormhole", "MPC_Tango", "Tango")
        val item = SpecialItemData(Items.WORMHOLE_ANCHOR, itemData.toJsonStr())
        playerFleet.cargo.addSpecial(item, 1f)

        val magnetarIntel = Global.getSector().intelManager.getFirstIntel(MPC_magnetarQuest::class.java) as? MPC_magnetarQuest ?: return false
        magnetarIntel.stage = MPC_magnetarQuest.Stage.FIND_SYSTEM
        magnetarIntel.sendUpdate(magnetarIntel.stage, dialog.textPanel);
        MPC_magnetarCalibrator().start()

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.KANTA_MAGNETAR_QUEST_STARTED] = true
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.KANTA_EXPECTING_PLAYER] = false

        return true
    }
}