package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain

open class baseOvergrownNanoforgeManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    nanoforge: overgrownNanoforgeHandler
) : baseOvergrownNanoforgeIntel(brain, nanoforge) {

    override fun addStages() {
        super.addStages()
        
        
    }

    fun culled() {
        if (shouldReportCulled()) reportCulled()
        
        handler.delete()
        if (shouldDeleteIfCulled()) delete()
    }

    private fun shouldDeleteIfCulled(): Boolean {

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