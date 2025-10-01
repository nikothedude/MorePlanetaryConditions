package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.Global
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.loading.CampaignPingSpec
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.plugins.MagicCampaignTrailPlugin
import java.awt.Color

class MPC_aegisMissileEntityPlugin: BaseCustomEntityPlugin() {

    companion object {
        fun getDamageStringAndColor(damage: ExplosionEntityPlugin.ExplosionFleetDamage): Pair<String, Color> {
            val damageString: String
            val damageColor: Color
            when (damage) {
                ExplosionEntityPlugin.ExplosionFleetDamage.NONE -> {
                    damageString = "None"
                    damageColor = Misc.getGrayColor()
                }
                ExplosionEntityPlugin.ExplosionFleetDamage.LOW -> {
                    damageString = "Moderate"
                    damageColor = Misc.getHighlightColor()
                }
                ExplosionEntityPlugin.ExplosionFleetDamage.MEDIUM -> {
                    damageString = "High"
                    damageColor = Misc.getHighlightColor()
                }
                ExplosionEntityPlugin.ExplosionFleetDamage.HIGH -> {
                    damageString = "Very High"
                    damageColor = Misc.getNegativeHighlightColor()
                }
                ExplosionEntityPlugin.ExplosionFleetDamage.EXTREME -> {
                    damageString = "Extreme"
                    damageColor = Misc.getNegativeHighlightColor()
                }
            }

            return Pair(damageString, damageColor)
        }

        fun createNew(params: MissileParams, containing: LocationAPI, loc: Vector2f, facing: Float): MPC_aegisMissileEntityPlugin {
            val name = params.name
            val id = params.id
            val entity = containing.addCustomEntity(id, name, "MPC_cruiseMissile", params.faction, params)

            entity.isDiscoverable = false
            entity.sensorProfile = params.sensorProfile
            entity.facing = facing

            entity.setLocation(loc.x, loc.y)

            val plugin = entity.customPlugin as MPC_aegisMissileEntityPlugin
            plugin.target = params.target
            return plugin
        }

        fun createNewFromMarket(
            market: MarketAPI,
            params: MissileParams
        ): MPC_aegisMissileEntityPlugin {
            if (params.name == null) params.name = "${market.faction.entityNamePrefix} Cruise Missile (${market.name})"
            params.originMarket = market
            params.faction = market.factionId

            return createNewFromEntity(market.primaryEntity, params)
        }

        fun createNewFromEntity(
            entity: SectorEntityToken,
            params: MissileParams
        ): MPC_aegisMissileEntityPlugin {
            val loc = entity.location
            val effectiveRadius = entity.radius / 2
            val randX = MathUtils.getRandomNumberInRange(-effectiveRadius, effectiveRadius)
            val randY = MathUtils.getRandomNumberInRange(-effectiveRadius, effectiveRadius)
            val randomSpot = Vector2f(loc).translate(randX, randY)
            val facName = if (entity.faction.id == Factions.NEUTRAL) "" else "${entity.faction.entityNamePrefix}"
            val ourName = params.name ?: "$facName Cruise Missile"
            params.name = ourName
            params.origin = entity
            if (params.faction == null) params.faction = entity.faction.id

            val facing = if (params.useTargetFacing) VectorUtils.getAngle(entity.location, params.target.location) else entity.facing

            val plugin = createNew(
                params,
                entity.containingLocation,
                randomSpot,
                facing
            )
            return plugin
        }

        const val DIST_FOR_FINAL_APPROACH = 300f
        const val TIME_FOR_FINAL_APPROACH_TO_BEGIN = 4f
        val defaultTrailColor = Color(255, 200, 50, 255)
    }

    var target: SectorEntityToken? = null

    var currSpeed = 0f
    var timeElapsed = 0f

    var wasDetected = false

    val pingTimer = IntervalUtil(0.7f, 0.7f) // seconds

    var playedSound = false

    var finalApproach = false
    var missed = false

    var ending = false

    var profileTooLowsecs = 0f

    lateinit var params: MissileParams
    lateinit var lifespan: IntervalUtil // days

    data class MissileParams(
        var target: SectorEntityToken,
        val id: String,
        var origin: SectorEntityToken?,
        var explosionParams: ExplosionEntityPlugin.ExplosionParams,
        var name: String? = null,
        var faction: String = Factions.NEUTRAL,
        val sensorProfile: Float = 5000f,
        var originMarket: MarketAPI? = null,
        var lifespan: Float = 10f,

        /// If null, will just explode on the player fleet
        var combatVariant: String? = null,
        var turnRate: Float = 12f,
        var speed: Float = 600f,
        var accelTime: Float = 3f,

        var trailColor: Color = defaultTrailColor,

        var onHitEffect: MissileOnHitEffect? = null,

        var useTargetFacing: Boolean = true,

        var losesLockUnderProfile: Float = 200f,
        var secsToLoseLock: Float = 2f
    )

    abstract class MissileOnHitEffect() {
        abstract fun execute(hit: SectorEntityToken)
    }

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        if (pluginParams !is MissileParams) throw RuntimeException("incorrect missile params!")
        params = pluginParams
        lifespan = IntervalUtil(params.lifespan, params.lifespan)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (entity.isExpired || !entity.isAlive || ending) return
        if (target != null && (target!!.isExpired || target!!.containingLocation != entity.containingLocation)) {
            target = null
        }
        val days = Misc.getDays(amount)
        lifespan.advance(days)
        if (lifespan.intervalElapsed()) {
            Misc.fadeAndExpire(entity, 1f)
            ending = true

            return
        }

        checkLockStatus(amount)
        homeInOnTarget(amount)

        if (!playedSound) {
            Global.getSoundPlayer().playSound("atropos_fire", MathUtils.getRandomNumberInRange(0.9f, 1.1f), 5f, entity?.location, Misc.ZERO)
            playedSound = true
        }

        val sensorLevel = entity.visibilityLevelToPlayerFleet
        pingTimer.advance(amount)
        if (sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS || sensorLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
            if (!wasDetected) {
                wasDetected = true

                if (target?.isPlayerFleet == true && isHostileToPlayer()) {
                    target!!.addFloatingText("Missile alert!", Misc.getNegativeHighlightColor(), 1f, true)
                }
            }

            if (pingTimer.intervalElapsed() && isHostileToPlayer()) {
                firePing()
            }
            addTrailToProj()
        }


        val hit = checkContactWithAllTargets()
        if (hit != null) {
            performOnHitEffects(hit)
            explode(hit)

            return
        }

        /*if (debugprobe == null) {
            debugprobe = entity.containingLocation.addCustomEntity(
                "DEBUGPROBE",
                "TARGET",
                Entities.GENERIC_PROBE,
                Factions.NEUTRAL
            )
        }

        val playerFleet = Global.getSector().playerFleet
        var targetLoc = Vector2f(playerFleet.location)
        val dist = MathUtils.getDistance(entity, playerFleet)
        val finalApproach = dist <= 600f
        val targetVel = playerFleet.velocity
        //val targetLocReal = playerFleet.location


        val currSlope = (targetLoc.y - entity.location.y) / (targetLoc.x - entity.location.x)
        var maxMove = 200f * amount
        if (finalApproach) {
            maxMove *= 2f
        }

        val targetDest = playerFleet.moveDestination
        val destAngle = targetDest?.let { VectorUtils.getAngle(playerFleet.location, targetDest) }

        val currVel = entity.velocity
        val currVelAngle = Misc.getAngleInDegrees(currVel)
        val targetVelAngle = Misc.getAngleInDegrees(targetVel)
        val diff = targetVelAngle - currVelAngle
        val finalTargetVelAngle = Misc.normalizeAngle(targetVelAngle - diff)
        val targetVelLoc = Misc.getUnitVectorAtDegreeAngle(finalTargetVelAngle)
        if (destAngle != null) {
            //Misc.rotateAroundOrigin(targetLoc, destAngle, playerFleet.location)
        }
        targetVelLoc.scale(playerFleet.velocity.length() * (dist * 0.0003f))
        targetLoc = targetLoc.translate(targetVelLoc.x, targetVelLoc.y)
        debugprobe?.setLocation(targetLoc!!.x, targetLoc!!.y)

        val angle = Misc.getAngleInDegrees(entity.location, targetLoc)

        val turn = Misc.getClosestTurnDirection(entity.facing, angle)
        val facingAdjustment = turn.coerceAtMost(maxMove)
        entity.facing += facingAdjustment

        /*val targetFacing = playerFleet.facing
        val facingDiff = targetFacing - entity.facing
        val adjustment = -facingDiff.coerceAtMost(maxMove).coerceAtLeast(-maxMove)
        entity.facing = Misc.normalizeAngle(entity.facing + adjustment)*/


        /*if (true) {

            val slopeDiff = currSlope - lastSlope
            val proportionalRate = 1f

            val adjustment = -(slopeDiff * proportionalRate).coerceAtMost(maxMove)
            if (adjustment != 0f) {
                entity.facing = Misc.normalizeAngle(entity.facing + adjustment)
                if (abs(adjustment) >= 1f) {
                    Global.getSector().campaignUI.addMessage(
                        "${adjustment} change, ${entity.facing} facing"
                    )
                }
                entity.name = "${adjustment} change, ${entity.facing} facing"
            }
        } else {
            val turn = Misc.getClosestTurnDirection(entity.facing, angle)
            val facingAdjustment = turn.coerceAtMost(maxMove)
            entity.facing += facingAdjustment
        }*/
        lastSlope = currSlope

        var mult = 1f
        /*val turn = Misc.getClosestTurnDirection(entity.facing, angle)
        val facingAdjustment = turn.coerceAtMost(maxMove)
        entity.facing += facingAdjustment*/

        val angleDiff = Misc.getAngleDiff(entity.facing, Misc.getAngleInDegrees(entity.location, targetLoc))
        if (finalApproach || angleDiff <= 10f && !reorienting) {
            val newAngle = Misc.getAngleInDegrees(entity.location, targetLoc)
            val newDiff = Misc.getAngleDiff(entity.facing, Misc.getAngleInDegrees(entity.location, targetLoc))

            val advAmount = 120f * amount
            val valAdjust = MathUtils.getPointOnCircumference(Misc.ZERO, advAmount, entity.facing)

            val angle = Misc.getAngleInDegrees(valAdjust)
            val currAngle = Misc.getAngleInDegrees(entity.velocity)
            val valDiff = Misc.getAngleDiff(currAngle, angle)

            if (valDiff >= 50f) {
                mult = 4f
            }

            entity.velocity.translate(valAdjust.x * mult, valAdjust.y * mult)
        } else {
            reorienting = true
            entity.velocity.scale(0.99f)

            if (entity.velocity.length() <= 10f && angleDiff <= 3f) {
                reorienting = false
            }
        }
        val maxormin = 1000f

        entity.velocity.x = entity.velocity.x.coerceAtMost(maxormin).coerceAtLeast(-maxormin)
        entity.velocity.y = entity.velocity.y.coerceAtMost(maxormin).coerceAtLeast(-maxormin)*/

    }

    private fun checkLockStatus(amount: Float) {
        if (target == null) return
        if (target!!.memoryWithoutUpdate.getBoolean("\$MPC_lastSeenLoc")) return
        val profile = target!!.detectedRangeMod.computeEffective(target!!.sensorProfile)
        if (profile < params.losesLockUnderProfile) {
            profileTooLowsecs += amount

            if (profileTooLowsecs >= params.secsToLoseLock) {
                loseLock()
                return
            }
        } else {
            profileTooLowsecs = 0f
        }
    }

    private fun loseLock() {
        if (target == null) return
        val visibilityLevel = entity.visibilityLevelToPlayerFleet
        if (visibilityLevel == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
            val color: Color
            val playerFleet = Global.getSector().playerFleet
            if (target == playerFleet) {
                color = Misc.getPositiveHighlightColor()
            } else if (target is CampaignFleetAPI && (target as CampaignFleetAPI).isHostileTo(playerFleet)) {
                color = Misc.getNegativeHighlightColor()
            } else {
                color = Misc.getHighlightColor()
            }
            entity.addFloatingText(
                "Lost lock!",
                color,
                1f
            )
            val lastSeenLoc = target!!.containingLocation.createToken(target!!.location.x, target!!.location.y)
            lastSeenLoc.memoryWithoutUpdate["\$MPC_lastSeenLoc"] = true
            target = lastSeenLoc
            profileTooLowsecs = 0f
        }
    }

    private fun performOnHitEffects(hit: SectorEntityToken) {

        if (params.onHitEffect != null) {
            params.onHitEffect!!.execute(hit)
        }

        if (hit.isPlayerFleet) {
            // todo highfleet missile defense
        }

        return
    }

    fun explode(hit: SectorEntityToken?) {

        if (hit == null || !hit.isPlayerFleet || params.combatVariant == null) {
            // do explosioin stuff here
            var targetLoc = Vector2f(entity.location)
            /*if (hit != null) {
                val diffX = hit.location.x - targetLoc.x
                val diffY = hit.location.y - targetLoc.y

                targetLoc.x += (diffX)
                targetLoc.y += (diffY)

            }*/
            params.explosionParams.loc = targetLoc
            val explosion = entity.containingLocation.addCustomEntity(
                "${entity.id}_explosion",
                null,
                "MPC_missileExplosion",
                entity.faction.id,
                params.explosionParams
            )
            explosion.setLocation(targetLoc.x, targetLoc.y)
            if (hit != null && hit is CampaignFleetAPI) {
                val plugin = explosion.customPlugin as MPC_missileExplosionPlugin
                plugin.applyDamageToFleet(hit, 1f)
                plugin.getAlreadyDamaged() += hit.id
            }
        }

        Misc.fadeAndExpire(entity, 0.2f)
        ending = true
    }

    private fun checkContactWithAllTargets(): SectorEntityToken? {
        if (target != null && checkContact(target!!)) return target
        for (fleet in entity.containingLocation.fleets.filter { it.faction.isHostileTo(entity.faction) }) {
            if (checkContact(fleet)) return fleet
        }

        return null
    }

    private fun checkContact(target: SectorEntityToken): Boolean {
        return MathUtils.getDistance(entity, target) <= 0f
    }

    val pingColor = Color(222, 86, 0, 255)
    private fun firePing() {
        val custom = CampaignPingSpec()
        custom.width = 15f
        custom.range = 300f
        custom.duration = 0.5f
        custom.alphaMult = 1f
        custom.inFraction = 0.1f
        custom.num = 3
        custom.delay = 0.1f
        custom.color = pingColor
        //custom.sounds.add("default_campaign_ping")
        Global.getSector().addPing(entity, custom)
        Global.getSoundPlayer().playSound("default_campaign_ping", 1f, 5f, entity.location, Misc.ZERO)
    }

    fun isHostileToFleet(fleet: CampaignFleetAPI): Boolean {
        return entity.faction.getRelationshipLevel(fleet.faction).isAtBest(RepLevel.HOSTILE)
    }

    fun isHostileToPlayer(): Boolean {
        return target == Global.getSector().playerFleet || isHostileToFleet(Global.getSector().playerFleet)
    }

    private fun homeInOnTarget(amount: Float) {
        // code based heavily on indevo arty stations but with many additions and changes and fixes
        timeElapsed += amount
        currSpeed = (timeElapsed / params.accelTime).coerceAtMost(1f)

        if (target != null) {
            val targetDist = MathUtils.getDistance(entity, target)
            if (timeElapsed >= TIME_FOR_FINAL_APPROACH_TO_BEGIN && targetDist <= DIST_FOR_FINAL_APPROACH) {
                finalApproach = true
            }
            if (finalApproach && targetDist >= DIST_FOR_FINAL_APPROACH + 100f) {
                missed = true
            }
        }
        val dist = params.speed * amount

        //direction
        var nextAngle: Float
        if (missed || target == null) {
            nextAngle = entity.facing
        } else {
            val currentTarget = target
            val targetAngle = VectorUtils.getAngle(entity.location, currentTarget!!.location)
            val moveAngle = entity.facing
            val angleDiff = Misc.getAngleDiff(targetAngle, moveAngle)
            var turn = params.turnRate * amount
            turn = turn.coerceAtMost(angleDiff)
            if (moveAngle > 180) {
                val inverted = moveAngle - 180f // dont normalize this.
                val targetIsLeft = !(targetAngle < moveAngle && targetAngle > inverted)
                val targetSign = (if (targetIsLeft) 1 else -1)
                nextAngle = entity.facing + turn * targetSign
            } else {
                val inverted = moveAngle + 180f
                val targetIsLeft = targetAngle > moveAngle && targetAngle < inverted
                val targetSign = (if (targetIsLeft) 1 else -1)
                nextAngle = entity.facing + turn * targetSign
            }
        }

        nextAngle = Misc.normalizeAngle(nextAngle)

        val nextPos = MathUtils.getPointOnCircumference(entity.location, dist, nextAngle)
        val diffX = (nextPos.x - entity.location.x) * currSpeed
        val diffY = (nextPos.y - entity.location.y) * currSpeed

        val newLoc = Vector2f(entity.location).translate(diffX, diffY) as Vector2f
        entity.setLocation(newLoc.x, newLoc.y)
        if (!missed) {
            entity.facing = nextAngle
        }
    }

    val trailID = MagicCampaignTrailPlugin.getUniqueID()
    fun addTrailToProj() {
        val trailColor = if (isHostileToPlayer()) Misc.getNegativeHighlightColor() else params.trailColor
        val playerFleet = Global.getSector().playerFleet
        MagicCampaignTrailPlugin.addTrailMemberSimple(
            entity,
            trailID,
            Global.getSettings().getSprite("fx", "base_trail_smoke"),
            entity.location,
            0f,
            entity.facing,
            16f,
            1f,
            trailColor,
            0.8f,
            1.2f,
            true,
            Vector2f(0f, 0f)
        )
    }

    fun getEntityExternal(): SectorEntityToken = entity

    fun addTargetSection(tooltip: TooltipMakerAPI) {
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addSpacer(5f)
        //tooltip.setGridFontSmallInsignia()

        val targetColor: Color
        val targetName: String

        if (target != null) {
            if (target!!.memoryWithoutUpdate.getBoolean("\$MPC_lastSeenLoc")) {
                targetColor = Misc.getGrayColor()
                targetName = "Last known position"
            } else {

                val targetSensorLevel = target!!.visibilityLevelToPlayerFleet
                when (targetSensorLevel) {
                    SectorEntityToken.VisibilityLevel.NONE -> {
                        targetColor = Misc.getGrayColor()
                        targetName = "Unknown"
                    }

                    SectorEntityToken.VisibilityLevel.SENSOR_CONTACT -> {
                        targetColor = Misc.getGrayColor()
                        targetName = "Unknown Entity"
                    }

                    SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS -> {
                        targetColor = Misc.getGrayColor()
                        targetName = "Unknown Entity"
                    }

                    SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS -> {
                        targetColor = target!!.faction.baseUIColor
                        targetName = if (target!!.isPlayerFleet) "Your fleet" else target!!.fullName
                    }
                }
            }
        } else {
            targetColor = Misc.getGrayColor()
            targetName = "None"
        }

        tooltip.addPara(
            "Target: %s",
            5f,
            targetColor,
            targetName
        )

        val damagePair = getDamageStringAndColor(params.explosionParams.damage)
        val damageString = damagePair.first
        val damageColor = damagePair.second

        tooltip.addPara(
            "Loses lock under %s sensor profile",
            5f,
            Misc.getHighlightColor(),
            params.losesLockUnderProfile.roundNumTo(1).trimHangingZero().toString()
        )

        //tooltip.setParaFontDefault()
        tooltip.setBulletedListMode(null)
    }

    override fun appendToCampaignTooltip(tooltip: TooltipMakerAPI?, level: SectorEntityToken.VisibilityLevel?) {
        super.appendToCampaignTooltip(tooltip, level)

        if (tooltip == null) return
        if (level == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {
            addTargetSection(tooltip)
        }
    }

    override fun createMapTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createMapTooltip(tooltip, expanded)

        if (tooltip == null) return

        tooltip.addPara(entity.name, 0f).color = entity.faction.baseUIColor
        addTargetSection(tooltip)
    }

    override fun hasCustomMapTooltip(): Boolean {
        return true
    }

}