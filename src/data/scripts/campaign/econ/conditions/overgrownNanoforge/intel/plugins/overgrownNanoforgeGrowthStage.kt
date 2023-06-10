package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

abstract class overgrownNanoforgeGrowthStage(override val intel: overgrownNanoforgeGrowthIntel) : overgrownNanoforgeIntelStage(intel) {

}

abstract class overgrownNanoforgeTargetDiscoveryStage(intel: overgrownNanoforgeGrowthIntel):
    overgrownNanoforgeGrowthStage(intel)

abstract class overgrownNanoforgeEffectDiscoveryStage(intel: overgrownNanoforgeGrowthIntel): overgrownNanoforgeGrowthStage(intel)

class overgrownNanoforgeDiscoverTargetNameStage(intel: overgrownNanoforgeGrowthIntel):
    overgrownNanoforgeTargetDiscoveryStage(intel) {

    val anchor = MathUtils.getRandomNumberInRange(5, 30)

    override fun getThreshold(): Int {
        val threshold = (anchor/100f) * intel.maxProgress
        return threshold.roundToInt()
    }

    override fun getName(): String {
        return "name learn"
    }

    override fun getDesc(): String = "Once reached, the name of the target industry will be revealed, if it exists."

    override fun stageReached() {
        intel.params.nameKnown = true
    }

}

class overgrownNanoforgeDiscoverEffectStage(intel: overgrownNanoforgeGrowthIntel): overgrownNanoforgeEffectDiscoveryStage(intel) {

    val anchor: Int
        get() {
            return intel.params.percentThresholdToTotalScoreKnowledge //TODO: ugly as fuck please refactor this
        }
    override fun getName(): String = "dsicover effects"
    override fun getDesc(): String = "Once reached, the specific and exact effects of this structure will be revealed. " +
            "As it is approached, the score estimate will become more accurate."

    override fun stageReached() {
        intel.knowExactEffects = true
    }

    override fun getThreshold(): Int {
        val threshold = (anchor/100f) * intel.maxProgress
        return threshold.roundToInt()
    }

}

enum class growthDiscoveryStages() {

    TARGET {
        override fun createChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeTargetDiscoveryStage> {
            val nameStage = overgrownNanoforgeDiscoverTargetNameStage(intel)

            return setOf(nameStage)
        }
    },

    EFFECTS {
        override fun createChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeEffectDiscoveryStage> {
            val masterStage = overgrownNanoforgeDiscoverEffectStage(intel)

            return setOf(masterStage)
        }

    };

    fun getChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage> {
        return sanitizeChildren(intel)
    }

    protected fun sanitizeChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage> {
        val children = createChildren(intel)

        return children
    }
    protected abstract fun createChildren(intel: overgrownNanoforgeGrowthIntel): Set<overgrownNanoforgeGrowthStage>
}
