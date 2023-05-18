package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain

open class baseOvergrownNanoforgeManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    nanoforge: overgrownNanoforgeHandler,
    open val ourHandler: overgrownNanoforgeHandler
) : baseOvergrownNanoforgeIntel(brain, nanoforge) {

    override fun addStartStage() {
        super.addStartStage()
        addStage(overgrownNanoforgeIntelCullStage(brain, this), 0, false)
    }

    override fun addFactors() {
        super.addFactors()
        addRegenFactor()
        addCountermeasuresFactor()
    }

    open fun addRegenFactor() {
        addFactor(overgrownNanoforgeIntelFactorStructureRegeneration(brain, ourHandler))
    }

    open fun addCountermeasuresFactor() {
        addFactor(overgrownNanoforgeIntelFactorCountermeasures(brain, ourHandler))
    }

    fun culled() {
        if (shouldReportCulled()) reportCulled()
        
        ourHandler.culled()
    }

    fun shouldReportCulled(): Boolean = getMarket().isPlayerOwned
    fun reportCulled() {
        Global.getSector().getCampaignUI().addMessage("thing culled lol")
    }

    open fun shouldDeleteIfCulled(): Boolean {
        return true
    }

    override fun setProgress(progress: Int?) {
        super.setProgress(progress)
        if (areWeCulled()) {
            culled()
        }
    }

    fun areWeCulled() {
        return (getProgress() <= 0)
    }
}

class overgrownNanoforgeCullStage(brain: overgrownNanoforgeSpreadingBrain, intel: baseOvergrownNanoforgeManipulationIntel)
    : overgrownNanoforgeIntelStage(brain, intel) {
    
    val castedIntel: baseOvergrownNanoforgeManipulationIntel = intel
    
    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        castedIntel.culled()
    }

}