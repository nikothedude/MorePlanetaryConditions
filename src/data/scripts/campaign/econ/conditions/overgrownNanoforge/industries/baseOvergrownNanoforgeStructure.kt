package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.comms.v2.EventsPanel
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.industries.baseNikoIndustry
import data.utilities.niko_MPC_marketUtils.hasJunkStructures
import data.utilities.niko_MPC_marketUtils.isValidTargetForOvergrownHandler
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

abstract class baseOvergrownNanoforgeStructure: baseNikoIndustry(), hasDeletionScript<overgrownStructureDeletionScript> {

    override var deletionScript: overgrownStructureDeletionScript? = null

    override fun canShutDown(): Boolean = false // You must go through [getHandler]'s [manipulationIntel].

    override fun apply() {
        apply(true)
    }

    override fun apply(withIncomeUpdate: Boolean) {
        super.apply(withIncomeUpdate)

        val handler = getHandlerWithUpdate()
        handler?.apply()
    }
    override fun unapply() {
        super.unapply()
        startDeletionScript()
    }

    override fun delete() {
        super.delete()
        getHandler()?.unapply()
        getHandler()?.currentStructureId = null
    }

    override fun canUpgrade(): Boolean = false

    override fun getCanNotShutDownReason(): String = "It is not a simple process to remove a structure such as this."

    /*open fun canBeDestroyed(): Boolean {
        if (playerNotNearAndDoWeCare()) return false
        if (market.hasJunkStructures()) return false

        return true
    }

    

    fun playerNotNearAndDoWeCare(): Boolean {
        if (!niko_MPC_settings.OVERGROWN_NANOFORGE_CARES_ABOUT_PLAYER_PROXIMITY_FOR_DECON) return false

        val playerFleet = Global.getSector().playerFleet ?: return true
        val marketContainingLocation = market.containingLocation ?: return false
        val playerContainingLocation = playerFleet.containingLocation ?: return true
        if (marketContainingLocation != playerContainingLocation) return true

        val playerLocation = playerFleet.location ?: return true
        val marketLocation = market.primaryEntity?.location ?: market.location ?: return false
        val distance = MathUtils.getDistance(marketLocation, playerLocation)
        if (distance > niko_MPC_settings.OVERGROWN_NANOFORGE_INTERACTION_DISTANCE) return true
        return false
    } 
    
    override fun upgradeFinished(previous: Industry?) {
        super.upgradeFinished(previous)
        reportDestroyed()
    }

    open fun reportDestroyed() {
        delete()
    }

    override fun sendBuildOrUpgradeMessage() {
        if (market.isPlayerOwned) {
            val intel = MessageIntel(currentName + " at " + market.name, Misc.getBasePlayerColor())
            intel.addLine(BaseIntelPlugin.BULLET + "dasfxgjjy") // dont think this is used
            intel.icon = Global.getSector().playerFaction.crest
            intel.sound = BaseIntelPlugin.getSoundStandardUpdate()
            Global.getSector().campaignUI.addMessage(intel, MessageClickAction.COLONY_INFO, market)
        }
    }

    */ unused

    override fun createDeletionScriptInstance(vararg args: Any): overgrownStructureDeletionScript {
        return overgrownStructureDeletionScript(this, this, market)
    }

    open fun getHandlerWithUpdate(): overgrownNanoforgeHandler? {
        updateHandler()
        return getHandler()
    }

    fun updateHandler() {
        if (getHandler() == null && market.isValidTargetForOvergrownHandler()) {
            instantiateNewHandler()
        }
    }

    open fun instantiateNewHandler(): overgrownNanoforgeHandler {
        val newHandler = createNewHandlerInstance()
        newHandler.init()
        return newHandler
    }

    override fun createTooltip(mode: Industry.IndustryTooltipMode?, tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(mode, tooltip, expanded)
        if (mode == null || mode != Industry.IndustryTooltipMode.NORMAL) return
        if (tooltip == null) return

        val handler = getHandlerWithUpdate() ?: return
        val positives = handler.getFormattedPositives()
        val negatives = handler.getFormattedNegatives()

        if (positives.isNotEmpty()) {
            tooltip.addSpacer(5f)
            tooltip.addSectionHeading("Positives", Alignment.MID, 0f)
            tooltip.addPara(positives, 5f)
        }
        if (negatives.isNotEmpty()) {
            tooltip.addSectionHeading("Negatives", Alignment.MID, 5f)
            tooltip.addPara(negatives, 5f)
        }
    }

    abstract fun createNewHandlerInstance(): overgrownNanoforgeHandler

    abstract fun getHandler(): overgrownNanoforgeHandler?
    fun getIntelOption(provider: overgrownNanoforgeOptionsProvider): IndustryOptionProvider.IndustryOptionData? {
        val option = createIntelOption(provider) ?: return null
        modifyIntelOption(option)
        return option
    }

    open fun modifyIntelOption(option: IndustryOptionProvider.IndustryOptionData) {
    }

    open fun createIntelOption(provider: overgrownNanoforgeOptionsProvider): IndustryOptionProvider.IndustryOptionData? {
        if (getHandlerWithUpdate()?.manipulationIntel == null) return null
        return IndustryOptionProvider.IndustryOptionData(
            "Open Intel",
            overgrownNanoforgeOptionsProvider.OVERGROWN_NANOFORGE_INDUSTRY_OPEN_INTEL_OPTION_ID,
            this,
            provider
        )
    }
    fun openIntel() {
        val handler = getHandlerWithUpdate() ?: return
        val intel = handler.manipulationIntel ?: return
        Global.getSector().campaignUI.showCoreUITab(CoreUITabId.INTEL, intel)
       // Global.getSector().campaignUI.showCoreUITab(CoreUITabId.INTEL, intel)
       //TODO: nonfunctional
    }
}
