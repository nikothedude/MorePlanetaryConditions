package data

import com.fs.starfarer.api.campaign.econ.MarketAPI

object MPC_conditionManager {}

    /*fun generateConditions(wasEnabledBefore: Boolean) {
        for (conditionData in MPC_conditionGenDataStore.getGeneratable(wasEnabledBefore)) {
            generateConditionsOf(conditionData)
        }
    }

    fun generateConditionsOf(conditionData: MPC_conditionGenDataStore) {

    }

}

enum class MPC_conditionGenDataStore(
    private val planetSpecWeights: MutableMap<String, Float>
) {

    fun shouldGenerate(): Boolean {
        return (isEnabled())
    }
    abstract fun canBeAppliedTo(market: MarketAPI): Boolean

    protected abstract fun isEnabled(): Boolean

    companion object {
        val allData = MPC_conditionGenDataStore.values().toSet()

        fun getGeneratable(wasEnabledBefore: Boolean): MutableList<MPC_conditionGenDataStore> {
            val generatable = ArrayList<MPC_conditionGenDataStore>()

            for (data in allData) {
                if (data.shouldGenerate()) generatable += data
            }
            return generatable
        }
    }
}*/
