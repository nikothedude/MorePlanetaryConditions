package data.scripts.campaign.abilities

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventMouseButton
import com.fs.starfarer.api.input.InputEventType
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import com.fs.state.AppDriver
import data.utilities.niko_MPC_dialogUtils.getChildrenCopy
import data.utilities.niko_MPC_reflectionUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Vector2f

class MPC_missileStrikeInputListener: CampaignInputListener, EveryFrameScript {

    companion object {
        fun get(): MPC_missileStrikeInputListener {
            var listener = Global.getSector().memoryWithoutUpdate["\$MPC_missileStrikeListener"] as? MPC_missileStrikeInputListener
            if (listener == null) {
                listener = MPC_missileStrikeInputListener()
                Global.getSector().memoryWithoutUpdate["\$MPC_missileStrikeListener"] = listener
            }
            return listener
        }
    }

    var active = false

    override fun getListenerInputPriority(): Int {
        return 0
    }

    override fun processCampaignInputPreCore(events: List<InputEventAPI?>?) {
        if (!active) return

        val ui = Global.getSector().campaignUI

        if (ui.currentInteractionDialog != null) {
            deactivate(false)
            return
        }

        for (input in events!!) {
            if (input == null || input.isConsumed) continue

            if (input.isKeyboardEvent && input.eventType == InputEventType.MOUSE_DOWN && input.eventValue != Keyboard.KEY_SPACE && input.eventValue != Keyboard.KEY_LSHIFT && input.eventValue != Keyboard.KEY_TAB) {
                deactivate(true)
                return
            }


            if (input.eventType == InputEventType.MOUSE_DOWN && input.eventValue == InputEventMouseButton.LEFT) {
                //input.consume() // always consume it so we dont move

                val engine = CampaignEngine.getInstance() ?: return
                var target: SectorEntityToken? = engine.mousedOverEntity
                val state = AppDriver.getInstance().currentState as? CampaignState ?: return
                val coreUI = state.core
                if (coreUI.currentTabId == CoreUITabId.MAP) {
                    val currFleet = lastMapFleet
                    if (currFleet != null) {
                        target = currFleet
                    }
                }

                if (target != null && plugin?.canTargetEntity(target) == true) {

                    input.consume()

                    if (plugin?.canFire() == true) {
                        plugin?.currTarget = target as CampaignFleetAPI?
                        plugin?.forceActivation()
                        plugin?.currTarget = null

                        Global.getSector().campaignUI.messageDisplay.addMessage(
                            "Missile away!",
                            Misc.getNegativeHighlightColor()
                        )
                    } else {
                        Global.getSector().campaignUI.messageDisplay.addMessage(
                            "Cannot fire missile",
                            Misc.getNegativeHighlightColor()
                        )
                    }

                    deactivate(false)
                } else {
                    Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f)
                }

                return
            }
        }
    }

    override fun processCampaignInputPreFleetControl(events: List<InputEventAPI?>?) {
        return
    }

    override fun processCampaignInputPostCore(events: List<InputEventAPI?>?) {
        return
    }

    var plugin: MPC_missileStrikeAbility? = null
    fun activate(plugin: MPC_missileStrikeAbility) {
        active = true
        this.plugin = plugin
        Global.getSector().listenerManager.addListener(this, true)
        Global.getSector().addTransientScript(this)
        Global.getSector().campaignUI.messageDisplay.addMessage(
            "Select a fleet for missile strike - open system map for long-ranged targeting (Press alt/ctrl/esc to abort)",
            "(Press alt/ctrl/esc to abort)",
            Misc.getNegativeHighlightColor()
        )
    }
    fun deactivate(withMessage: Boolean) {
        active = false
        plugin = null
        lastMapFleet = null
        if (withMessage) {
            Global.getSector().campaignUI.messageDisplay.addMessage(
                "Missile launch aborted",
                Misc.getNegativeHighlightColor()
            )
        }
        Global.getSector().removeTransientScript(this)
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun isDone(): Boolean {
        return !active
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    var lastMapFleet: CampaignFleetAPI? = null
    var strikes: Float = 0f
    override fun advance(amount: Float) {
        val engine = CampaignEngine.getInstance() ?: return
        val state = AppDriver.getInstance().currentState as? CampaignState ?: return
        val coreUI = state.core

        if (coreUI.currentTabId != CoreUITabId.MAP) {
            lastMapFleet = null
            return
        }

        var screenPanel = niko_MPC_reflectionUtils.get("screenPanel", state) as? UIPanelAPI ?: return
        val children = screenPanel.getChildrenCopy()
        val tooltip = children.find { it is StandardTooltipV2Expandable && it::class.java.isAnonymousClass && it::class.java.declaredFields.size == 2 } ?: return
        val thing = niko_MPC_reflectionUtils.getLastDeclaredField(tooltip) ?: return
        if (niko_MPC_reflectionUtils.hasVariableOfName("oÒ0000", thing)) { // coreUI.A.float
            val fleet = niko_MPC_reflectionUtils.get("oÒ0000", thing) as? CampaignFleetAPI

            if (fleet != null) {
                lastMapFleet = fleet
                strikes = 0f
            } else {
                strikes++
                if (strikes >= 3f) {
                    strikes = 0f
                    lastMapFleet = null
                }

            }
        }

    }
}