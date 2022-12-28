package data.scripts.campaign.econ.conditions.overgrownNanoforge

import data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData.overgrownNanoforgeCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData.overgrownNanoforgeCommoditySource

class overgrownNanoforgeCommodityData(
    val sources: MutableSet<overgrownNanoforgeCommoditySource> = HashSet()
) {



    fun applySource(source: overgrownNanoforgeCommoditySource) {
        val id = source.id
        val supplyData = getSupplyForSource(id)
        val sourceData = source.getSupply() ?: return
        for (commodityId in sourceData.keys) {
            supplyData[commodityId] = sourceData[commodityId]!!
        }
        supplyData[id] = sourceData[supplyData.commodityId]
    }

    fun getSupplyForSource(id: String): HashMap<String, overgrownNanoforgeSupplyData> {
        val supplyData = sourceIdToSupply[id]
        if (supplyData == null) sourceIdToSupply[id] = HashMap()
        return sourceIdToSupply[id]
    }

    fun getSupply(): overgrownNanoforgeSupplyData {
        for (sources in sourceIdToSupply.keys) {
            for (commodityId in sourceIdToSupply[sources]!!) {

            }
        }
    }


}
class overgrownNanoforgeSupplyData(
    var id: String,
    var category: overgrownNanoforgeCategories,
    var commodityId: String,
    var amount: Int,
    var demand: MutableSet<overgrownNanoforgeDemandData>
)

class overgrownNanoforgeDemandData(
    var id: String,
    var category: String,
    var commodityId: String,
    var amount: Int,
    var supply: MutableSet<overgrownNanoforgeSupplyData>
)
