package data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityData
import data.scripts.campaign.econ.industries.overgrownJunk.niko_MPC_baseOvergrownNanoforgeIndustry

abstract class overgrownNanoforgeTheme(): overgrownNanoforgeCommoditySource() {

    companion object {
        fun convertToTheme(pickedTheme: String): overgrownNanoforgeTheme? {
            val convertedTheme: overgrownNanoforgeTheme? = null
            when (pickedTheme) {
                Industries.HEAVYINDUSTRY -> return heavyIndustryTheme()
            }
            return null
        }
    }
}