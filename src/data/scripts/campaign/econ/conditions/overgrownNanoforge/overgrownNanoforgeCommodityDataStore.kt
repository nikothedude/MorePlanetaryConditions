package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.impl.campaign.ids.Commodities
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT


object overgrownNanoforgeCommodityDataStore: HashMap<String, overgrownNanoforgeCommodityDataStore.overgrownNanoforgeCommoditySetupData>() {
    // TODO: nasty. make this an enum, or some shit, stop abusing pairs and hashmaps
    // FORMAT
    // Commodity ID (String)
    // Cost (Float)
    // Weight (Float)
    // Demand (HashMap of (Commodity ID (String) -> Ratio of demand to supply (Float)))

    val supplyData = overgrownNanoforgeCommoditySetupData(Commodities.SUPPLIES, 25f, 10f, hashMapOf(Pair(Commodities.METALS, 0.25f), Pair(Commodities.ORGANICS, 0.2f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))

    val metalData = overgrownNanoforgeCommoditySetupData(Commodities.METALS, 35f, 10f, hashMapOf(Pair(Commodities.ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val rareMetalData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_METALS, 40f, 5f, hashMapOf(Pair(Commodities.RARE_ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val machineryData = overgrownNanoforgeCommoditySetupData(Commodities.HEAVY_MACHINERY, 30f, 10f, hashMapOf(Pair(Commodities.METALS, 0.2f), Pair(Commodities.RARE_METALS, 0.08f)))
    val shipData = overgrownNanoforgeCommoditySetupData(Commodities.SHIPS, 30f, 2f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.3f), Pair(Commodities.HEAVY_MACHINERY, 0.5f), Pair(Commodities.SUPPLIES, 0.4f)))
    val weaponsData = overgrownNanoforgeCommoditySetupData(Commodities.HAND_WEAPONS, 25f, 5f, hashMapOf(Pair(Commodities.METALS, 0.5f), Pair(Commodities.RARE_METALS, 0.4f), Pair(Commodities.HEAVY_MACHINERY, 0.5f)))
    val drugsData = overgrownNanoforgeCommoditySetupData(Commodities.DRUGS, 50f, 1f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f)))
    val domesticGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.DOMESTIC_GOODS, 14f, 4f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val luxuryGoodsData = overgrownNanoforgeCommoditySetupData(Commodities.LUXURY_GOODS, 17f, 2f, hashMapOf(Pair(Commodities.ORGANICS, 0.5f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))
    val fuelData = overgrownNanoforgeCommoditySetupData(Commodities.FUEL, 40f, 3f, hashMapOf(Pair(Commodities.VOLATILES, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val foodData = overgrownNanoforgeCommoditySetupData(Commodities.FOOD, 35f, 1f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.2f)))
    val oreData = overgrownNanoforgeCommoditySetupData(Commodities.ORE, 5f, 2f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.34f)))
    val rareOreData = overgrownNanoforgeCommoditySetupData(Commodities.RARE_ORE, 8f, 8f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.4f)))

    val organicsData = overgrownNanoforgeCommoditySetupData(Commodities.ORGANICS, 12f, 10f, hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.35f)))
    val volatilesData = overgrownNanoforgeCommoditySetupData(Commodities.VOLATILES, 17f,8f,  hashMapOf(Pair(Commodities.HEAVY_MACHINERY, 0.5f)))

    val shipComponentsData = overgrownNanoforgeCommoditySetupData("IndEvo_parts", 40f, 5f, hashMapOf(Pair(Commodities.ORE, 0.8f), Pair(Commodities.HEAVY_MACHINERY, 0.1f)))

    fun reload() {
        this.clear()
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

        if (niko_MPC_settings.indEvoEnabled) {
            this["IndEvo_parts"] = shipComponentsData
        }
    }

    fun getWeightForCommodity(commodityId: String, nanoforge: overgrownNanoforgeHandler, negative: Boolean = false): Float {
        val coreHandler = nanoforge.getCoreHandler()
        val supply = coreHandler.getSupply(commodityId)
        var weight: Float = this[commodityId]?.baseWeight ?: return 0f
        if (negative && supply <= 0f) return 0f //prevents us from removing commodities we dont create
        if (supply > 0f && nanoforge.getCoreHandler().focusingOnExistingCommodities) weight *= OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT
        return weight
    }

    class overgrownNanoforgeCommoditySetupData(
        val commodity: String, val cost: Float, val baseWeight: Float, val demandPerSupply: MutableMap<String, Float>
    )
}
