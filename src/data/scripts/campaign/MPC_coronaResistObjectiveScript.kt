package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.campaign.objectives.MPC_baryonEmitterObjectiveScript
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus

class MPC_coronaResistObjectiveScript(entity: SectorEntityToken, val plugin: MPC_baryonEmitterObjectiveScript): MPC_coronaResistScript(entity) {

    override fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        if (!super.shouldAffectFleet(fleet)) return false

        val hacked = plugin.isHacked
        val faction = plugin.getEntity().faction
        val playerFaction = Global.getSector().playerFaction

        val neededRep = fleet.getRepLevelForArrayBonus()

        if (hacked && playerFaction.getRelationshipLevel(fleet.faction) >= neededRep) return true
        if (faction.getRelationshipLevel(fleet.faction) >= neededRep) return true

        return false
    }

    override fun advance(amount: Float) {
        if (plugin.script != this) { // sanity
            delete()
            return
        }

        super.advance(amount)
    }

}