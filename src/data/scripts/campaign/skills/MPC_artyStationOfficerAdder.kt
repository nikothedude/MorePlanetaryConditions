package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin
import data.scripts.campaign.magnetar.AIPlugins.MPC_slavedOmegaCoreOfficerPlugin
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import indevo.industries.artillery.industry.ArtilleryStation
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getStationFleet

class MPC_artyStationOfficerAdder(
    val market: MarketAPI
): niko_MPC_baseNikoScript() {

    companion object {
        fun addIfDoesntExist(market: MarketAPI) {
            if (market.memoryWithoutUpdate["\$MPC_artyStationOfficerAdder"] != null) {
                return
            }
            MPC_artyStationOfficerAdder(market).start()
        }
        fun removeScript(market: MarketAPI) {
            val script = market.memoryWithoutUpdate["\$MPC_artyStationOfficerAdder"] as? MPC_artyStationOfficerAdder ?: return
            script.delete()
        }
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
        market.memoryWithoutUpdate["\$MPC_artyStationOfficerAdder"] = this
    }

    override fun stopImpl() {
        val artyStationObject = market.connectedEntities?.firstOrNull { it.customEntityType == "IndEvo_ArtilleryStation" } ?: return
        val fleet = artyStationObject.memoryWithoutUpdate.getFleet(MemFlags.STATION_FLEET) ?: return
        if (fleet.flagship?.captain?.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            fleet.flagship.captain = null
        }

        Global.getSector().removeScript(this)
        market.memoryWithoutUpdate["\$MPC_artyStationOfficerAdder"] = null
    }

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        if (market.admin.aiCoreId != niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            delete()
            return
        }

        val artyStationObject = market.connectedEntities.firstOrNull { it.customEntityType == "IndEvo_ArtilleryStation" } ?: return
        val fleet = artyStationObject.memoryWithoutUpdate.getFleet(MemFlags.STATION_FLEET) ?: return
        if (fleet.flagship != null) {
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