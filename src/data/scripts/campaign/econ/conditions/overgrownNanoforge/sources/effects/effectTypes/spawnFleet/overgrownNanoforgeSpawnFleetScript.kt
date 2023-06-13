package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class overgrownNanoforgeSpawnFleetScript(
    val effect: overgrownNanoforgeSpawnFleetEffect
): niko_MPC_baseNikoScript() {

    companion object {
        const val NANOFORGE_BOMBARDMENT_FLEET_FACTION_ID = Factions.DERELICT
    }

    val fleets: MutableSet<CampaignFleetAPI> = HashSet()
    val bombardmentTimer = IntervalUtil(90f, 140f)

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun delete(): Boolean {
        killFleets()
        return super.delete()
    }

    private fun killFleets() {
        for (fleet in fleets) {
            fleet.despawn()
        }
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val daysAmount = getConvertedAmount(amount)
        bombardmentTimer.advance(daysAmount)
        if (bombardmentTimer.intervalElapsed()) {
            spawnBombardmentFleet()
        }

        TODO("Not yet implemented")
    }

    fun getMarket(): MarketAPI {
        return effect.getMarket()
    }


    private fun getConvertedAmount(amount: Float): Float {
        val days = Misc.getDays(amount)
        return days
    }

    fun spawnBombardmentFleet(): CampaignFleetAPI {

        val market = getMarket()
        val params = FleetParamsV3(
            market,
            market.location,
            NANOFORGE_BOMBARDMENT_FLEET_FACTION_ID,
            null,
            FleetTypes.PATROL_LARGE,
            100f, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params)

        setFleetProperties(fleet)

        fleets += fleet
        return fleet //FIXME: nonfunctional! untested! hell yaeh
    }

    private fun setFleetProperties(fleet: CampaignFleetAPI?) {
        if (fleet == null) return

        fleet.removeAbility(Abilities.GO_DARK)


        // to make sure they attack the player on sight when player's transponder is off
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true

        fleet.addScript(RemnantAssignmentAI(fleet, getSystem(), null))


    }

    private fun getSystem(): StarSystemAPI {
        return (getMarket().starSystem)
    }

}
