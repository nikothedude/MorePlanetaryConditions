package data.scripts.campaign.econ.industries.overgrownJunk

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import data.scripts.campaign.econ.conditions.overgrownNanoforge.niko_MPC_overgrownNanoforge
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_miscUtils.canBeFarmed
import data.utilities.niko_MPC_miscUtils.canBeMined
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.validOvergrownNanoforgeCommoditiesToData
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

class niko_MPC_overgrownNanoforgeCommodityData(
    val market: MarketAPI,
    val supply: MutableMap<String, Int>,
    val demand: MutableMap<String, Int>
    ) {

    init {
        doInitialCommodityUpdate()
    }

    fun doInitialCommodityUpdate() {
        val theme = getIndustry()?.theme ?: return
        setupTheme(theme)

        var budget = MathUtils.getRandomNumberInRange(niko_MPC_settings.MIN_OVERGROWN_NANOFORGE_BUDGET, niko_MPC_settings.MAX_OVERGROWN_NANOFORGE_BUDGET)
        var limit = MathUtils.getRandomNumberInRange(niko_MPC_settings.OVERGROWN_NANOFORGE_MINOR_THEMES_MIN, niko_MPC_settings.OVERGROWN_NANOFORGE_MINOR_THEMES_LIMIT)

        val commoditiesToPickFrom = ArrayList(validOvergrownNanoforgeCommoditiesToData.keys)
        if (!market.canBeFarmed()) commoditiesToPickFrom -= Commodities.FOOD
        if (!market.canBeMined()) {
            commoditiesToPickFrom -= Commodities.ORE
            commoditiesToPickFrom -= Commodities.RARE_ORE
            commoditiesToPickFrom -= Commodities.VOLATILES
            commoditiesToPickFrom -= Commodities.ORGANICS
        }
        commoditiesToPickFrom.shuffle()
        val chosenCommoditites = HashMap<String, Float>()
        for (entry in commoditiesToPickFrom) {
            limit--
            chosenCommoditites[entry] = 0f
            if (limit <= 0) break
        }

        var divisor = chosenCommoditites.size
        for (commodity in chosenCommoditites.keys) {
            val cost = validOvergrownNanoforgeCommoditiesToData[commodity]!!.cost
            val min = cost
            val max = budget/(divisor/4)
            val providedBudget = (MathUtils.getRandomNumberInRange(min, max))
            chosenCommoditites[commodity] = providedBudget

            budget -= providedBudget
            divisor--

            val amountOfCommodity = (providedBudget / cost).roundToInt()
            supply[commodity] = amountOfCommodity

            val demandFromData = validOvergrownNanoforgeCommoditiesToData[commodity]!!.demandPerSupply
            for (entry in demandFromData.entries) {
                val demandCommodityId = entry.key
                val amountPerSupply = entry.value

                val projectedDemand = (amountOfCommodity/amountPerSupply).roundToInt()
                if (demand[demandCommodityId] == null) demand[demandCommodityId] = 0
                if (demand[demandCommodityId]!! < projectedDemand) demand[demandCommodityId] = projectedDemand
            }
        }
    }

    fun setupTheme(theme: String) {
        when (theme) {
            Industries.HEAVYINDUSTRY -> {

            }
         }
    }

    fun updateCommodityValues() {
        TODO("Not yet implemented")
    }

    fun getCondition(): niko_MPC_overgrownNanoforge? {
        val condition = market.getCondition(niko_MPC_ids.overgrownNanoforgeConditionId)?.plugin
        if (condition !is niko_MPC_overgrownNanoforge) {
            handleNullCondition()
            return null
        }
        return condition
    }

    fun getIndustry(): niko_MPC_overgrownNanoforgeIndustry? {
        val industry = market.getIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId)
        if (industry !is niko_MPC_overgrownNanoforgeIndustry) {
            handleNullIndustry()
            return null
        }
        return industry
    }

    fun handleNullIndustry() {
        delete()
    }

    fun handleNullCondition() {
        delete()
    }

    fun delete() {
        market.memoryWithoutUpdate[niko_MPC_ids.overgrownNanoforgeCommoditiesId] = null
    }
}