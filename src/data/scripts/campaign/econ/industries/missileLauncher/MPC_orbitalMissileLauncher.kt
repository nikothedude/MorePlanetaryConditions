package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.Global
import data.utilities.niko_MPC_mathUtils.roundNumTo
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import data.scripts.campaign.abilities.MPC_missileStrikeAbility
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_stringUtils
import org.lazywizard.lazylib.MathUtils

abstract class MPC_orbitalMissileLauncher: niko_MPC_baseNikoScript() {

    companion object {
        fun getMissileLaunchers(): MutableSet<MPC_orbitalMissileLauncher> {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] == null) {
                Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] = HashSet<MPC_orbitalMissileLauncher>()
            }
            return Global.getSector().memoryWithoutUpdate["\$MPC_missileLaunchers"] as MutableSet<MPC_orbitalMissileLauncher>
        }
    }

    abstract var maxMissilesLoaded: Float
    open var missilesLoaded: Float = maxMissilesLoaded
        get() {
            field = field.coerceAtMost(maxMissilesLoaded)
            return field
        }
    open var missileReloadInterval = IntervalUtil(14f, 15f) // days
    val missilesInFlight = HashMap<MPC_aegisMissileEntityPlugin, CampaignFleetAPI>()

    // scales detection rate up to 100% from min to max
    open var minSensorProfile = 300f
    open var maxSensorProfile = 1300f
    open var maxDetectionRate = 0.3f // per second at max
    open var detectionDecayRate = 0.05f
    val detection = HashMap<CampaignFleetAPI, Float>()
    val detectionReminderInterval = IntervalUtil(0.5f, 0.6f) // secs

    val checkInterval = IntervalUtil(0.8f, 0.9f) // days

    open var renderTerrain: Boolean = true
    open var useTerrain: Boolean = true
    var terrainRing: MPC_missileTargettingTerrain? = null

    var reloadRateMult: Float = 1f
        get() {
            if (field == null) field = 1f
            return field
        }

    abstract fun getHost(): SectorEntityToken

    open fun apply() {
        getMissileLaunchers() += this
        if (useTerrain) {
            createTerrain()
        }

    }
    open fun unapply() {
        getMissileLaunchers() -= this
        deleteTerrain()
    }

    fun createTerrain() {
        if (terrainRing != null) return
        val host = getHost() ?: return
        val params = MPC_missileTargettingTerrain.MissileTerrainParams(
            getMaxTargettingRange() * 50f,
            0f,
            getHost(),
            this
        )
        val terrain: CampaignTerrainAPI = host.containingLocation.addTerrain(
            "MPC_missileTerrain",
            params
        ) as CampaignTerrainAPI
        terrain.setLocation(getHost().location.x, getHost().location.y)
        terrainRing = terrain.plugin as MPC_missileTargettingTerrain?
    }

    fun deleteTerrain() {
        if (terrainRing == null) return
        terrainRing!!.entity.containingLocation.removeEntity(terrainRing!!.entity)
        terrainRing = null
    }

    override fun startImpl() {
        getHost()?.addScript(this)
        apply()
    }

    override fun stopImpl() {
        getHost()?.removeScript(this)
        unapply()
    }

    open fun reloadMissiles(days: Float) {
        val newDays = days * reloadRateMult
        missileReloadInterval.advance(newDays)
        if (missileReloadInterval.intervalElapsed()) {
            missilesLoaded = (missilesLoaded + 1).coerceAtMost(maxMissilesLoaded)
        }
    }

    override fun advance(amount: Float) {

        if (terrainRing != null) {
            val terrainEntity = terrainRing!!.entity
            terrainEntity.setLocation(getHost().location.x, getHost().location.y)
        }

        for (missile in missilesInFlight.toMutableMap().keys) {
            if (missile.ending) {
                missilesInFlight -= missile
            }
        }

        val days = Misc.getDays(amount)

        reloadMissiles(days)
        if (usingDetection()) {
            incrementDetections(amount)
            detectionReminderInterval.advance(amount)
            val canRemind = detectionReminderInterval.intervalElapsed()
            if (canRemind && getHost()?.containingLocation?.isCurrentLocation == true) {
                val test = 5
                val engine = CampaignEngine.getInstance() ?: return
                var target = engine.mousedOverEntity as? CampaignFleetAPI
                if (target != null) {
                    val amount = getDetectionLevel(target)
                    target.addFloatingText(
                        "Missile detection level: ${niko_MPC_stringUtils.toPercent(amount.roundNumTo(2))}",
                        Misc.getNegativeHighlightColor(),
                        0.3f
                    )
                }
                /*for (entry in detection) {
                    val amount = entry.value
                    if (amount <= 0f) continue // sanity
                    val fleet = entry.key
                    if (fleet.isPlayerFleet) continue

                    val sensorLevel = fleet.visibilityLevelToPlayerFleet
                    if (sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS || sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
                        fleet.addFloatingText(
                            "Missile detection level: ${niko_MPC_stringUtils.toPercent(amount.roundNumTo(1))}",
                            Misc.getNegativeHighlightColor(),
                            0.4f
                        )
                    }
                }*/
            }
        }

         checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {
            if (readyToFire()) {
                val possibleTargets = getPossibleTargets()
                if (possibleTargets.isEmpty()) return

                while (readyToFire() && possibleTargets.isNotEmpty()) {
                    fireOn(possibleTargets.random(), possibleTargets)
                }
            }
        }
    }

    fun usingDetection(): Boolean {
        return maxDetectionRate > 0f
    }

    private fun sanitizeDetected() {
        val host = getHost()
        for (fleet in detection.keys.toMutableSet()) {
            if (fleet.isExpired || host != null && host.containingLocation != fleet.containingLocation) {
                detection -= fleet
            }
        }
    }

    abstract fun getMaxTargettingRange(): Float

    open fun getDetectionIncrement(fleet: CampaignFleetAPI, amount: Float): Float {
        var adjust: Float
        val profile = fleet.detectedRangeMod.computeEffective(fleet.sensorProfile)
        val effectiveProfile = (profile - minSensorProfile)
        var mult = (effectiveProfile / (maxSensorProfile - minSensorProfile)).coerceAtLeast(0f).coerceAtMost(1f)
        adjust = maxDetectionRate * mult

        if (!canTargetFleet(fleet)) {
            mult = 0f
        }

        if (mult == 0f) {
            adjust = -detectionDecayRate
        }
        adjust *= amount
        return adjust
    }

    open fun incrementDetections(amount: Float) {
        val host = getHost() ?: return
        sanitizeDetected()
        for (fleet in host.containingLocation.fleets) {
            if (fleet == host) continue

            val adjust = getDetectionIncrement(fleet, amount)

            var entry = detection[fleet]
            if (entry == null) {
                if (adjust <= 0f) continue
                detection[fleet] = 0f
                entry = detection[fleet]
            }
            detection[fleet] = (getDetectionLevel(fleet) + adjust).coerceAtMost(1f)

            val sensorLevel = fleet.visibilityLevelToPlayerFleet
            if (sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS || sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
                if (getDetectionLevel(fleet) >= 1f) {
                    if (!fleet.memoryWithoutUpdate.getBoolean("\$MPC_detectedPinged")) {
                        fleet.memoryWithoutUpdate["\$MPC_detectedPinged"] = true
                        fleet.addFloatingText("Detected by missile platform!", Misc.getNegativeHighlightColor(), 1f)
                    }
                } else if (fleet.memoryWithoutUpdate.getBoolean("\$MPC_detectedPinged")) {
                    fleet.memoryWithoutUpdate.unset("\$MPC_detectedPinged")
                }
            }

            if (getDetectionLevel(fleet) <= 0f) {
                detection -= fleet
                continue
            }
        }
    }

    fun getDetectionLevel(target: CampaignFleetAPI): Float {
        return detection[target] ?: 0f
    }

    fun getPossibleTargets(): MutableList<SectorEntityToken> {
        val targets = ArrayList<SectorEntityToken>()
        val host = getHost() ?: return targets

        for (fleet in host.containingLocation.fleets) {
            val detectionLevel = getDetectionLevel(fleet)
            if (usingDetection() && detectionLevel < 1f) continue
            if (canTargetFleet(fleet)) targets += fleet
        }

        return targets
    }

    open fun canTargetFleet(fleet: CampaignFleetAPI): Boolean {
        if (fleet in missilesInFlight.values) return false // no spam
        val host = getHost()
        if (host != null) {
            val dist = MathUtils.getDistance(host, fleet)
            if (dist > getMaxTargettingRange()) return false
        }

        return isHostileTo(fleet)
    }

    fun isHostileTo(fleet: CampaignFleetAPI): Boolean {
        return getFaction().id == Factions.NEUTRAL || fleet.faction.isHostileTo(getFaction())
    }

    open fun readyToFire(): Boolean {
        return missilesLoaded >= 1f
    }

    open fun fireOn(target: SectorEntityToken, possibleTargets: MutableList<SectorEntityToken>): Boolean {
        if (target is CampaignFleetAPI && !canTargetFleet(target)) return false

        val spec = getSpec(target)
        createMissile(spec, target)

        missilesLoaded = (missilesLoaded - 1).coerceAtLeast(0f)
        possibleTargets -= target
        return true
    }

    abstract fun createMissile(spec: MPC_missileStrikeAbility.Missile, target: SectorEntityToken): MPC_aegisMissileEntityPlugin

    abstract fun getSpec(target: SectorEntityToken): MPC_missileStrikeAbility.Missile

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

    override fun runWhilePaused(): Boolean {
        return false
    }

    abstract fun getTerrainName(): String?
    abstract fun getDescNoun(): String?
}