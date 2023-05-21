package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.everyFrames.deletionScript

class overgrownStructureDeletionScript(hasDeletionScript: hasDeletionScript<out deletionScript?>,
    val structure: baseOvergrownNanoforgeStructure, val market: MarketAPI
    ) : deletionScript(hasDeletionScript) {

    override fun deleteItem() {
        structure.getHandler()?.unapply() // not delete, since the habndler can exist without the structure
    }

    override fun shouldDelete(): Boolean {
        return (!market.hasIndustry(structure.id))
    }

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

}
