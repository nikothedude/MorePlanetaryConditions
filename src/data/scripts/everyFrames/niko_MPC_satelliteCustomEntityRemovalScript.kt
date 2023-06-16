package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.utilities.niko_MPC_satelliteUtils.getConditionLinkedHandler
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

open class niko_MPC_satelliteCustomEntityRemovalScript(
    entity: SectorEntityToken, conditionId: String, override val condition: niko_MPC_antiAsteroidSatellitesBase, val handler: niko_MPC_satelliteHandlerCore,
    hasDeletionScript: hasDeletionScript<out deletionScript?>
): niko_MPC_conditionRemovalScript(entity, conditionId, condition, hasDeletionScript) {
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

    override fun shouldDeleteWithMarket(market: MarketAPI?): Boolean {
        return (!handler.deleted
                && (super.shouldDeleteWithMarket(market)
                || entity == null
                || entity.market == null
                || market?.getConditionLinkedHandler(conditionId) == null
                || !entity.hasSatelliteHandler(handler)))
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        return true
    }
}