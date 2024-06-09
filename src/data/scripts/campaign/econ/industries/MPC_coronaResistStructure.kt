package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_coronaResistStructureScript
import data.utilities.niko_MPC_fleetUtils.approximateCounterVelocityOfTerrain
import data.utilities.niko_MPC_fleetUtils.counterTerrainMovement
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus
import data.utilities.niko_MPC_industryIds
import org.magiclib.kotlin.getMarketsInLocation

class MPC_coronaResistStructure: baseNikoIndustry() {
    var script: MPC_coronaResistStructureScript? = null

    companion object {
        fun createBlueprint(cargo: CargoAPI) {
            cargo.addSpecial(SpecialItemData("industry_bp", niko_MPC_industryIds.coronaResistIndustry), 1f)
        }

        const val coronaResistance = 0.0f
    }

    override fun apply() {
        script = MPC_coronaResistStructureScript(market.primaryEntity, this)
        script!!.start()
        return
    }

    override fun unapply() {
        if (reapplying) return

        script?.delete()
        script = null

        super.unapply()
    }

    override fun isAvailableToBuild(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        if (interferenceDetected()) return false
        return true
    }

    private fun interferenceDetected(): Boolean {
        val market = getMarket() ?: return false
        val containingLocation = market.containingLocation ?: return false

        for (iterMarket in Misc.getMarketsInLocation(containingLocation)) {
            if (iterMarket == market) continue
            if (iterMarket.hasIndustry(niko_MPC_industryIds.coronaResistIndustry)) return true
        }
        return false
    }

    override fun getUnavailableReason(): String {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return "Blueprint unknown"
        }
        if (interferenceDetected()) {
            return "Maximum of one per star system"
        }
        return super.getUnavailableReason()
    }
}