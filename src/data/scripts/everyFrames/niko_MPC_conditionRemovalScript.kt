package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCondition
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

abstract class niko_MPC_conditionRemovalScript(val entity: SectorEntityToken?, var conditionId: String,
                                               open val condition: niko_MPC_baseNikoCondition? = null,
                                               hasDeletionScript: hasDeletionScript<out deletionScript?>
): deletionScript(hasDeletionScript) {

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        if (shouldDelete()) {
            deleteItem()
        }
        delete()
    }

    override fun shouldDelete(): Boolean {
        val market: MarketAPI? = entity?.market ?: condition?.getMarket()
        return shouldDeleteWithMarket(market)
    }

    open fun shouldDeleteWithMarket(market: MarketAPI?): Boolean {
        if (market == null) {
            if (runs >= thresholdTilEnd) {
                return true
            }
            return false
        }
        return (entity != null && (entity.isExpired || entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) || !market.hasCondition(conditionId))
    }

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun deleteItem() {
        condition?.delete()
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        hasDeletionScript.deletionScript = null

        return true
    }
}