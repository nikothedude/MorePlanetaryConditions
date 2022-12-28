package data.scripts.campaign.econ.industries.overgrownJunk

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition
import com.fs.starfarer.api.impl.campaign.econ.impl.Farming
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData.overgrownNanoforgeTheme
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_miscUtils.canBeFarmed
import data.utilities.niko_MPC_miscUtils.canBeMined
import data.utilities.niko_MPC_miscUtils.getOvergrownNanoforgeCommodities
import data.utilities.niko_MPC_miscUtils.hasJunk
import data.utilities.niko_MPC_settings

class niko_MPC_overgrownNanoforgeIndustry: niko_MPC_baseOvergrownNanoforgeIndustry() {
    var theme: overgrownNanoforgeTheme? = createTheme()
    val commodityData: overgrownNanoforgeCommodityData = overgrownNanoforgeCommodityData()

    fun createTheme(): overgrownNanoforgeTheme? {

        val themesToChance = HashMap(niko_MPC_settings.overgrownNanoforgeThemesToChance)
        if (!market.canBeFarmed()) themesToChance -= Industries.FARMING
        val canAquaculture = market.planetEntity != null && Farming.AQUA_PLANETS.contains(market.planetEntity.typeId)
        if (!canAquaculture) themesToChance -= Industries.AQUACULTURE
        if (!market.canBeMined()) themesToChance -= Industries.MINING

        val picker = WeightedRandomPicker<String>()
        for (entry in themesToChance.entries) {
            picker.add(entry.key, entry.value)
        }
        val pickedTheme = picker.pick()
        val theme = overgrownNanoforgeTheme.convertToTheme(pickedTheme)
        if (theme == null) {
            niko_MPC_debugUtils.displayError("Null theme created by $this using $pickedTheme", true)
        }
        return theme
    }

    override fun init(id: String?, market: MarketAPI?) {
        super.init(id, market)
        instantiateNewCommodityData()
    }

    private fun instantiateNewCommodityData(): niko_MPC_overgrownNanoforgeCommodityData {
        return market.getOvergrownNanoforgeCommodities()
    }

    override fun isIndustry(): Boolean {
        return true
    }

    override fun canDowngrade(): Boolean {
        return (!market.hasJunk())
    }

    override fun apply(withIncomeUpdate: Boolean) {
        setupCommodities()
        super.apply(withIncomeUpdate)
    }

    open fun setupCommodities() {
        val commodityData = getCommodityData()
        commodityData.updateCommodityValues()
        for (supplyData in commodityData.supply.entries) {
            val commodityId = supplyData.key
            val amount = supplyData.value
            supply(commodityId, amount)
        }
        for (demandData in commodityData.demand.entries) {
            val commodityId = demandData.key
            val amount = demandData.value
            demand(commodityId, amount)
        }
    }

    private fun getCommodityData(): niko_MPC_overgrownNanoforgeCommodityData {
        return market.getOvergrownNanoforgeCommodities()
    }

    override fun apply() {
        apply(true)
    }

    override fun canShutDown(): Boolean {
        return false
    }

    override fun getCanNotShutDownReason(): String {
        return "sdfgbhnsedfbjhnsedfjhni"
    }

}
