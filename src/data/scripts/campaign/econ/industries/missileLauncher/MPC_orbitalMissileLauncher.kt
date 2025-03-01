package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import data.scripts.everyFrames.niko_MPC_baseNikoScript

abstract class MPC_orbitalMissileLauncher: niko_MPC_baseNikoScript() {

    companion object {
        fun getMissileLaunchers(): MutableSet<MPC_orbitalMissileLauncher> {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] == null) {
                Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] = HashSet<MPC_orbitalMissileLauncher>()
            }
            return Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] as MutableSet<MPC_orbitalMissileLauncher>
        }
    }

    abstract fun getHost(): SectorEntityToken?

    open fun apply() {
        getMissileLaunchers() += this
    }
    open fun unapply() {
        getMissileLaunchers() -= this
    }

    override fun startImpl() {
        getHost()?.addScript(this)
    }

    override fun stopImpl() {
        getHost()?.removeScript(this)
    }

    open fun advanceInCombat(amount: Float, engine: CombatEngineAPI) {

    }

    fun canJoinBattle(battle: BattleAPI): Boolean {
        return getSideForBattle(battle) != BattleSide.NO_JOIN
    }

    open fun getSideForBattle(battle: BattleAPI): BattleSide {
        val dummyFleet = FleetFactory.createEmptyFleet(getFaction().id, FleetTypes.PATROL_LARGE, null)
        val pickedSide = battle.pickSide(dummyFleet)
        dummyFleet.despawn()

        return pickedSide
    }

    abstract fun getFaction(): FactionAPI
}