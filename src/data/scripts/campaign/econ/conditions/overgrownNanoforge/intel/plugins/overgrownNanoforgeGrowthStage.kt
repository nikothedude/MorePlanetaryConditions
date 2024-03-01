package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import data.scripts.campaign.intel.baseNikoEventStageInterface
import data.utilities.niko_MPC_marketUtils.isPopulationAndInfrastructure
import java.awt.Color
import kotlin.math.roundToInt

enum class overgrownNanoforgeGrowthStages: baseNikoEventStageInterface<overgrownNanoforgeGrowthIntel> {
    START {
        override fun getName(): String = "Start"
        override fun stageReached(intel: overgrownNanoforgeGrowthIntel) { return }

        override fun isOneOffEvent(): Boolean = false
        override fun getThreshold(intel: overgrownNanoforgeGrowthIntel): Int = 0

        override fun hideIconWhenComplete(): Boolean = false
        override fun keepIconBrightWhenComplete(): Boolean = false
    },
    DISCOVER_TARGET {
        override fun getName(): String {
            return "Learn name of target industry"
        }

        override fun getDesc(): String = "Once reached, the name of the target industry will be revealed, if it exists."

        override fun stageReached(intel: overgrownNanoforgeGrowthIntel) {
            intel.params.nameKnown = true
        }

        override fun getThreshold(intel: overgrownNanoforgeGrowthIntel): Int {
            val threshold = (intel.discoverTargetAnchor/100f) * intel.maxProgress
            return threshold.roundToInt()
        }

        override fun modifyIntelUpdateWhenStageReached(
            info: TooltipMakerAPI,
            mode: IntelInfoPlugin.ListInfoMode?,
            tc: Color?,
            initPad: Float,
            intel: overgrownNanoforgeGrowthIntel
        ): Boolean {
            super.modifyIntelUpdateWhenStageReached(info, mode, tc, initPad, intel)
            val target = intel.params.ourIndustryTarget
            val highlightColor = if (target == null) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
            val targetName = if (target == null) "None" else target.currentName
            info.addPara("Target: %s", initPad, highlightColor, targetName)
            if (target != null) {
                val soundId = if (target.isPopulationAndInfrastructure()) "cr_playership_critical" else "cr_playership_warning"
                Global.getSoundPlayer().playUISound(soundId, 1f, 1f)
            }
            return true
        }
    },
    DISCOVER_EFFECTS {
        override fun getName(): String = "Discover effects"
        override fun getDesc(): String = "Once reached, the specific and exact effects of this structure will be revealed. " +
                "As it is approached, the score estimate will become more accurate."

        override fun stageReached(intel: overgrownNanoforgeGrowthIntel) {
            intel.knowExactEffects = true
        }

        override fun getThreshold(intel: overgrownNanoforgeGrowthIntel): Int {
            val threshold = (intel.params.percentThresholdToTotalScoreKnowledge/100f) * intel.maxProgress
            return threshold.roundToInt()
        }
    },
    END {
        override fun getName(): String = "Growth Finished"
        override fun getDesc(): String = "Once reached, the growth will become permanent and begin applying its effects."

        override fun stageReached(intel: overgrownNanoforgeGrowthIntel) {
            intel.growingComplete()
        }

        override fun getThreshold(intel: overgrownNanoforgeGrowthIntel): Int = intel.maxProgress
    };
}

/*enum class growthDiscoveryStages() {

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
}*/

class overgrownNanoforgeStartSpreadStage(override val intel: overgrownNanoforgeSpreadingIntel)
    : overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Spread starts"
    override fun getDesc(): String = "Growth begins, creating a new growth that must be culled or cultivated."

    override fun stageReached() {
        intel.startSpreading()
    }

    override fun getThreshold(): Int = intel.maxProgress

}