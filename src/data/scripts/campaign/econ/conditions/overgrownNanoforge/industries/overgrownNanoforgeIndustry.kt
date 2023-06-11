package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider.Companion.OVERGROWN_NANOFORGE_INDUSTRY_OPEN_INTEL_OPTION_ID
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeCondition
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MIN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_INDUSTRY_NAME
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_IS_INDUSTRY
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeIndustry: baseOvergrownNanoforgeStructure() {

    override fun unapply() {
        super.unapply()
        val condition = market.getOvergrownNanoforgeCondition()
        condition?.startDeletionScript(market)
    }

    fun getAllSources(): Set<overgrownNanoforgeEffectSource> {
        return getSources() + getBaseSource()
    }

    fun getBaseSource(): overgrownNanoforgeEffectSource {
        return getHandlerWithUpdate().baseSource
    }

    fun getSources(): MutableSet<overgrownNanoforgeEffectSource> {
        return getHandlerWithUpdate().getJunkSources()
    }

    override fun getHandlerWithUpdate(): overgrownNanoforgeIndustryHandler {
        return super.getHandlerWithUpdate() as overgrownNanoforgeIndustryHandler
    }
    override fun getHandler(): overgrownNanoforgeIndustryHandler? {
        return market.getOvergrownNanoforgeIndustryHandler()
    }

    override fun instantiateNewHandler(): overgrownNanoforgeIndustryHandler {
        return super.instantiateNewHandler() as overgrownNanoforgeIndustryHandler
    }

    override fun createNewHandlerInstance(): overgrownNanoforgeIndustryHandler {
        return overgrownNanoforgeIndustryHandler(market)
    }

    override fun canInstallAICores(): Boolean {
        return false
    }

    override fun getVisibleInstalledItems(): MutableList<SpecialItemData> {
        return super.getVisibleInstalledItems()
    }

    override fun isIndustry(): Boolean {
        return OVERGROWN_NANOFORGE_IS_INDUSTRY
    }

    companion object {
        fun getBaseScore(): Float {
            return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_BASE_SCORE_MIN, OVERGROWN_NANOFORGE_BASE_SCORE_MAX)
        }
    }

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean {
        return false
    }

    override fun getCurrentName(): String {
        return OVERGROWN_NANOFORGE_INDUSTRY_NAME
    }

    override fun isTooltipExpandable(): Boolean {
        return true
    }

    override fun createTooltip(mode: Industry.IndustryTooltipMode?, tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(mode, tooltip, expanded)

        if (tooltip == null) return
        if (expanded) {
            tooltip.addPara("Before the collapse, the Domain kept tabs on all Nanoforges. Being a dangerous (and lucrative) technology, they kept a Domain engineer on site of each Nanoforge, along with (either officially or unofficially) a small projection of force to ensure it's proper use, dictated by them. Each Nanoforge would only produce exactly what the owner (often the government of a caste-world) is supposed to output, and exactly what they need to produce it. Any other usage would have been a slippery slope to a grey goo scenario, as claimed by the Domain COMSEC.\n" +
                    "\n" +
                    "Why this particular one seems to have fulfilled that prophecy is up for debate. Maybe the Domain was right, and it was used improperly, to fill more roles than intended, creating food for a foundry world? Maybe it was the on-hand engineer - desperate for a foothold in the new world, using their knowhow to disable the growth safeties in a way that bypasses the thousands of DRM protections? Or was it done with full knowledge of the consequences in a clear head, done for the sole purpose of destruction? One thing's for sure - This was no accident.\n" +
                    "\n" +
                    "The fact it's still running with nobody around most certainly is, though, as only someone completely insane or mind-bogglingly destructive would willingly induce a grey goo scenario on a planet. Right?",
            5f)
        }
    }
}
