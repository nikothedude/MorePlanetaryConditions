package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class overgrownNanoforgeSpawnFleetScript(
    val effect: overgrownNanoforgeSpawnFleetEffect
): niko_MPC_baseNikoScript() {

    companion object {
        const val NANOFORGE_BOMBARDMENT_FLEET_FACTION_ID = Factions.DERELICT
    }

    val fleets: MutableSet<CampaignFleetAPI> = HashSet()

    val timer = IntervalUtil(90f, 140f)

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val daysAmount = getConvertedAmount(amount)
        timer.advance(daysAmount)
        if (timer.intervalElapsed()) {
            spawnBombardmentFleet()
        }
        TODO("Not yet implemented")
    }

    private fun getConvertedAmount(amount: Float): Float {
        val days = Misc.getDays(amount)
        return days
    }

    fun spawnBombardmentFleet(): CampaignFleetAPI {

        val fleet = FleetFactoryV3.createFleet()
    }

}
