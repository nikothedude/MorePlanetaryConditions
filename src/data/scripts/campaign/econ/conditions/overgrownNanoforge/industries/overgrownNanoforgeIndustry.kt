package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeIndustrySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeJunkSpreader
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.hasJunkStructures
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeIndustry: baseOvergrownNanoforgeStructure() {

    var baseSource: overgrownNanoforgeIndustrySource? = generateBaseStats()
        set(value) {
            if (field != null) field!!.delete()
            field = value
        }
    val sources: MutableList<overgrownNanoforgeEffectSource> = ArrayList()
    val junk: MutableSet<overgrownNanoforgeJunk> = HashSet()
    var junkSpreader: overgrownNanoforgeJunkSpreader = overgrownNanoforgeJunkSpreader(this)

    override fun init(id: String?, market: MarketAPI?) {
        super.init(id, market)

        generateBaseStats()
        junkSpreader = overgrownNanoforgeJunkSpreader(this)
    }

    private fun generateBaseStats(): overgrownNanoforgeIndustrySource {
        val remainingScore = getBaseScore()
        val supplyEffect = overgrownNanoforgeEffectPrototypes.ALTER_SUPPLY.getInstance(this, remainingScore.toFloat())
            ?: return overgrownNanoforgeIndustrySource(this, //shuld never happen
                mutableSetOf(overgrownNanoforgeAlterSupplySource(this, hashMapOf(Pair(Commodities.ORGANS, 500)))))

        val source = overgrownNanoforgeIndustrySource(this, mutableSetOf(supplyEffect))
        return source
    }

    //use this one
    override fun apply(withIncomeUpdate: Boolean) {
        super.apply(withIncomeUpdate)

        applyConditions()
        for (source in getAllSources()) source.apply()
    }

    private fun applyConditions() {
        if (market.hasCondition(Conditions.HABITABLE)) {
            market.addCondition(Conditions.POLLUTION)
        }
    }

    override fun apply() {
        apply(true)
    }

    override fun unapply() {
        super.unapply()
        if (!reapplying) {
            delete()
        } else {
            for (source in getAllSources()) source.unapply()
        }
    }

    fun getAllSources(): List<overgrownNanoforgeEffectSource> {
        if (baseSource != null) return sources + baseSource!!
        return sources
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val spreader = junkSpreader ?: return
        spreader.spreadJunkIfPossible(amount)
    }

    override fun delete() {
        super.delete()
        for (source in getAllSources()) source.delete()
        TODO()
    }

    override fun canBeDestroyed(): Boolean {
        if (playerNotNearAndDoWeCare()) return false
        if (market.hasJunkStructures()) return false

        return true
    }

    override fun reportDestroyed() {
        super.reportDestroyed()
        val overgrownNanoforgeData = SpecialItemData(niko_MPC_ids.overgrownNanoforgeItemId, null)
        Misc.getStorage(market).cargo.addSpecial(overgrownNanoforgeData, 1f)
        TODO("this will not work")
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
        fun getBaseScore(): Int {
            return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_BASE_SCORE_MIN, OVERGROWN_NANOFORGE_BASE_SCORE_MAX)
        }
    }
}
