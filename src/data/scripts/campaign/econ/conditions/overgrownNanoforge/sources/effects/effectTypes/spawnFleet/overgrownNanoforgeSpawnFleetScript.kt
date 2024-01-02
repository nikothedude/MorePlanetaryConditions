package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyDecivListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.shouldntApplyFleetScript
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetScriptListMemoryId
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

class overgrownNanoforgeSpawnFleetScript(
    val effect: overgrownNanoforgeSpawnFleetEffect,
    val hostile: Boolean,
    val respawnMin: Float,
    val respawnMax: Float,
    val maxFleetPoints: Float,
    spawnAll: Boolean,
): niko_MPC_baseNikoScript(), FleetEventListener, ColonyDecivListener {

    companion object {
        const val NANOFORGE_BOMBARDMENT_DEFAULT_FACTION_ID = overgrownNanoforgeFleetFactionId
        const val NANOFORGE_BOMBARDMENT_FLEET_CONSTRUCTION_ID = overgrownNanoforgeFleetFactionId
    }

    private val chanceForTinyFleet: Float = 0.2f
    private val extraMinFleetSizeMult: Float = 0.3f
    private val chanceForExtraFleetSize: Float = 0.2f
    private val extraFleetSizeMult: Float = 3f
    val absoluteMinPoints = 3f

    private val largePatrolThreshold: Float = 48f
    private val mediumPatrolThreshold: Float = 26f
    val fleets: MutableMap<CampaignFleetAPI, Float> = HashMap()

    val idealFleets: Int = (maxFleetPoints/60).roundToInt().coerceAtLeast(1)
    val idealMaxFleetSize = (maxFleetPoints/idealFleets)
    val idealMinFleetSize = idealMaxFleetSize * 0.7f

    val bombardmentTimer = IntervalUtil(respawnMin, respawnMax)

    var fleetFactionId: String = getEffectiveFactionId()
        set(value) {
            if (field != value) {
                updateFaction(value)
            }
            field = value
        }

    init {
        if (spawnAll) spawnAllPossibleFleets()

        if (Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] !is MutableSet<*>) {
            Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] = HashSet<overgrownNanoforgeSpawnFleetScript>()
        }
        Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] as MutableSet<overgrownNanoforgeSpawnFleetScript> += this
    }

    private fun spawnAllPossibleFleets() {
        var index = 0
        var threshold = 1000
        while (true) {
            index++
            spawnBombardmentFleet() ?: break
            if (index >= threshold) {
                displayError("spawnAllPossibleFleets failsafe triggered")
                break
            }
        }
    }

    fun updateFaction(newId: String) {
        val faction = Global.getSector().getFaction(newId)?.id ?: return
        for (fleet in fleets.keys) {
            fleet.setFaction(faction)
        }
        if (!isHostile()) {
            removeUnsafeTagIfAble()
        } else addUnsafeTag()
    }

    private fun addUnsafeTag() {
        val system = getSystem()
        if (shouldntApplyFleetScript(system)) return
        system.addTag(Tags.THEME_UNSAFE)
    }

    private fun removeUnsafeTagIfAble() {
        /*if (getSystem().shouldRemoveUnsafeTag(this)) { // just to be safe, lets disable this for now
            getSystem().removeTag(Tags.THEME_UNSAFE)
        }*/
    }

    fun apply() {
        addUnsafeTag()
        updateFactionId()
        start()
    }

    fun unapply() {
        updateFactionId()
        stop()
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
        Global.getSector().listenerManager.addListener(this, false)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
        Global.getSector().listenerManager.removeListener(this)
        bombardmentTimer.elapsed = 0f
    }

    override fun delete(): Boolean {
        killFleets()
        if (Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] !is MutableSet<*>) {
            Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] = HashSet<overgrownNanoforgeSpawnFleetScript>()
        }
        Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] as MutableSet<overgrownNanoforgeSpawnFleetScript> -= this
        removeUnsafeTagIfAble()
        return super.delete()
    }

    private fun killFleets() {
        for (fleet in fleets.toMutableMap().keys) {
            fleet.despawn()
        }
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        bombardmentTimer.advance(amount)
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

        var points: Float = (MathUtils.getRandomNumberInRange(getMinPointsForCreation(), getMaxPointsForCreation()))
        val random = MathUtils.getRandom()
        val randomFloat = random.nextFloat()
        if (randomFloat < chanceForExtraFleetSize) {
            points *= extraFleetSizeMult
        } else if (randomFloat < chanceForTinyFleet) {
            points *= extraMinFleetSizeMult
        }

        val remainder = (getUsedPoints() + points) - maxFleetPoints
        points -= (remainder.coerceAtLeast(0F))

        if (points < absoluteMinPoints) return null

        var type = FleetTypes.PATROL_SMALL
        if (points >= mediumPatrolThreshold) type = FleetTypes.PATROL_MEDIUM
        if (points >= largePatrolThreshold) type = FleetTypes.PATROL_LARGE

        val params = FleetParamsV3(
            location.location,
            NANOFORGE_BOMBARDMENT_FLEET_CONSTRUCTION_ID,
            null,
            type,
            points, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return null

        fleets[fleet] = points

        location.addEntity(fleet)
        fleet.facing = MathUtils.getRandomNumberInRange(0f, 360f)
        val marketLoc = market.primaryEntity.location ?: market.location
        fleet.setLocation(marketLoc.x, marketLoc.y)

        setFleetProperties(fleet)

        /*val playerFleet = Global.getSector().playerFleet ?: return fleet
        val playerLoc = playerFleet.location
        if (market.containingLocation == playerFleet.containingLocation) {
            playerFleet.setLocation(marketLoc.x, marketLoc.y)
        }*/

        return fleet
    }

    private fun getMaxPointsForCreation(): Float {
        var max = idealMaxFleetSize
        return max.coerceAtMost(maxFleetPoints)
       /* val mutatedIdealFleets = if (hostileAndInhabited()) idealFleetsColonizedAndHostile else idealFleets
        return (maxFleetPoints/mutatedIdealFleets).coerceAtMost(maxFleetPoints)*/
    }

    private fun getMinPointsForCreation(): Float {
        var min = idealMinFleetSize

        return min.coerceAtMost(getRemainingPoints())
        /*val base = if (hostileAndInhabited()) idealMinFleetSizeColonizedAndHostile else idealMinFleetSize
        return base.coerceAtMost(getRemainingPoints())*/
    }

    private fun hostileAndInhabited(): Boolean {
        return isHostile() && getMarket().isInhabited()
    }

    private fun getRemainingPoints(): Float {
        val used = getUsedPoints()
        return maxFleetPoints - used
    }

    private fun setFleetProperties(fleet: CampaignFleetAPI) {
        fleet.setFaction(fleetFactionId)

        fleet.removeAbility(Abilities.EMERGENCY_BURN)
        fleet.removeAbility(Abilities.SENSOR_BURST)
        fleet.removeAbility(Abilities.GO_DARK)

        // to make sure they attack the player on sight when player's transponder is off
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = false

        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS] = true

        fleet.addScript(overgrownNanoforgeFleetAssignmentAI(fleet, getSystem(), getMarket().primaryEntity))
        fleet.addEventListener(this)

        RemnantSeededFleetManager.addRemnantInteractionConfig(fleet)

    }

    fun getUsedPoints(): Float {
        var points = 0f
        for (entry in fleets) {
            points += entry.value
        }
        return points
    }

    fun updateFactionId() {
        fleetFactionId = getEffectiveFactionId()
    }

    fun getEffectiveFactionId(): String {
        if (!getMarket().isInhabited()) return NANOFORGE_BOMBARDMENT_DEFAULT_FACTION_ID
        return if (isHostile()) NANOFORGE_BOMBARDMENT_DEFAULT_FACTION_ID else getMarket().faction.id
    }

    fun isHostile(): Boolean {
        if (hostile) return true 
        if (!getMarket().isInhabited()) return true

        return false
    }

    fun getSystem(): StarSystemAPI {
        return (getMarket().starSystem)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: CampaignEventListener.FleetDespawnReason?, param: Any?) {
        if (fleet == null) return

        fleets -= fleet
        fleet.removeScriptsOfClass(overgrownNanoforgeFleetAssignmentAI::class.java)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {}

    override fun reportColonyAboutToBeDecivilized(market: MarketAPI?, fullyDestroyed: Boolean) {
    }

    override fun reportColonyDecivilized(market: MarketAPI?, fullyDestroyed: Boolean) {
        if (market == null) return
        if (market == getMarket()) {
            updateFactionId()
        }
    }

}

private fun StarSystemAPI.shouldRemoveUnsafeTag(script: overgrownNanoforgeSpawnFleetScript): Boolean {
    if (script.isHostile()) return false
    if (hasTag(Tags.THEME_REMNANT_MAIN) && !(hasTag(Tags.THEME_REMNANT_DESTROYED) || hasTag(Tags.THEME_REMNANT_NO_FLEETS))) return false

    if (Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] !is MutableSet<*>) {
        Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] = HashSet<overgrownNanoforgeSpawnFleetScript>()
    }
    val scripts = Global.getSector().memoryWithoutUpdate[overgrownNanoforgeFleetScriptListMemoryId] as MutableSet<overgrownNanoforgeSpawnFleetScript>
    for (iteratedScript in scripts) {
        if (iteratedScript == script) continue
        if (iteratedScript.getSystem() == this) {
            if (iteratedScript.isHostile()) return false
        }
    }
    return true
}
