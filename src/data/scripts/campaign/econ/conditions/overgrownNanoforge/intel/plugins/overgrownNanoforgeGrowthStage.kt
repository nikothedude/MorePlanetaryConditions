package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import org.lazywizard.lazylib.MathUtils

abstract class overgrownNanoforgeGrowthStage(override val intel: overgrownNanoforgeGrowthIntel) : overgrownNanoforgeIntelStage(intel) {
    abstract fun getThreshold(): Int

}

abstract class overgrownNanoforgeTargetDiscoveryStage(intel: overgrownNanoforgeGrowthIntel):
    overgrownNanoforgeGrowthStage(intel)

class overgrownNanoforgeDiscoverTargetNameStage(intel: overgrownNanoforgeGrowthIntel):
    overgrownNanoforgeTargetDiscoveryStage(intel) {
    override fun getThreshold(): Int {
        val anchor = MathUtils.getRandomNumberInRange(5, 30)
        val threshold = (anchor/100) * intel.maxProgress
        return threshold
    }

    override fun getName(): String {
        return "name learn"
    }

    override fun stageReached() {
        intel.params.nameKnown = true
    }

}

enum class growthDiscoveryStages() {

    TARGET {
        override fun createChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeTargetDiscoveryStage> {
            val nameStage = overgrownNanoforgeDiscoverTargetNameStage(intel)

            return setOf(nameStage)
        }
    };

    /*EFFECTS {

    },*/

    fun getChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage> {
        return sanitizeChildren(intel)
    }

    protected fun sanitizeChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage> {
        val children = createChildren(intel)

        return children
    }
    protected abstract fun createChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage>
}