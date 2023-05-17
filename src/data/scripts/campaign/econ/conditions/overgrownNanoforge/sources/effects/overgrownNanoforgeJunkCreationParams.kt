package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

class overgrownNanoforgeJunkCreationParams(
    val params: overgrownNanoforgeRandomizedSourceParams
    target: Industry? = null
) {

    var industryTarget = target ?: getIndustryTarget()
        set(value: Industry?) {
            alertPlayerTargetChanged(value)
            field = value
        }

    fun alertPlayerTargetChanged(newIndustry: Industry?) {

    }

    fun getIndustryTarget(): Industry? {
        if (!getMarket().hasMaxStructures()) return null

        var population: Industry? = null
        val picker: WeightedRandomPicker<Industry> = WeightedRandomPicker()
        for (structure in getMarket().industries) {
            if (!structure.isValidTarget()) continue
            if (structure is PopulationAndInfrastructure) {
                population = structure
                continue
            }
            picker.add(structure, getTargettingChance(structure))
        }
        return picker.pick() ?: population
    }

    fun Industry.isValidTarget(): Boolean {
        return (isApplied() && !isJunkStructure())
    }

    fun getMarket(): MarketAPI = params.getMarket()
    fun getIntel(): overgrownNanoforgeIntel = getMarket().getOvergrownNanoforgeIndustryHandler()!!.intel

    var cullingResistance = getInitialCullingResistance()

    fun getInitialCullingResistance(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
        )
    }

    var cullingResistanceRegeneration = getInitialCullingResistanceRegen()

    fun createBaseCullingResistanceRegeneration(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
        )
    }

    fun createJunk(): overgrownNanoforgeJunkHandler? {
        val junk = params.createJunk(cullingResistance, cullingResistanceRegeneration) ?: return null

        return junk
    }
}