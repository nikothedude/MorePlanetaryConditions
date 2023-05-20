package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorCountermeasures
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorStructureRegeneration
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage

open class baseOvergrownNanoforgeManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    open val ourHandler: overgrownNanoforgeHandler,
    hidden: Boolean = true,
    ) : baseOvergrownNanoforgeIntel(brain, hidden) {

    override fun addStartStage() {
        super.addStartStage()
        addStage(overgrownNanoforgeIntelCullStage(this), 0, false)
    }

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getMaxProgress())
    }

    override fun addFactors() {
        addRegenFactor()
        super.addFactors()
    }

    open fun addRegenFactor() {
        addFactorWrapped(overgrownNanoforgeIntelFactorStructureRegeneration(this))
    }
    open fun culled() {
        if (shouldReportCulled()) reportCulled()
        
        ourHandler.culled()
    }

    fun shouldReportCulled(): Boolean = getMarket().isPlayerOwned
    fun reportCulled() {
        Global.getSector().campaignUI.addMessage("thing culled lol")
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (areWeCulled()) {
            culled()
        }
    }

    fun areWeCulled(): Boolean {
        return (getProgress() <= 0)
    }

    override fun getName(): String {
        return "${ourHandler.getCurrentName()} on ${getMarket().name}"
    }
}

class overgrownNanoforgeIntelCullStage(
    override val intel: baseOvergrownNanoforgeManipulationIntel
    ): overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        intel.culled()
    }

}