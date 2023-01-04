package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.impl.campaign.ids.Commodities
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry


object overgrownNanoforgeCommodityDataStore: HashMap<String, overgrownNanoforgeCommodityDataStore.overgrownNanoforgeCommoditySetupData>() {

    val supplyData = overgrownNanoforgeCommoditySetupData(Commodities.SUPPLIES, 35f, 20f, hashMapOf(Pair(Commodities.METALS, 0.25f), Pair(Commodities.ORGANICS, 0.2f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))

    val metalData = overgrownNanoforgeCommoditySetupData(Commodities.METALS, 20f, 10f, hashMapOf(Pair(Commodities.ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val rareMetalData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_METALS, 30f, 5f, hashMapOf(Pair(Commodities.RARE_ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val machineryData = overgrownNanoforgeCommoditySetupData(Commodities.HEAVY_MACHINERY, 35f, 10f, hashMapOf(Pair(Commodities.METALS, 0.2f), Pair(Commodities.RARE_METALS, 0.08f)))
    val shipData = overgrownNanoforgeCommoditySetupData(Commodities.SHIPS, 30f, 2f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.3f), Pair(Commodities.HEAVY_MACHINERY, 0.5f), Pair(Commodities.SUPPLIES, 0.4f)))
    val weaponsData = overgrownNanoforgeCommoditySetupData(Commodities.HAND_WEAPONS, 40f, 5f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.4f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))
    val drugsData = overgrownNanoforgeCommoditySetupData(Commodities.DRUGS, 50f, 1f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f)))
    val domesticGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.DOMESTIC_GOODS, 14f, 4f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val luxuryGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.LUXURY_GOODS, 17f, 2f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val fuelData = overgrownNanoforgeCommoditySetupData(Commodities.FUEL, 60f, 3f, hashMapOf(Pair(Commodities.VOLATILES, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val foodData = overgrownNanoforgeCommoditySetupData(Commodities.FOOD, 40f, 1f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val oreData = overgrownNanoforgeCommoditySetupData(Commodities.ORE, 5f, 10f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.34f)))
    val rareOreData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_ORE, 8f, 8f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.4f)))

    val organicsData = overgrownNanoforgeCommoditySetupData(Commodities.ORGANICS, 12f, 10f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.35f)))
    val volatilesData = overgrownNanoforgeCommoditySetupData(Commodities.VOLATILES, 17f,8f,  hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.5f)))
    init {
        reload()
    }
    fun reload() {
        this[Commodities.SUPPLIES] = supplyData
        this[Commodities.METALS] = metalData
        this[Commodities.RARE_METALS] = rareMetalData
        this[Commodities.HEAVY_MACHINERY] = machineryData
        this[Commodities.SHIPS] = shipData
        this[Commodities.HAND_WEAPONS] = weaponsData
        this[Commodities.DRUGS] = drugsData
        this[Commodities.DOMESTIC_GOODS] = domesticGoodsData
        this[Commodities.LUXURY_GOODS] = luxuryGoodsData
        this[Commodities.FUEL] = fuelData
        this[Commodities.FOOD] = foodData

        this[Commodities.ORE] = oreData
        this[Commodities.RARE_ORE] = rareOreData
        this[Commodities.VOLATILES] = volatilesData
        this[Commodities.ORGANICS] = organicsData
    }

    fun getWeightForCommodity(commodityId: String, nanoforge: overgrownNanoforgeIndustry): Float {
        var weight: Float = this[commodityId]?.baseWeight ?: return 0f
        val supply = nanoforge.getSupply(commodityId)
        if (supply != null && supply.quantity.modifiedInt > 0f) weight *= OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT
        return weight
    }

    class overgrownNanoforgeCommoditySetupData(
        val commodity: String, val cost: Float, val baseWeight: Float, val demandPerSupply: MutableMap<String, Float>
    )
}
