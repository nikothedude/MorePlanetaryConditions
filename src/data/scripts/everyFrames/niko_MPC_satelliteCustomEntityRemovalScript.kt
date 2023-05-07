package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_satelliteUtils.getConditionLinkedHandler
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

open class niko_MPC_satelliteCustomEntityRemovalScript(
    entity: SectorEntityToken, conditionId: String, condition: niko_MPC_antiAsteroidSatellitesBase, val handler: niko_MPC_satelliteHandlerCore
): niko_MPC_conditionRemovalScript(entity, conditionId, condition) {
    override var thresholdTilEnd = 250

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun getPrimaryLocation(): LocationAPI? {
        return handler.getLocation()
    }

    override fun deleteItem() {
        handler.delete()
    }

    override fun shouldDelete(market: MarketAPI): Boolean {
        return (super.shouldDelete(market)
                || handler.deleted
                || entity == null
                || entity.market == null
                || market.getConditionLinkedHandler(conditionId) == null
                || !entity.hasSatelliteHandler(handler))
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