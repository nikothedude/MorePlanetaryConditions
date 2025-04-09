package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin
import data.scripts.campaign.magnetar.AIPlugins.MPC_slavedOmegaCoreOfficerPlugin
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getStationFleet

class MPC_fractalStationOfficerAdder(
    val market: MarketAPI
): niko_MPC_baseNikoScript() {

    companion object {
        fun addIfDoesntExist(market: MarketAPI) {
            if (market.memoryWithoutUpdate["\$MPC_fractalOfficerAdder"] != null) {
                return
            }
            MPC_fractalStationOfficerAdder(market).start()
        }
        fun removeScript(market: MarketAPI) {
            val script = market.memoryWithoutUpdate["\$MPC_fractalOfficerAdder"] as? MPC_fractalStationOfficerAdder ?: return
            script.delete()
        }
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
        market.memoryWithoutUpdate["\$MPC_fractalOfficerAdder"] = this
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
        market.memoryWithoutUpdate["\$MPC_fractalOfficerAdder"] = null

        val fleet = market.getStationFleet()
        if (fleet?.flagship?.captain?.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            fleet.flagship.captain = null
        }
    }

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        if (market.admin.aiCoreId != niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            delete()
            return
        }

        val fleet = market.getStationFleet()
        if (fleet != null && fleet.flagship != null) {
            if (fleet.flagship.captain.aiCoreId != niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
                fleet.flagship.captain = MPC_slavedOmegaCoreOfficerPlugin().createPerson(
                    niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID,
                    market.factionId,
                    MathUtils.getRandom()
                )
                RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.flagship)
            }
        }

    }
}