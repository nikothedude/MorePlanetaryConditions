package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_coronaResistScript
import data.scripts.campaign.MPC_coronaResistStructureScript
import data.utilities.niko_MPC_fleetUtils.approximateCounterVelocityOfTerrain
import data.utilities.niko_MPC_fleetUtils.counterTerrainMovement
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus
import data.utilities.niko_MPC_industryIds
import org.magiclib.kotlin.getMarketsInLocation

class MPC_coronaResistStructure: baseNikoIndustry() {
    var script: MPC_coronaResistStructureScript? = null

    override fun apply() {
        super.apply(true)
        if (reapplying) return
        if (script == null) {
            script = MPC_coronaResistStructureScript(market.primaryEntity, this)
        }
        script?.start()
    }

    override fun unapply() {
        if (reapplying) return

        script?.delete()
        script = null

        super.unapply()
    }

    override fun showWhenUnavailable(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        return true
    }

    override fun isAvailableToBuild(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        if (interferenceDetected()) return false
        return super.isAvailableToBuild()
    }

    private fun interferenceDetected(): Boolean {
        return MPC_coronaResistScript.interferenceDetected(market.containingLocation)
    }

    override fun getUnavailableReason(): String? {
        if (!super.isAvailableToBuild()) {
            return super.getUnavailableReason()
        }

        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return "Blueprint unknown"
        }
        if (MPC_coronaResistScript.getScriptsInLocation(market.containingLocation).size > 1) {
            return "Maximum of one baryon emitter per star system"
        }
        return null
    }
}