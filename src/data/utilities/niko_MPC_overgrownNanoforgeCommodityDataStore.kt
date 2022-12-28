package data.utilities

import com.fs.starfarer.api.impl.campaign.ids.Commodities


object niko_MPC_overgrownNanoforgeCommodityDataStore: HashMap<String, overgrownNanoforgeCommoditySetupData>() {

    val supplyData = overgrownNanoforgeCommoditySetupData(Commodities.SUPPLIES, 35f, hashMapOf(Pair(Commodities.METALS, 0.25f), Pair(Commodities.ORGANICS, 0.2f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))

    val metalData = overgrownNanoforgeCommoditySetupData(Commodities.METALS, 20f, hashMapOf(Pair(Commodities.ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val rareMetalData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_METALS, 30f, hashMapOf(Pair(Commodities.RARE_ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val machineryData = overgrownNanoforgeCommoditySetupData(Commodities.HEAVY_MACHINERY, 35f, hashMapOf(Pair(Commodities.METALS, 0.2f), Pair(Commodities.RARE_METALS, 0.08f)))
    val shipData = overgrownNanoforgeCommoditySetupData(Commodities.SHIPS, 30f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.3f), Pair(Commodities.HEAVY_MACHINERY, 0.5f), Pair(Commodities.SUPPLIES, 0.4f)))
    val weaponsData = overgrownNanoforgeCommoditySetupData(Commodities.HAND_WEAPONS, 40f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.4f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))
    val drugsData = overgrownNanoforgeCommoditySetupData(Commodities.DRUGS, 50f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f)))
    val domesticGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.DOMESTIC_GOODS, 14f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val luxuryGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.LUXURY_GOODS, 17f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val fuelData = overgrownNanoforgeCommoditySetupData(Commodities.FUEL, 60f, hashMapOf(Pair(Commodities.VOLATILES, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val foodData = overgrownNanoforgeCommoditySetupData(Commodities.FOOD, 40f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val oreData = overgrownNanoforgeCommoditySetupData(Commodities.ORE, 5f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.34f)))
    val rareOreData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_ORE, 8f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.4f)))

    val organicsData = overgrownNanoforgeCommoditySetupData(Commodities.ORGANICS, 12f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.35f)))
    val volatilesData = overgrownNanoforgeCommoditySetupData(Commodities.VOLATILES, 17f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.5f)))
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
}

class overgrownNanoforgeCommoditySetupData(
    val commodity: String, val cost: Float, val demandPerSupply: MutableMap<String, Float>
)
