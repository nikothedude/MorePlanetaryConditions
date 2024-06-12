package data.scripts.campaign

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus

class MPC_coronaResistStructureScript(entity: SectorEntityToken, val industry: MPC_coronaResistStructure): MPC_coronaResistScript(entity) {
    override fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        if (!super.shouldAffectFleet(fleet)) return false
        if (!industry.isFunctional) return false

        val market = industry.market
        val reqRep = fleet.getRepLevelForArrayBonus()

        return (fleet.faction.getRelationshipLevel(market.faction) >= reqRep)
    }

    override fun advance(amount: Float) {
        if (industry.script == null) {
            delete()
            return
        }

        super.advance(amount)
    }
}