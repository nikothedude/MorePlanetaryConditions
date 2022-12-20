package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_miscUtils.getStationFleet
import data.utilities.niko_MPC_miscUtils.getStationsInOrbit

/** Every successful [advance] run, iterate through [niko_MPC_satelliteHandlerCore.getPrimaryHolder]'s [MarketAPI.getConnectedEntities]
 * if the provided value is a market, or if its an entity, and it has a market. We then check every found entity in that list
 * to see if it's a station, and if it is, we check it's station fleet to see if it's engaged in combat. If it is, we run
 * [niko_MPC_satelliteHandlerCore.tryToJoinBattle].(stationBattle).*/
class niko_MPC_satelliteStationBattleChecker(val handler: niko_MPC_satelliteHandlerCore) : niko_MPC_deltaTimeScript() {
    override val thresholdForAdvancement: Float
        get() = 0.2f
    override val doOneSecondDelayIfPlayerNotNear: Boolean
        get() = true

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (!canAdvance(amount)) return
        var handlerEntity: HasMemory = handler.getPrimaryHolder() ?: return
        if (handlerEntity is SectorEntityToken) {
            if (handlerEntity.market != null) {
                handlerEntity = handlerEntity.market
            } else return
        }
        if (handlerEntity !is MarketAPI) return
        for (possibleStation: SectorEntityToken in handlerEntity.getStationsInOrbit()) {
            val stationFleet = possibleStation.getStationFleet() ?: continue
            // no fleet: fleet defeated
            // has base fleet: fleet defeated
            // no need to doublecheck
            val battle: BattleAPI = stationFleet.battle ?: continue
            handler.tryToJoinBattle(battle)
        }
    }
}