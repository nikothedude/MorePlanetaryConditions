package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

// WHAT THIS CLASS SHOULD HOLD
// 1. The base source of the industry
// 2. A list of structures spawned by this industry, or at least their parameters so they may be remade on demand
// 2.1: Alternatively, both, and just have this be the core's data hub it grabs everything from
// 3. The spreading scripts, as to allow spreading to happen when decivilized
// 4. The intel, to allow it to be active when uncolonized

// All in all, this is the permanent representation of the existance of a overgrown nanoforge
// The industry itself is only the hook we use to put this class into the game
class overgrownNanoforgeIndustryHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeIndustrySource = createBaseSource()
): overgrownNanoforgeHandler(initMarket, initBaseSource) {

    override val baseSource: overgrownNanoforgeIndustrySource = baseSource
    val junkHandlers: MutableSet<overgrownNanoforgeJunkHandler> = HashSet()

    val junkSpreader: overgrownNanoforgeJunkSpreader = overgrownNanoforgeJunkSpreader(this)

    private fun createBaseSource(): overgrownNanoforgeIndustrySource {
        val baseScore = getBaseScore()
        val supplyEffect = getBaseEffectPrototype().getInstance(this, baseScore)
        if (supplyEffect == null) {
            displayError("null supplyeffect on basestats oh god oh god oh god oh god oh god help")
            val source = overgrownNanoforgeIndustrySource(this, //shuld never happen
                mutableSetOf(overgrownNanoforgeAlterSupplySource(this, hashMapOf(Pair(Commodities.ORGANS, 500)))))
            return source
        return source
    }

    private fun getBaseEffectPrototype(): overgrownNanoforgeEffectPrototypes {
        return overgrownNanoforgeEffectPrototypes.ALTER_SUPPLY 
        // Originally we just wanted the base effect to be alter supply
        // THis method is just here for like. Potential future changes
    }

    override fun getStructure(): overgrownNanoforgeIndustry? {
        val ourMarket = market ?: return null
        return ourMarket.getOvergrownNanoforge()
    }

    companion object {
        fun getBaseScore(): Float {
            return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_BASE_SCORE_MIN, OVERGROWN_NANOFORGE_BASE_SCORE_MAX)
        }
    }
}

Before the collapse, the Domain kept tabs on all Nanoforges. Being a dangerous (and lucrative) technology, they kept a Domain engineer on site of each Nanoforge, along with (either officially or unofficially) a small projection of force to ensure it's proper use, dictated by them. Each Nanoforge would only produce exactly what the owner (often the government of a caste-world) is supposed to output, and exactly what they need to produce it. Any other usage would have been a slippery slope to a grey goo scenario, as claimed by the Domain COMSEC. 

Why this particular one seems to have fulfilled that prophecy is up for debate. Maybe the Domain was right, and it was used improperly, to fill more roles than intended, creating food for a foundry world? Maybe it was the on-hang engineer - desperate for a foothold in the new world, using their knowhow to disable the growth safeties in a way that bypasses the thousands of DRM protections? Or was it done with full knowledge of the consequences in a clear head, done for the sole purpose of destruction? One thing's for sure - This was no accident. 

The fact it's still running with nobody around most certainly is, though, as only someone completely insane or mind-bogglingly destructive would willingly induce a grey goo scenario on a planet. Right?