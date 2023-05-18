package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider.Companion.OVERGROWN_NANOFORGE_INDUSTRY_DECONSTRUCTION_ID
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeCondition
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MIN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_IS_INDUSTRY
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeIndustry: baseOvergrownNanoforgeStructure() {

    override fun unapply() {
        super.unapply()
        val condition = market.getOvergrownNanoforgeCondition()
        if (condition == null) {
            delete()
            return
        }
        condition.startDeletionScript(market)
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

    override fun createDestructionOption(provider: overgrownNanoforgeOptionsProvider): IndustryOptionProvider.IndustryOptionData {
        return IndustryOptionProvider.IndustryOptionData(
            "Deconstruct",
            OVERGROWN_NANOFORGE_INDUSTRY_DECONSTRUCTION_ID,
            this,
            provider
        )
    }

    override fun instantiateNewHandler(): overgrownNanoforgeIndustryHandler {
        return super.instantiateNewHandler() as overgrownNanoforgeIndustryHandler
    }

    override fun createNewHandlerInstance(): overgrownNanoforgeIndustryHandler {
        return overgrownNanoforgeIndustryHandler(market)
    }

    override fun reportDestroyed() {
        val overgrownNanoforgeData = SpecialItemData(niko_MPC_ids.overgrownNanoforgeItemId, null)
        Misc.getStorage(market).cargo.addSpecial(overgrownNanoforgeData, 1f)
        super.reportDestroyed()
        //TODO("this will not work")
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
}
