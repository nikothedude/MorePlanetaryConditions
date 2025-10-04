package data.scripts.campaign.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.ai.ModularFleetAI
import data.hullmods.MPC_missileCarrier
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.abilities.MPC_missileStrikeAbility.Missile.Companion.pickable
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisMissileEntityPlugin
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import sound.int
import java.awt.Color
import kotlin.math.PI
import kotlin.math.max

class MPC_missileStrikeAbility: BaseDurationAbility() {

    // high cooldown, very expensive in terms of heavy armaments, and needs a missile carrier

    // if reading bc you want to copy; WWLB is CC BY-NC, meaning you can copy this and remix it as long as you dont profit off it

    enum class Missile(val nameColor: Color, val frontEndName: String, val availableToAbility: Boolean = true, val combatVariant: String? = null) {
        EXPLOSIVE(Color(255, 90, 60), "Explosive (Low-Yield)") {
            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val params = super.adjustMissileParams(params)

                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    Color(255, 90, 60),
                    source.containingLocation,
                    source.location,
                    100f,
                    0.4f
                )
                explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW

                params.explosionParams = explosionParams

                return params
            }

            override fun getMaxSpeed(): Float {
                return 800f
            }

            override fun getTurnRate(): Float {
                return 18f
            }

            override fun getAccelTime(): Float {
                return 6f
            }

            override fun getArmamentCost(): Float {
                return 325f
            }

            override fun getFuelCost(): Float {
                return 300f
            }

            override fun getCooldownTimeDays(): Float {
                return 10f
            }

            override fun getSpeedString(): String = "Fast"
            override fun getSpeedHl(): Color = Misc.getHighlightColor()
            override fun getManeuverString(): String = "Bad"
            override fun getManeuverHl(): Color = Misc.getNegativeHighlightColor()
            override fun getBaseRepLoss(): Float {
                return 10f
            }
        },
        EXPLOSIVE_HEAVY(Color(255, 40, 20), "Explosive (High-Yield)") {
            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val params = super.adjustMissileParams(params)

                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    Color(255, 90, 60),
                    source.containingLocation,
                    source.location,
                    700f,
                    1f
                )
                explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.HIGH

                params.explosionParams = explosionParams

                return params
            }

            override fun getMaxSpeed(): Float {
                return 400f
            }

            override fun getTurnRate(): Float {
                return 16f
            }

            override fun getAccelTime(): Float {
                return 12f
            }

            override fun getArmamentCost(): Float {
                return 1500f
            }

            override fun getFuelCost(): Float {
                return 2000f
            }

            override fun getCooldownTimeDays(): Float {
                return 60f
            }

            override fun getSpeedString(): String = "Slow"
            override fun getSpeedHl(): Color = Misc.getNegativeHighlightColor()
            override fun getManeuverString(): String = "Terrible"
            override fun getManeuverHl(): Color = Misc.getNegativeHighlightColor()
            override fun getBaseRepLoss(): Float {
                return 20f
            }

            override fun getTrailColor(): Color {
                return nameColor
            }

            override fun getBaseEntityName(): String {
                return "Fusion Torpedo"
            }

            val spNeeded = 1

            override fun addAfterStatsSection(
                tooltip: TooltipMakerAPI,
                params: MPC_aegisMissileEntityPlugin.MissileParams,
                isForAbility: Boolean
            ) {
                super.addAfterStatsSection(tooltip, params, isForAbility)

                tooltip.addPara(
                    "Costs %s to fire. Requires %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "$spNeeded story points", "missile specialization"
                ).setHighlightColors(
                    Misc.getStoryOptionColor(),
                    Misc.getHighlightColor()
                )

                tooltip.addPara(
                    "Has a large blast radius, and can hit multiple fleets in the blast.",
                    5f
                )
            }
            fun getCantFireReason(): Pair<String, Color>? {
                val sp = Global.getSector().playerStats.storyPoints
                if (!Global.getSector().playerStats.hasSkill(Skills.MISSILE_SPECIALIZATION)) return Pair("Needs missile specialization", Misc.getNegativeHighlightColor())
                if (sp <= spNeeded) return Pair("Not enough story points, $spNeeded needed", Misc.getStoryOptionColor())

                return null
            }

            override fun getNameSuffix(name: String): String {
                val reason = getCantFireReason()
                if (reason != null) {
                    return " (${reason.first})"
                }
                return ""
            }

            override fun canFireFromAbility(): Boolean {
                return getCantFireReason() == null
            }

            override fun postAbilityUse() {
                super.postAbilityUse()

                Global.getSector().playerStats.spendStoryPoints(
                    spNeeded,
                    true,
                    null,
                    true,
                    "Fired a ${getBaseEntityName()}"
                )
                Global.getSoundPlayer().playUISound(
                    "ui_char_spent_story_point",
                    1f,
                    1f
                )
                Global.getSector().campaignUI.addMessage("Spent $spNeeded story points!", Misc.getStoryOptionColor())
            }
        },
        INTERDICT(Color(134, 255, 228), "Interdictor") {
            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val params = super.adjustMissileParams(params)
                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    Color(134, 255, 228),
                    source.containingLocation,
                    source.location,
                    75f,
                    0.4f
                )

                params.explosionParams = explosionParams

                return params
            }

            override fun getMaxSpeed(): Float {
                return 1000f
            }

            override fun getTurnRate(): Float {
                return 70f
            }

            override fun getAccelTime(): Float {
                return 0.5f
            }

            override fun getArmamentCost(): Float {
                return 125f
            }

            override fun getFuelCost(): Float {
                return 75f
            }

            override fun getCooldownTimeDays(): Float {
                return 3f
            }

            override fun getTrailColor(): Color {
                return nameColor
            }

            override fun getSpeedString(): String = "Very Fast"
            override fun getSpeedHl(): Color = Misc.getPositiveHighlightColor()
            override fun getManeuverString(): String = "Excellent"
            override fun getManeuverHl(): Color = Misc.getPositiveHighlightColor()
            override fun getBaseRepLoss(): Float {
                return 2f
            }

            override fun getBaseEntityName(): String {
                return "Interdictor"
            }

            fun getEffectiveInterdictStrength(other: SectorEntityToken): Float {
                val offense: Float = 1000f
                val defense: Float = other.sensorRangeMod.computeEffective(other.sensorStrength)
                val diff = offense - defense

                val extra = diff / InterdictionPulseAbility.STRENGTH_PER_SECOND

                var total = getBaseInterdictTime() + extra
                if (total < 0f) total = 0f
                return total.coerceAtMost(getBaseInterdictTime()) // / Global.getSector().getClock().getSecondsPerDay();
            }

            override fun getOnHitEffect(): MPC_aegisMissileEntityPlugin.MissileOnHitEffect? {

                class InterdictEffect: MPC_aegisMissileEntityPlugin.MissileOnHitEffect() {
                    override fun execute(source: SectorEntityToken?, hit: SectorEntityToken) {
                        var interdictTime = getEffectiveInterdictStrength(hit)
                        if (interdictTime > 0 && interdictTime < 1f) interdictTime = 1f

                        val vis: VisibilityLevel? = hit.visibilityLevelToPlayerFleet
                        if (vis == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS || vis == VisibilityLevel.COMPOSITION_DETAILS ||
                            (vis == VisibilityLevel.SENSOR_CONTACT)
                        ) {
                            if (interdictTime <= 0) {
                                hit.addFloatingText(
                                    "Interdict avoided!",
                                    hit.faction.baseUIColor,
                                    1f,
                                    true
                                )
                                return
                            } else {
                                hit.addFloatingText(
                                    "Interdict! (" + Math.round(interdictTime) + "s)",
                                    hit.faction.baseUIColor,
                                    1f,
                                    true
                                )
                            }
                        }

                        val interdictDays = interdictTime / Global.getSector().clock.secondsPerDay

                        for (ability in hit.abilities.values) {
                            if (!ability.spec.hasTag(Abilities.TAG_BURN + "+") && !ability.spec
                                    .hasTag(Abilities.TAG_DISABLED_BY_INTERDICT) && (ability.id != Abilities.INTERDICTION_PULSE)
                            ) continue

                            val origCooldown: Float = ability.cooldownLeft
                            var extra = 0f
                            if (ability.isActiveOrInProgress) {
                                extra += ability.spec.deactivationCooldown * ability.progressFraction
                                ability.deactivate()
                            }

                            if (!ability.spec.hasTag(Abilities.TAG_BURN + "+")) continue

                            var cooldown = interdictDays
                            //cooldown = Math.max(cooldown, origCooldown);
                            cooldown += origCooldown
                            cooldown += extra
                            val max = max(ability.spec.deactivationCooldown, 2f)
                            if (cooldown > max) cooldown = max
                            ability.cooldownLeft = cooldown
                        }
                    }
                }

                return InterdictEffect()
            }

            fun getBaseInterdictTime(): Float = 6f
            override fun addAfterStatsSection(
                tooltip: TooltipMakerAPI,
                params: MPC_aegisMissileEntityPlugin.MissileParams,
                isForAbility: Boolean
            ) {
                super.addAfterStatsSection(tooltip, params, isForAbility)

                tooltip.addPara(
                    "Interdicts the hit fleet for %s seconds on detonation.",
                    5f,
                    Misc.getHighlightColor(),
                    "${getBaseInterdictTime().trimHangingZero()}"
                )
            }
        },
        SENSOR(Color(147, 20, 170), "Obscure") {
            override fun getArmamentCost(): Float {
                return 200f
            }

            override fun getFuelCost(): Float = 200f

            override fun getCooldownTimeDays(): Float = 8f

            override fun getMaxSpeed(): Float {
                return 700f
            }

            override fun getTurnRate(): Float {
                return 35f
            }

            override fun getSpeedString(): String {
                return "Average"
            }

            override fun getManeuverString(): String {
                return "Average"
            }

            override fun getSpeedHl(): Color {
                return Misc.getHighlightColor()
            }

            override fun getManeuverHl(): Color {
                return Misc.getHighlightColor()
            }

            override fun getBaseRepLoss(): Float {
                return 5f
            }

            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val params = super.adjustMissileParams(params)
                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                   nameColor,
                    source.containingLocation,
                    source.location,
                    75f,
                    0.2f
                )

                params.explosionParams = explosionParams

                return params
            }

            override fun getTrailColor(): Color {
                return nameColor
            }

            override fun getBaseEntityName(): String {
                return "Obscura"
            }

            override fun getOnHitEffect(): MPC_aegisMissileEntityPlugin.MissileOnHitEffect? {

                val durDays = 5f
                val detectedMult = 3f
                val sensorMult = 0.25f
                val name = "Tagging Missile"

                val id = "${name}_${Misc.genUID()}"

                class TagHitEffect: MPC_aegisMissileEntityPlugin.MissileOnHitEffect() {
                    override fun execute(
                        source: SectorEntityToken?,
                        hit: SectorEntityToken
                    ) {
                        if (hit !is CampaignFleetAPI) return

                        hit.stats.addTemporaryModMult(
                            durDays, id,
                            name, detectedMult,
                            hit.stats.detectedRangeMod
                        )
                        hit.stats.addTemporaryModMult(
                            durDays, "${id}_range",
                            name, sensorMult,
                            hit.stats.sensorRangeMod
                        )

                        for (view in hit.views) {
                            val jitterDur = (durDays * Global.getSector().clock.secondsPerDay)
                            view.setJitter(0.1f, jitterDur, nameColor, 2, 3f)
                            view.setUseCircularJitter(true)
                            view.setJitterDirection(Misc.ZERO)
                            view.setJitterLength(jitterDur)
                            view.setJitterBrightness(0.2f)
                        }
                    }
                }

                return TagHitEffect()
            }

            override fun addAfterStatsSection(
                tooltip: TooltipMakerAPI,
                params: MPC_aegisMissileEntityPlugin.MissileParams,
                isForAbility: Boolean
            ) {
                super.addAfterStatsSection(tooltip, params, isForAbility)

                tooltip.addPara(
                    "Shrouds the target in %s, reducing their sensor range by %s and increasing sensor profile by %s. Lasts for %s days.",
                    5f,
                    Misc.getHighlightColor(),
                    "ionized darkness", "75%", "300%", "3"
                )
            }

        };

        open fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
            params.spec = this

            return params
        }
        abstract fun getArmamentCost(): Float
        abstract fun getFuelCost(): Float
        abstract fun getCooldownTimeDays(): Float
        open fun canFireFromAbility(): Boolean = true
        open fun getNameSuffix(name: String): String = ""
        open fun postAbilityUse() {
            return
        }
        fun addToTooltip(tooltip: TooltipMakerAPI, params: MPC_aegisMissileEntityPlugin.MissileParams, isForAbility: Boolean) {
            val params = adjustMissileParams(params)
            val damagePair = MPC_aegisMissileEntityPlugin.getDamageStringAndColor(params.explosionParams.damage)

            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            tooltip.addPara(
                "Damage: %s",
                5f,
                damagePair.second,
                damagePair.first
            )
            tooltip.addPara(
                "Speed: %s",
                5f,
                getSpeedHl(),
                getSpeedString()
            )
            tooltip.addPara(
                "Maneuverability: %s",
                5f,
                getManeuverHl(),
                getManeuverString()
            )
            if (isForAbility) {
                val fleet = Global.getSector().playerFleet

                val armamentAmount = getArmamentCost()
                val fuelAmount = getFuelCost()

                val cargo = fleet.cargo

                val notEnoughArmaments = cargo.getCommodityQuantity(Commodities.HAND_WEAPONS) < armamentAmount
                val notEnoughFuel = cargo.getCommodityQuantity(Commodities.FUEL) < fuelAmount

                val armamentColor =
                    if (notEnoughArmaments) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
                val fuelColor = if (notEnoughFuel) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()

                tooltip.addPara(
                    "Costs %s and %s to fire",
                    10f,
                    Misc.getHighlightColor(),
                    "${armamentAmount.toInt()} heavy armaments", "${fuelAmount.toInt()} fuel"
                ).setHighlightColors(
                    armamentColor,
                    fuelColor
                )

                tooltip.addPara(
                    "Reload time: %s",
                    5f,
                    Misc.getHighlightColor(),
                    "${getCooldownTimeDays().trimHangingZero()} days"
                )
            } else {
                if (getLosesLockUnderProfile() > 0) {
                    tooltip.addPara(
                        "Loses lock under %s sensor profile",
                        5f,
                        Misc.getHighlightColor(),
                        getLosesLockUnderProfile().roundNumTo(1).trimHangingZero().toString()
                    )
                }
            }
            tooltip.setBulletedListMode(null)

            addAfterStatsSection(tooltip, params, isForAbility)
        }

        abstract fun getSpeedString(): String
        abstract fun getManeuverString(): String
        abstract fun getSpeedHl(): Color
        abstract fun getManeuverHl(): Color
        abstract fun getBaseRepLoss(): Float
        open fun getSensorProfile(): Float = 3000f
        open fun getLifespan(): Float = 10f // days

        open fun getBaseEntityName(): String = "Cruise Missile"

        open fun getLosesLockUnderProfile(): Float = 200f
        open fun getSecsToLoseLock(): Float = 2f
        open fun getOnHitEffect(): MPC_aegisMissileEntityPlugin.MissileOnHitEffect? = null
        open fun getAccelTime(): Float = 3f
        open fun getMaxSpeed(): Float = 600f
        open fun getTurnRate(): Float = 12f
        open fun getTrailColor(): Color = Color(255, 200, 50, 255)

        open fun addAfterStatsSection(
            tooltip: TooltipMakerAPI,
            params: MPC_aegisMissileEntityPlugin.MissileParams,
            isForAbility: Boolean
        ) {
            return
        }

        companion object {
            val pickable = getPickableMissiles()

            private fun getPickableMissiles(): List<Missile> {
                return Missile.entries.filter { it.availableToAbility }
            }

            fun getMissileFrom(
                missile: Missile,
                source: SectorEntityToken,
                target: SectorEntityToken,
                id: String = Misc.genUID(),
                market: MarketAPI? = null,
                faceTarget: Boolean = true,
            ): MPC_aegisMissileEntityPlugin {
                val params = getStandardAbilityParams(source, target, id)
                missile.adjustMissileParams(params)
                params.launchFacingTarget = faceTarget

                val new: MPC_aegisMissileEntityPlugin
                if (market == null) {
                    if (source is CampaignFleetAPI) {
                        new = MPC_aegisMissileEntityPlugin.createNewFromFleet(
                            source,
                            params
                        )
                    } else {
                        new = MPC_aegisMissileEntityPlugin.createNewFromEntity(
                            source,
                            params
                        )
                    }
                } else {
                    new = MPC_aegisMissileEntityPlugin.createNewFromMarket(
                        market,
                        params,
                        missile
                    )
                }

                return new
            }
        }
    }

    companion object {
        fun alertFleetToThreat(fleet: CampaignFleetAPI, target: SectorEntityToken) {
            if (target !is CampaignFleetAPI) return
            val pursuitTarget = fleet.memoryWithoutUpdate.getFleet(FleetAIFlags.PURSUIT_TARGET)
            if (pursuitTarget == null) {
                val ai = fleet.ai as? ModularFleetAI ?: return
                val tac = ai.tacticalModule
                val strat = ai.strategicModule
                if (strat.isAllowedToEngage(target) && !tac.isBusy && tac.priorityTarget == null) {
                    fleet.memoryWithoutUpdate[FleetAIFlags.PURSUIT_TARGET] = target
                    tac.setPriorityTarget(target, 3f, false)
                    fleet.memoryWithoutUpdate[FleetAIFlags.LAST_SEEN_TARGET_LOC] = Vector2f(target.location)
                    fleet.memoryWithoutUpdate[FleetAIFlags.PLACE_TO_LOOK_FOR_TARGET] = Vector2f(target.location)
                }
            } else if (pursuitTarget == target) {
                fleet.memoryWithoutUpdate[FleetAIFlags.LAST_SEEN_TARGET_LOC] = Vector2f(target.location)
                fleet.memoryWithoutUpdate[FleetAIFlags.PLACE_TO_LOOK_FOR_TARGET] = Vector2f(target.location)
            }
        }

        fun getMissileCarriers(fleet: CampaignFleetAPI): MutableSet<FleetMemberAPI> {
            val carriers = HashSet<FleetMemberAPI>()
            for (member in fleet.fleetData.membersListCopy) {
                if (member.variant.hasHullMod("MPC_missileCarrier") && !member.variant.hasTag("MPC_missileCarrierDisarmed")) {
                    carriers += member
                }
            }
            return carriers
        }

        fun getStandardAbilityParams(fleet: SectorEntityToken, currTarget: SectorEntityToken?, id: String): MPC_aegisMissileEntityPlugin.MissileParams {
            val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                Color(255, 90, 60),
                fleet.containingLocation,
                fleet.location,
                0f,
                0f
            )
            explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.NONE
            val params = MPC_aegisMissileEntityPlugin.MissileParams(
                currTarget ?: fleet, // please dont use this missile if you dont pass in target.
                "${id}_${Misc.genUID()}",
                null,
                fleet,
                Missile.EXPLOSIVE,
                explosionParams,
                faction = fleet.faction.id,
                launchFacingTarget = false,
            )
            return params
        }

        fun getPlayerAbility(): MPC_missileStrikeAbility? = Global.getSector().playerFleet?.getAbility("MPC_missileStrike") as? MPC_missileStrikeAbility


        fun playerNextMissile() {
            val ability = getPlayerAbility() ?: return
            ability.nextMissile()
        }
        fun playerPrevMissile() {
            val ability = getPlayerAbility() ?: return
            ability.prevMissile()
        }

        const val MAX_RANGE = 7500f
        const val CARRIER_USABLE_CR_PERCENT = 0.2f
    }

    fun getMaxMissiles(): Int {
        if (fleet == null) return 0
        var max = 0
        val carriers = getMissileCarriers(fleet).filter { it.repairTracker.cr >= CARRIER_USABLE_CR_PERCENT }
        for (carrier in carriers) {
            val mod = MPC_missileCarrier.getNumMissiles(carrier)
            max += mod
        }

        return max
    }

    private fun sanitizeMaxMissiles() {
        val max = getMaxMissiles()
        val currMissiles = missilesLoaded + reloadIntervals.size
        //val sizeOfReloads = reloadIntervals.size
        var failsafeNeeded = (max - currMissiles)

        while (failsafeNeeded-- > 0) {
            reloadIntervals += IntervalUtil(3f, 4f)
        }
    }

    var missilesLoaded: Int = 2
        get() {
            if (field == null) field = 2
            return field.coerceAtMost(getMaxMissiles())
        }
    var reloadIntervals = ArrayList<IntervalUtil>()
        get() {
            if (field == null) field = ArrayList<IntervalUtil>()
            return field
        }
    var currMissile: Missile = Missile.EXPLOSIVE
    @Transient
    var currTarget: CampaignFleetAPI? = null

    override fun init(id: String?, entity: SectorEntityToken?) {
        super.init(id, entity)

        missilesLoaded = getMaxMissiles()
    }

    private fun nextMissile() {
        val currOrdinal = pickable.indexOf(currMissile)
        var nextOrdinal = currOrdinal + 1
        if (nextOrdinal > pickable.size - 1) nextOrdinal = 0

        val target = pickable.find { it.ordinal == nextOrdinal } ?: throw RuntimeException("somehow we didnt get the right missile. index: $nextOrdinal, list size: ${pickable.size}")
        currMissile = target
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val days = Misc.getDays(amount)

        sanitizeMaxMissiles()

        for (interval in reloadIntervals.toList()) {
            interval.advance(days)
            if (interval.intervalElapsed()) {
                if (missilesLoaded < getMaxMissiles() && fleet.isPlayerFleet) {
                    fleet.addFloatingText(
                        "Missile loaded!",
                        Misc.getPositiveHighlightColor(),
                        0.2f
                    )
                    Global.getSoundPlayer().playUISound("MPC_missileStrikeReload", 1f, 1f)
                }
                missilesLoaded++
                reloadIntervals.remove(interval)
            }
        }
    }

    private fun prevMissile() {
        val currOrdinal = pickable.indexOf(currMissile)
        var nextOrdinal = currOrdinal - 1
        if (nextOrdinal <= -1f) nextOrdinal = pickable.size - 1

        val target = pickable.find { it.ordinal == nextOrdinal } ?: throw RuntimeException("somehow we didnt get the right missile. index: $nextOrdinal, list size: ${pickable.size}")
        currMissile = target
    }

    override fun pressButton() {
        if (isUsable && !turnedOn) {
            if (fleet.isPlayerFleet) {

                if (MPC_missileStrikeInputListener.get(false)?.active != true) {

                    val soundId = onSoundUI
                    if (soundId != null) {
                        if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                            Global.getSoundPlayer()
                                .playSound(soundId, 1f, 1f, Global.getSoundPlayer().listenerPos, Vector2f())
                        } else {
                            Global.getSoundPlayer().playUISound(soundId, 0.7f, 1f)
                        }
                    }

                    class DelayedScript : MPC_delayedExecutionNonLambda(
                        IntervalUtil(0f, 0f),
                        useDays = false,
                        runIfPaused = true
                    ) {
                        override fun executeImpl() {
                            MPC_missileStrikeInputListener.get(true)!!.activate(this@MPC_missileStrikeAbility)
                        }
                    }

                    DelayedScript().start()
                }

            }
        }
    }

    fun forceActivation() {
        activateImpl()
        missilesLoaded--
        addReloadTimer(currMissile.getCooldownTimeDays())
        //setCooldownLeft(currMissile.getCooldownTimeDays())
        subtractCommodities()
    }

    private fun addReloadTimer(cooldownTimeDays: Float) {
        val newInterval = IntervalUtil(cooldownTimeDays, cooldownTimeDays)
        reloadIntervals.add(newInterval)
    }

    private fun subtractCommodities() {
        val armaments = getArmamentCost()
        val fuel = getFuelCost()

        val cargo = fleet.cargo
        cargo.removeCommodity(Commodities.HAND_WEAPONS, armaments)
        cargo.removeFuel(fuel)
    }

    override fun activateImpl() {
        // do the actual missile launching here
        // target is guaranteed here
        Missile.getMissileFrom(currMissile, fleet, currTarget!!, faceTarget = false)
        currMissile.postAbilityUse()

        if (fleet.isPlayerFleet && !Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_missileStrikeReactionPrepared")) {
            for (fleet in fleet.containingLocation.fleets) {
                if (fleet == this.fleet) continue
                val level = fleet.getVisibilityLevelTo(this.fleet)
                if (level.ordinal >= VisibilityLevel.SENSOR_CONTACT.ordinal) {
                    MPC_missileStrikeReactionScript.get(true)?.start()
                }
            }
        }

        for (iterFleet in fleet.containingLocation.fleets.filter { fleet.getVisibilityLevelTo(it).ordinal >= VisibilityLevel.SENSOR_CONTACT.ordinal }) {
            if (iterFleet.isPlayerFleet) continue
            alertFleetToThreat(iterFleet, fleet)
        }
    }

    fun getBaseParams(): MPC_aegisMissileEntityPlugin.MissileParams {
        return getStandardAbilityParams(fleet, currTarget, id)
    }

    override fun applyEffect(amount: Float, level: Float) {
        return
    }

    override fun deactivateImpl() {

    }

    override fun cleanupImpl() {

    }

    override fun isUsable(): Boolean {
        val superCall = super.isUsable
        if (!superCall) return false

        if (MPC_missileStrikeInputListener.get(true)!!.active) return false

        if (!canFire()) return false

        return true
    }

    fun canFire(): Boolean {
        val carriers = getMissileCarriers(fleet)
        if (carriers.isEmpty() || carriers.all { it.repairTracker.cr <= CARRIER_USABLE_CR_PERCENT }) return false
        if (missilesLoaded <= 0f) return false

        if (fleet.isPlayerFleet) {
            val cargo = fleet.cargo

            if (cargo.getCommodityQuantity(Commodities.HAND_WEAPONS) < getArmamentCost()) return false
            if (cargo.fuel < getFuelCost()) return false
            if (!currMissile.canFireFromAbility()) return false
        }

        return true
    }

    fun getArmamentCost(): Float {
        return currMissile.getArmamentCost() // placeholder
    }

    fun getFuelCost(): Float {
        return currMissile.getFuelCost() // placeholder
    }

    fun canTargetEntity(target: SectorEntityToken): Boolean {
        if (target !is CampaignFleetAPI) return false

        if (target == fleet) return false
        if (target.containingLocation != fleet.containingLocation) return false

        val dist = MathUtils.getDistance(fleet, target)
        if (dist > MAX_RANGE) return false

        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val fleet = getFleet()
        if (fleet == null) return

        Misc.getGrayColor()
        Misc.getHighlightColor()
        Misc.getNegativeHighlightColor()

        if (!Global.CODEX_TOOLTIP_MODE) {
            tooltip.addTitle(spec.name)
        } else {
            tooltip.addSpacer(-10f)
        }

        val pad = 10f

        tooltip.addPara(
            "Utilize the missile carriers within your fleet to deliver a devastating missile strike to a single fleet within %s.",
            pad,
            Misc.getHighlightColor(),
            "${MAX_RANGE.toInt()}su*"
        )

        if (!Global.CODEX_TOOLTIP_MODE) {
            if (getMissileCarriers(fleet).isEmpty()) {
                tooltip.addPara(
                    "You do not have any missile carriers in your fleet.",
                    pad
                ).color = Misc.getNegativeHighlightColor()

                return
            } else if (getMissileCarriers(fleet).all { it.repairTracker.cr <= 0.1f }) {
                tooltip.addPara(
                    "All of your fleet's missile carriers are combat-incapable.",
                    pad
                ).color = Misc.getNegativeHighlightColor()
            }
        }

        if (Global.CODEX_TOOLTIP_MODE) {
            tooltip.addPara(
                "Costs %s of %s and %s to fire, based on the loaded missile.",
                pad,
                Misc.getHighlightColor(),
                "a significant amount", "heavy armaments", "fuel"
            )
        } else {

            val loadedColor = if (missilesLoaded <= 0f) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
            val missilesString = if (missilesLoaded == 1) "missile" else "missiles"
            tooltip.addPara(
                "%s $missilesString loaded - %s max",
                pad,
                Misc.getHighlightColor(),
                "$missilesLoaded", "${getMaxMissiles()}"
            ).setHighlightColors(
                loadedColor,
                Misc.getHighlightColor()
            )

            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            for (interval in reloadIntervals) {
                val until = (interval.intervalDuration - interval.elapsed).roundNumTo(1).trimHangingZero()
                tooltip.addPara(
                    "New missile in %s",
                    0f,
                    Misc.getHighlightColor(),
                    "$until days"
                )
            }
            tooltip.setBulletedListMode(null)

            tooltip.addPara(
                "Fires from the direction your fleet is facing, meaning you must %s for an accurate shot.",
                5f,
                Misc.getHighlightColor(),
                "orient your fleet towards your target"
            )

            val color = if (currMissile.canFireFromAbility()) currMissile.nameColor else Misc.getGrayColor()
            val name = currMissile.frontEndName + currMissile.getNameSuffix(currMissile.frontEndName)
            tooltip.addPara(
                "Currently loaded missile type: %s %s",
                10f,
                color,
                name, "(Press left or right arrow to switch)"
            ).setHighlightColors(
                color,
                Misc.getHighlightColor()
            )
            currMissile.addToTooltip(tooltip, getBaseParams(), true)

            val gray = Misc.getGrayColor()
            tooltip.addPara("*2000 units = 1 map grid cell", gray, pad)
            if (expanded) {
                tooltip.addSectionHeading("Tactical", Alignment.MID, 5f)
                tooltip.addPara(
                    "Fleets will only evade if they can see the missile. Consider firing in/from an obscured position.",
                    pad
                )
                tooltip.addPara(
                    "Firing directly ahead while being chased causes the missile to loop around, either forcing them to evade and break pursuit or be hit in the back.",
                    pad
                )
            }
        }

        addIncompatibleToTooltip(tooltip, expanded)
    }
}