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
    val effect: overgrownNanoforgeSpawnFleetEffect,
    val hostile: Boolean
): niko_MPC_baseNikoScript(), FleetEventListener {

    companion object {
        const val NANOFORGE_BOMBARDMENT_FLEET_FACTION_ID = Factions.DERELICT
    }

    val fleets: MutableMap<CampaignFleetAPI, Int> = HashMap()
    val idealFleets: Int = 5

    val maxFleetPoints: Int = 550
    val minPoints: Int = 3
    val maxPoints: Int = (maxFleetPoints/idealFleets).toInt()

    val bombardmentTimer = IntervalUtil(25f, 35f)

    var fleetFactionId: String = getEffectiveFactionId()
        set(value) {
            if (field != value) {
                updateFaction(value)
            }
            field = value
        }

    fun updateFaction(newId: String) {
        val faction = Global.getSector().getFaction(newId) ?: return
        for (fleet in fleets.keys) {
            fleet.setFaction(faction)
        }
    }

    override fun start() {
        Global.getSector().addScript(this)
        updateFactionId()
    }

    override fun stop() {
        Global.getSector().removeScript(this)
        updateFactionId()
    }

    override fun delete(): Boolean {
        killFleets()
        return super.delete()
    }

    private fun killFleets() {
        val iterator = fleets.keys.iterator()
        while (iterator.hasNext()) {
            val fleetHolder = iterator.next()
            fleetHolder.fleet.despawn()
            iterator.remove()
        }
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val daysAmount = getConvertedAmount(amount)
        bombardmentTimer.advance(daysAmount)
        if (bombardmentTimer.intervalElapsed()) {
            spawnBombardmentFleet()
        }
    }

    fun getMarket(): MarketAPI {
        return effect.getMarket()
    }


    private fun getConvertedAmount(amount: Float): Float {
        val days = Misc.getDays(amount)
        return days
    }

    fun spawnBombardmentFleet(): CampaignFleetAPI? {

        updateFactionId()

        val market = getMarket()
        val location = market.containingLocation ?: return null

        val random = MathUtils.getRandom()
        var points = (MathUtils.getRandomNumberBetween(minPoints, maxPoints))

        val remainder = (getUsedPoints() + points) - maxFleetPoints
        points -= (remainder.coerceAtLeast(0f))

        if (points < minPoints) return null

        var type = FleetTypes.PATROL_SMALL
        if (points > 8) type = FleetTypes.PATROL_MEDIUM
        if (points > 16) type = FleetTypes.PATROL_LARGE

        val params = FleetParamsV3(
            market,
            location,
            fleetFactionId,
            null,
            type,
            points, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return null

        setFleetProperties(fleet)

        fleets[fleet] = points 

        location.addEntity(fleet)
        fleet.setFacing(MathUtils.getRandomNumberBetween(0f, 360f))

        return fleet //FIXME: nonfunctional! untested! hell yaeh
    }

    private fun setFleetProperties(fleet: CampaignFleetAPI) {
        fleet.removeAbility(Abilities.EMERGENCY_BURN);
        fleet.removeAbility(Abilities.SENSOR_BURST);
        fleet.removeAbility(Abilities.GO_DARK);

        // to make sure they attack the player on sight when player's transponder is off
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true

        fleet.addScript(RemnantAssignmentAI(fleet, getSystem(), getMarket().primaryEntity))
        fleet.addEventListener(this)
    }

    fun getUsedPoints(): Float {
        var points = 0f
        for (entry in fleets) {
            points += entry.points
        }
        return points
    }

    fun updateFactionId() {
        fleetFactionId = getEffectiveFactionId()
    }

    fun getEffectiveFactionId(): String {
        if (!getMarket().isInhabited()) return Factions.DERELICT
        return if (isHostile()) Factions.DERELICT else getMarket.getFaction().getId()
    }

    fun isHostile(): Boolean {
        if (hostile) return true 
        if (!getMarket().isInhabited()) return true

        return false
    }

    private fun getSystem(): StarSystemAPI {
        return (getMarket().starSystem)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Object?) {
        if (fleet == null) return

        fleets -= fleet
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}

}
