package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import data.utilities.niko_MPC_satelliteUtils.getConditionLinkedHandler
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

open class niko_MPC_satelliteCustomEntityRemovalScript(var entity: SectorEntityToken, var conditionId: String, val handler: niko_MPC_satelliteHandlerCore, val condition: niko_MPC_antiAsteroidSatellitesBase? = null)
    : niko_MPC_baseNikoScript() {
    var runs = 0
    var thresholdTilEnd = 250

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun getPrimaryLocation(): LocationAPI? {
        return handler.getLocation()
    }

    override fun advance(amount: Float) {
        if (handler.deleted) {
            delete()
            return
        }
        runs++
        val market: MarketAPI? = entity.market
        if (market == null) {
            if (runs >= thresholdTilEnd) {
                delete()
            }
            return
        }
        if ((entity.isExpired || entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) ||
            !market.hasCondition(conditionId) || market.getConditionLinkedHandler(conditionId) == null || !entity.hasSatelliteHandler(handler)) {
            handler.delete()
        }
        delete()
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