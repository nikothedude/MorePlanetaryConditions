package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

class overgrownNanoforgeJunkCreationParams(
    val params: overgrownNanoforgeRandomizedSourceParams
) {

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