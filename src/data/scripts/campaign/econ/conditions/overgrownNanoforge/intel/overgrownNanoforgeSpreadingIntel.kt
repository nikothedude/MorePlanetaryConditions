package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeSpreadingIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    nanoforge: overgrownNanoforgeHandler,
) : baseOvergrownNanoforgeIntel(brain, nanoforge) {


    fun getTimeTilNextSpread(): Float {
        return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    }

    override fun init() {
        super.init()
        startPreparingToSpread()
    }

    fun startPreparingToSpread() {
        Global.getSector().intelManager.addIntel(this)
        setProgress(0)
        addFactor(overgrownNanoforgePrepareGrowthFactor(this))
        val max = getTimeTilNextSpread()
        setMaxProgress(max)
        addStage(overgrownNanoforgeBeginSpreadingStage(intel.brain, intel), intel.getMaxProgress())
    }

    fun stopPreparing() {
        Global.getSector().intelManager.removeIntel(this)
        removeFactorOfClass(overgrownNanoforgePrepareGrowthFactor::class.java as Class<EventFactor>)
        val iterator = stages.iterator()
        while (iterator.hasNext()) {
            val stage = iterator.next()
            if (stage is overgrownNanoforgePrepareGrowthFactor) iterator.remove()
        }
    }

    fun startSpreading() {
        brain.startSpreading()
    }

    fun stopSpreading() {
        brain.stopSpreading()
    }


}

class overgrownNanoforgeStartSpreadStage(brain: overgrownNanoforgeSpreadingBrain, intel: overgrownNanoforgeSpreadingIntel)
    : overgrownNanoforgeIntelStage(brain, intel) {
    
    val castedIntel: overgrownNanoforgeSpreadingIntel = intel
    
    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        castedIntel.startSpreading()
    }

}