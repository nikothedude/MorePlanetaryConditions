package data.scripts.campaign.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventType
import indevo.items.consumables.itemPlugins.SpooferConsumableItemPlugin
import org.lwjgl.input.Keyboard

class MPC_missileStrikeKeypressListener: CampaignInputListener {

    override fun getListenerInputPriority(): Int {
        return 0
    }

    override fun processCampaignInputPreCore(events: MutableList<InputEventAPI>) {
        val ui = Global.getSector().campaignUI
        if (ui.currentInteractionDialog == null) {
            for (input in events) {
                if (!input.isConsumed && input.eventType == InputEventType.KEY_DOWN) {
                    if (input.eventValue == Keyboard.KEY_RIGHT) {
                        MPC_missileStrikeAbility.playerNextMissile()
                    }

                    if (input.eventValue == Keyboard.KEY_LEFT) {
                        MPC_missileStrikeAbility.playerPrevMissile()
                    }
                }
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: MutableList<InputEventAPI?>?) {
        return
    }

    override fun processCampaignInputPostCore(events: MutableList<InputEventAPI?>?) {
        return
    }

}