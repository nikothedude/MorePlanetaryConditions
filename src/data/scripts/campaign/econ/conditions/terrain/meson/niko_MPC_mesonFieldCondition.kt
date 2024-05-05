package data.scripts.campaign.econ.conditions.terrain.meson

import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript

class niko_MPC_mesonFieldCondition:

niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_mesonConditionDeletionScript>{

    override var deletionScript: niko_MPC_mesonConditionDeletionScript? = null

    var applied = false
    //var plugin: niko_MPC_mesonField? = null

    val slipstreamDetectionRadius = 2
    val defenseRatingIncrement = 300

    override fun apply(id: String) {
        super.apply(id)

        applyConditionAttributes(id)

        if (applied)
            return

        spawnTerrain(id)
        applied = true
    }

    private fun spawnTerrain(id: String) {
        TODO("Not yet implemented")
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        if (id == null) return

        unapplyConditionAttributes(id)

        val market = getMarket() ?: return
        createDeletionScriptInstance(market)
    }

    private fun applyConditionAttributes(id: String) {
        val market = getMarket() ?: return


    }

    private fun unapplyConditionAttributes(id: String) {
        TODO("Not yet implemented")
    }

    override fun createDeletionScriptInstance(vararg args: Any): niko_MPC_mesonConditionDeletionScript {
        val market = args[0] as MarketAPI
        return niko_MPC_mesonConditionDeletionScript(market.primaryEntity, getCondition().id, this, this)
    }
}

class niko_MPC_mesonConditionDeletionScript(entity: SectorEntityToken?, conditionId: String,
                                            override val condition: niko_MPC_mesonFieldCondition? = null,
                                            hasDeletionScript: hasDeletionScript<out deletionScript>
) : niko_MPC_conditionRemovalScript(entity, conditionId, condition, hasDeletionScript)