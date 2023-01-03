package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_satelliteUtils.getConditionLinkedHandler
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

abstract class niko_MPC_conditionRemovalScript(val entity: SectorEntityToken?, var conditionId: String, val condition: niko_MPC_baseNikoCondition? = null): niko_MPC_baseNikoScript() {
    var runs = 0
    var thresholdTilEnd = 250

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun advance(amount: Float) {
        runs++
        val market: MarketAPI? = entity?.market ?: condition?.getMarket()
        if (market == null) {
            if (runs >= thresholdTilEnd) {
                delete()
            }
            return
        }
        if (shouldDelete(market)) {
            deleteItem()
        }
        delete()
    }

    abstract fun deleteItem()

    protected open fun shouldDelete(market: MarketAPI): Boolean {
        return (entity != null && (entity.isExpired || entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) || !market.hasCondition(conditionId))
    }

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        condition?.deletionScript = null

        return true
    }
}