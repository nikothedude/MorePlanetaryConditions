package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.campaign.econ.MarketAPI

abstract class baseOvergrownNanoforgeEventFactor(
    val overgrownIntel: overgrownNanoforgeIntel
): baseNikoEventFactor() {
    open fun shouldBeRemovedWhenSpreadingStops(): Boolean {
        return true
    }

    fun getMarket(): MarketAPI {
        return overgrownIntel.getMarket()
    }
}