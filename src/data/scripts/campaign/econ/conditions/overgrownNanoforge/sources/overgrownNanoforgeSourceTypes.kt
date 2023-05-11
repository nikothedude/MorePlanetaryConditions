package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.utilities.niko_MPC_settings

enum class overgrownNanoforgeSourceTypes(
    val chance: Float
) {
    INTERNAL(0f),
    STRUCTURE(100f);

    companion object picker {
        fun getAdjustedTypeChances(): MutableMap<overgrownNanoforgeSourceTypes, Float> {
            val adjustedMap = HashMap<overgrownNanoforgeSourceTypes, Float>()
            overgrownNanoforgeSourceTypes.values().forEach { adjustedMap[it] = it.chance }
            val iterator = adjustedMap.keys.iterator()
            while (iterator.hasNext()) {
                if (adjustedMap.keys.size <= 1) break
                val type = iterator.next()
                val chance = type.chance
                when (type) {
                    overgrownNanoforgeSourceTypes.STRUCTURE -> {
                        if (!niko_MPC_settings.OVERGROWN_NANOFORGE_USE_JUNK_STRUCTURES) {
                            iterator.remove()
                            continue
                        }
                    }
                    overgrownNanoforgeSourceTypes.INTERNAL -> {
                        iterator.remove() // TODO return to this
                    }
                }
            }
            return adjustedMap
        }

        fun adjustedPick(): overgrownNanoforgeSourceTypes? {
            val picker = WeightedRandomPicker<overgrownNanoforgeSourceTypes>()
            for (entry in getAdjustedTypeChances()) picker.add(entry.key, entry.value)
            return picker.pick()
        }
    }
}
