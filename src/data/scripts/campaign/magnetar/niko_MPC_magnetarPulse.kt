package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin.ExplosionFleetDamage
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.MPC_delayedExecutionNonLambda
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_stringUtils.toPercent
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addHitGlow
import java.awt.Color
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The wave itself */
class niko_MPC_magnetarPulse(): BaseRingTerrain(), AuroraRenderer.AuroraRendererDelegate {

    companion object {
        fun createPulse(source: SectorEntityToken, params: MagnetarPulseParams): niko_MPC_magnetarPulse {
            val terrain = source.containingLocation.addTerrain(
                TERRAIN_ID,
                params
            ) as CampaignTerrainAPI
            terrain.setLocation(source.location.x, source.location.y)
            return terrain.plugin as niko_MPC_magnetarPulse
        }

        val BASE_COLOR = Color(37, 245, 200, 190)

        /** At max interdict effectiveness, immobility is reduced by this percent. */
        const val MAX_INTERDICT_REINFORCEMENT = 1f
        /** MAX_INTERDICT_REINFORCEMENT is reached at this progress level of interdiction. */
        const val MAX_INTERDICT_PROGRESS_NEEDED = 0.6f
        const val INTERDICT_EXTRA_COOLDOWN = 3f

        const val INTERDICT_COMPLETE_GRACE_DAYS = 0.1f

        const val BASE_STRIKE_DAMAGE = 25

        const val BASE_SHOCKWAVE_SPEED = 5000f

        const val BASE_BURN_MULT = 0.8f
        const val BASE_BURN_MULT_DURATION = 0.2f

        const val PARTICLE_GEN_DIST = 10000f
        const val PARTICLES_PER_SECOND = 45f

        const val TERRAIN_ID = "MPC_magnetarPulseTerrain"

        const val DIST_TO_PLAY_LOOP = 2500f
        const val MIN_DIST_FOR_MAX_VOL = 500f

        const val SHIELDS_FROM_PULSE_TAG = "MPC_blocksMagnetarPulse"
    }

    class MagnetarPulseParams(
        /** The distance this pulse will travel before completely expiring. */
        val distance: Float,
        /** Works with distance. When within this many SU of the source, the pulse does the max effect possible. Outside, it will fade from 100% to 0% opacity - hitting 0% at the end of lifespan.*/
        val maxEffectRange: Float,
        bandWidth: Float,
        relatedEntity: SectorEntityToken? = null,
        startingRange: Float = 0f,
        name: String = "Magnetar Pulse",
        /** Will ALWAYS play, even if the player is too far away. If the player IS far away, plays a lot more quietely.*/
        val spawnSounds: List<String> = listOf("MPC_magnetarPulseLaunch"),
        /** In SU/s. Does not increase the distance of the wave. */
        val speed: Float = BASE_SHOCKWAVE_SPEED,
        var damage: ExplosionFleetDamage = ExplosionFleetDamage.LOW,
        var explosionDamageMult: Float = 1f,
        var explosionDisruptionMult: Float = 1f,
        /** Do we respect the memory flag that says 'we dont take damage from this terrain'?*/
        val respectIgnore: Boolean = true,
        val sourceFleet: CampaignFleetAPI? = null,
        val baseRepLoss: Float = 0f,
        val color: Color = BASE_COLOR,
    ): RingParams(bandWidth, startingRange, relatedEntity, name)

    @Transient
    var blockerUtil: MPC_rangeBlockerWithEnds = MPC_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_PULSE_TAG)
        get() {
            if (field == null) field = MPC_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_PULSE_TAG)
            return field
        }

    /** A list of fleets we've already hit. Used to avoid spamming hits over and over. */
    val alreadyHit = HashSet<CampaignFleetAPI>()
    /** The bandwidth we started with. Our bandwith can change based on the position of our ring, and if its too small. */
    var idealBandwidth: Float = 0f

    @Transient
    var texture: SpriteAPI? = null

    lateinit var token: SectorEntityToken
    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        if (entity == null) return
        if (name == null || name == "Unknown") name = "Magnetar Pulse"
        val casted = getCastedParams()
        if (casted.spawnSounds.isNotEmpty() && relatedEntity?.containingLocation?.isCurrentLocation == true || entity.containingLocation?.isCurrentLocation == true) {
            val viewport = Global.getSector().viewport
            var volume = 1f

            var soundLoc = Vector2f(entity.location)

            if (!viewport.isNearViewport(soundLoc, 10f)) {
                val vec = VectorUtils.getDirectionalVector(soundLoc, viewport.center)
                val dist = MathUtils.getDistance(soundLoc, viewport.center)
                soundLoc = vec.scale(dist * 0.9f) as Vector2f

                volume = 0.1f
            }

            for (id in casted.spawnSounds) {
                Global.getSoundPlayer().playSound(id, 1f, volume, soundLoc, Misc.ZERO)
            }
        }
        idealBandwidth = params.bandWidthInEngine

        token = relatedEntity.containingLocation.createToken(Vector2f(0f, 0f))
    }

    var particleCache = 0f
    override fun advance(amount: Float) {
        if (params !is MagnetarPulseParams) {
            relatedEntity?.containingLocation?.removeEntity(entity)
            return // wait for compatibility utils to clean us up
        }

        super.advance(amount)

        corona.advance(amount)
        for (fader in faders.values) {
            fader.advance(amount * MathUtils.getRandomNumberInRange(0.1f, 10f))
        }

        val mult = getEffectMult(null)
        if (mult <= 0f) {
            delete()
            return
        }

        val casted = getCastedParams()
        val days = Misc.getDays(amount)
        val adjustPos = casted.speed * days
        casted.middleRadius += adjustPos

        if (!blockerUtil.wasEverUpdated()) {
            blockerUtil.updateAndSync(entity, entity.starSystem?.star, 0.1f)
        }

        blockerUtil.updateLimits(entity, entity.starSystem?.star, 0.5f)
        blockerUtil.advance(amount, 100f, 0.5f)

        val ourDist = getProgress()
        val viewport = Global.getSector().viewport
        val viewportOffset = MathUtils.getDistance(entity.location, viewport.center)
        val distFromUs = (viewportOffset - ourDist).coerceAtLeast(0f)
        if (distFromUs <= DIST_TO_PLAY_LOOP) {
            playLoop()
        }

        for (fleet in entity.containingLocation.fleets) {
            if (fleet.memoryWithoutUpdate.getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE) && !fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION)) {
                fleet.memoryWithoutUpdate.set(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION, true, INTERDICT_COMPLETE_GRACE_DAYS)
            }
        }

        /*particleCache += amount * PARTICLES_PER_SECOND
        if (particleCache >= 1) {
            while (particleCache-- > 0) {
                val point = MathUtils.getRandomPointOnCircumference(
                    entity.location,
                    casted.middleRadius + (casted.bandWidthInEngine / 2)
                )
                if (MathUtils.getDistance(point, Global.getSector().viewport.center) > PARTICLE_GEN_DIST) return
                val particle = entity.containingLocation.addGlowyParticle(
                    Global.getSector().playerFleet.location, // we're tricking the particles to spawn even when they will be outside our viewport
                    VectorUtils.getDirectionalVector(entity.location, point).scale(casted.speed * MathUtils.getRandomNumberInRange(0.04f, 0.06f)) as Vector2f,
                    MathUtils.getRandomNumberInRange(50f, 60f),
                    0.1f,
                    MathUtils.getRandomNumberInRange(20f, 30f),
                    Color(0, 90, 0)
                )
                for (part in particle) {
                    if (part == null) continue
                    part.setPos(point.x, point.y)
                }
                particle[1]?.color = Color(0, 30, 0)
                particle[2]?.color = Color(0, 30, 0)
            }
        }*/
    }

    private fun playLoop() {
        val ourDist = getProgress()
        val viewport = Global.getSector().viewport
        val angle = VectorUtils.getAngle(entity.location, viewport.center)
        val target = MathUtils.getPointOnCircumference(
            entity.location,
            ourDist,
            angle
        )
        val point = MathUtils.getPointOnCircumference(entity.location, params.middleRadius, angle)
        val newLoc = MathUtils.getPointOnCircumference(viewport.center, 250f, VectorUtils.getAngle(viewport.center, point))
        token.setLocation(newLoc.x, newLoc.y)

        val distFromTarget = MathUtils.getDistance(point, viewport.center)
        val adjustedDist = (distFromTarget - MIN_DIST_FOR_MAX_VOL).coerceAtLeast(0f)
        var volume = 1 - (adjustedDist / DIST_TO_PLAY_LOOP)

        volume *= getEffectMult(null)
        volume *= 0.5f
        Global.getSoundPlayer().playLoop(
            "phase_beam_loop",
            token,
            0.6f,
            volume,
            token.location,
            Misc.ZERO
        )
    }

    val corona = MPC_magnetarFieldAuroraRenderer(this)
    override fun render(layer: CampaignEngineLayers?, v: ViewportAPI?) {
        super.render(layer, v)

        if (layer == null || v == null) return

        //params.middleRadius = idealBandwidth.coerceAtMost(params.middleRadius) // prevents really weird inverse ring behavior
        corona.render(Global.getSector().viewport.alphaMult)
        /*params.bandWidthInEngine = idealBandwidth.coerceAtMost(params.middleRadius) // prevents really weird inverse ring behavior
        // glitches on advance, trust me, i tried

        val params = getCastedParams()
        OpenGlUtils.drawTexturedRing(
            Vector2f(entity.location),
            params.middleRadius,
            params.bandWidthInEngine,
            380, // polygons of the circle
            25, // segments of the ring
            500f,
            sprite,
            90f,
            getEffectMult(null),
        )*/
    }

    @Transient
    var rr: RingRenderer? = null
    override fun renderOnMap(factor: Float, alphaMult: Float) {
        if (params == null) return
        val castedParams = getCastedParams()
        if (rr == null) {
            rr = RingRenderer("systemMap", "map_asteroid_belt")
        }
        val playerFleet = Global.getSector().playerFleet
        // make it a bit transparent if the wave isnt a current threat
        var adjustedAlpha = if (shouldHitFleet(playerFleet)) alphaMult else alphaMult * 0.5f
        val mult = getEffectMult(null)
        adjustedAlpha *= mult
        val max = castedParams.middleRadius + (castedParams.bandWidthInEngine / 2f)
        val min = max - 500f
        rr!!.render(
            entity.location,
            min,
            max,
            getCastedParams().color,
            false, factor, adjustedAlpha
        )
    }

    override fun renderOnRadar(radarCenter: Vector2f?, factor: Float, alphaMult: Float) {
        if (radarCenter == null) return

        GL11.glPushMatrix();
        GL11.glTranslatef(-radarCenter.x * factor, -radarCenter.y * factor, 0f);
        renderOnMap(factor, alphaMult);
        GL11.glPopMatrix();
    }

    private fun delete() {
        val containing = entity.containingLocation
        containing.removeEntity(entity)
    }

    fun getCastedParams(): MagnetarPulseParams = params as MagnetarPulseParams

    val UID = Misc.genUID()
    override fun getTerrainId(): String? {
        return UID // allows multiple pulses to affect a fleet
    }

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        if (entity !is CampaignFleetAPI) return

        // place behavior that also affects the dawntide fleets here
        if (fleetIsBlocked(entity) && entity.isPlayerFleet) {
            val discoveredInterdictTech = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")
            if (!discoveredInterdictTech) {
                class parryDiscoverScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
                    override fun executeImpl() {
                        if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")) {
                            Global.getSector().campaignUI.showInteractionDialog(
                                RuleBasedInteractionDialogPluginImpl("MPC_hidMagnetarPulseFirstTime"),
                                Global.getSector().playerFleet
                            )
                        }
                    }
                }
                parryDiscoverScript(IntervalUtil(0.1f, 0.1f)).start()
            }
        }
        if (!shouldHitFleet(entity)) return
        hitFleet(entity)
    }

    fun shouldHitFleet(fleet: CampaignFleetAPI): Boolean {
        if (fleetIsImmune(fleet)) return false
        if (fleet in alreadyHit) return false
        if (fleetIsBlocked(fleet)) return false

        return true
    }

    fun getMiddle(): Vector2f {
        return (Vector2f(entity.location).translate(params.middleRadius, params.middleRadius))
    }

    fun hitFleet(fleet: CampaignFleetAPI) {
        val mult = getEffectMult(fleet)
        val casted = getCastedParams()

        val members = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return

        val interdictAbility = fleet.getAbility(Abilities.INTERDICTION_PULSE) as? InterdictionPulseAbility
        var interdictionEffectiveness: Float? = null
        /** Non-null if an interdiction was attempted. */
        var shatterTimeMult = mult
        if (interdictAbility != null) {
            val progressFraction: Float = interdictAbility.progressFraction

            if (fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION)) {
                interdictionEffectiveness = MAX_INTERDICT_REINFORCEMENT
            } else if (interdictAbility.isInProgress) {
                interdictionEffectiveness = (progressFraction / MAX_INTERDICT_PROGRESS_NEEDED).coerceAtMost(MAX_INTERDICT_REINFORCEMENT)
            }

            if (interdictionEffectiveness != null) {
                shatterTimeMult *= (1 - interdictionEffectiveness)
                if (interdictAbility.isInProgress) {
                    interdictAbility.activeDaysLeft = 0.0000001f
                    interdictAbility.advance(1f) // just to get it to naturally deactivate
                }
                interdictAbility.cooldownLeft += INTERDICT_EXTRA_COOLDOWN
            }
        }
        var interdictionResultsString = ""
        if (interdictionEffectiveness != null) {
            interdictionResultsString = " (interdiction reduced days needed by ${toPercent((interdictionEffectiveness).roundNumTo(1))}"
        }

        val immobileDur = ((1f * shatterTimeMult * casted.explosionDisruptionMult).roundNumTo(1)).coerceAtMost(
            niko_MPC_magnetarStarScript.MIN_DAYS_PER_PULSE * 0.8f)
        val immobileFromDays = Global.getSector().clock.convertToSeconds(immobileDur)
        val desc = "Drive field destroyed (${immobileDur.trimHangingZero()} days to repair)"

        for (ability in fleet.abilities.values) {
            if (!ability.spec.hasTag(Abilities.TAG_BURN + "+") && ability.id != Abilities.TRANSVERSE_JUMP) continue
            ability.deactivate()
            ability.cooldownLeft = ability.cooldownLeft.coerceAtLeast(immobileDur)
        }

        if (immobileFromDays > 0) {
            for (view in fleet.views) {
                view.setJitter(0.1f, immobileFromDays, getCastedParams().color, 2, 3f)
                view.setUseCircularJitter(true)
                view.setJitterDirection(Misc.ZERO)
                view.setJitterLength(immobileFromDays)
                view.setJitterBrightness(0.2f)
            }
        }

        fleet.stats.addTemporaryModMult(immobileDur, entity.id + "_magnetPulseAftermathEngines", desc, -500f, fleet.stats.fleetwideMaxBurnMod)
        fleet.memoryWithoutUpdate.set(niko_MPC_ids.DRIVE_BUBBLE_DESTROYED, true, immobileDur)

        if (fleet.isPlayerFleet) {
            if (interdictionEffectiveness != null && interdictionEffectiveness >= 1f)  {
                Global.getSector().campaignUI.addMessage(
                    "The ionized pulse bounces off your reinforced drive bubble, thanks to your interdiction", Misc.getHighlightColor()
                )
            } else {
                Global.getSector().campaignUI.addMessage(
                    "The ionized pulse shatters your drive bubble, disabling your travel drive for ${immobileDur.trimHangingZero()} days${interdictionResultsString}",
                    Misc.getNegativeHighlightColor()
                )
            }
        }

        var totalValue = 0f
        for (member in members) {
            totalValue += member.stats.suppliesToRecover.modifiedValue
        }
        if (totalValue <= 0) return

        var damageFraction = 0f
        damageFraction = when (casted.damage) {
            ExplosionFleetDamage.NONE -> return
            ExplosionFleetDamage.LOW -> 0.4f
            ExplosionFleetDamage.MEDIUM -> 0.7f
            ExplosionFleetDamage.HIGH -> 0.8f
            ExplosionFleetDamage.EXTREME -> 0.9f
            else -> return
        }

        damageFraction *= mult

        if (fleet.isInCurrentLocation && fleet.isVisibleToPlayerFleet) {
            val dist = Misc.getDistance(fleet, Global.getSector().playerFleet)
            if (dist < HyperspaceTerrainPlugin.STORM_STRIKE_SOUND_RANGE) {
                val volumeMult = 3f * damageFraction
                Global.getSoundPlayer().playSound("MPC_magnetarPulseImpact", 1f, volumeMult, fleet.location, Misc.ZERO)
            }
        }

        val picker = WeightedRandomPicker<FleetMemberAPI>()
        for (member in members) {
            var w = 1f
            if (member.isFrigate) w *= 0.1f
            if (member.isDestroyer) w *= 0.2f
            if (member.isCruiser) w *= 0.5f
            picker.add(member, w)
        }

        val numStrikes = picker.items.size
        alreadyHit += fleet

        for (i in 0 until numStrikes) {
            val member = picker.pick() ?: return
            val crPerDep = member.deployCost
            //if (crPerDep <= 0) continue;
            val suppliesPerDep = member.stats.suppliesToRecover.modifiedValue
            if (suppliesPerDep <= 0 || crPerDep <= 0) return
            val suppliesPer100CR = suppliesPerDep * 1f / 0.01f.coerceAtLeast(crPerDep)

            // half flat damage, half scaled based on ship supply cost cost
            var strikeSupplies = ((BASE_STRIKE_DAMAGE) + suppliesPer100CR) * 0.5f * damageFraction
            strikeSupplies *= casted.explosionDamageMult
            //strikeSupplies = suppliesPerDep * 0.5f * damageFraction;
            var strikeDamage = strikeSupplies / suppliesPer100CR * (0.75f + Math.random().toFloat() * 0.5f)
            strikeDamage *= 0.6f
            strikeDamage *= mult

            //float strikeDamage = damageFraction * (0.75f + (float) Math.random() * 0.5f);
            val resistance = member.stats.dynamic.getValue(Stats.CORONA_EFFECT_MULT)
            strikeDamage *= resistance
            if (strikeDamage > HyperspaceTerrainPlugin.STORM_MAX_STRIKE_DAMAGE) {
                strikeDamage = HyperspaceTerrainPlugin.STORM_MAX_STRIKE_DAMAGE
            }
            if (strikeDamage > 0) {
                val currCR = member.repairTracker.baseCR
                val crDamage = currCR.coerceAtMost(strikeDamage)
                if (crDamage > 0) {
                    member.repairTracker.applyCREvent(
                        -crDamage, "explosion_" + entity.id,
                        "Damaged by explosion"
                    )
                }
                val hitStrength = member.stats.armorBonus.computeEffective(member.hullSpec.armorRating)
                //hitStrength *= strikeDamage / crPerDep;
                var numHits = (strikeDamage / 0.1f).toInt()
                if (numHits < 1) numHits = 1
                for (j in 0 until numHits) {
                    member.status.applyDamage(hitStrength)
                }
                //member.getStatus().applyHullFractionDamage(1f);
                if (member.status.hullFraction < 0.01f) {
                    member.status.hullFraction = 0.01f
                    picker.remove(member)
                } else {
                    val w = picker.getWeight(member)
                    picker.setWeight(picker.items.indexOf(member), w * 0.5f)
                }
            }
            //picker.remove(member);
        }

        val shoveDir = Misc.getAngleInDegrees(entity.location, fleet.location)
        val shoveIntensity = (damageFraction * 20f).coerceAtMost(casted.speed / 12000f) // not arbitrary, it mostly keeps it locked to the speed of the pulse
        fleet.addScript(ShoveFleetScript(fleet, shoveDir, shoveIntensity))

        val sourceFleet = casted.sourceFleet
        if (sourceFleet?.isPlayerFleet == true && fleet.knowsWhoPlayerIs() && sourceFleet.faction != fleet.faction) {
            val repParams = CoreReputationPlugin.CustomRepImpact()
            repParams.delta = -casted.baseRepLoss * (mult * 10f)
            repParams.limit = RepLevel.INHOSPITABLE
            Global.getSector().adjustPlayerReputation(
                CoreReputationPlugin.RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, repParams, null, false),
                fleet.faction.id
            )
        }

        if (fleet.isInCurrentLocation) {
            val vis: SectorEntityToken.VisibilityLevel = fleet.visibilityLevelToPlayerFleet
            if (fleet.isPlayerFleet || vis == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS || vis == SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS) {
                if (interdictionEffectiveness != null && interdictionEffectiveness >= 1f) {
                    fleet.addFloatingText("Pulse avoided!", fleet.faction.baseUIColor, 1f, true)
                    if (fleet.isPlayerFleet) {
                        Global.getSector().memoryWithoutUpdate["\$MPC_discoveredInterdictsCounterPulses"] = true
                    }
                } else {
                    fleet.addFloatingText(
                        "Drive field destroyed! ($immobileDur days to repair)",
                        fleet.faction.baseUIColor,
                        1f,
                        true
                    )
                    if (fleet.isPlayerFleet) {
                        val discoveredInterdictTech = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")
                        if (discoveredInterdictTech) return
                        class MPC_hitByPulseScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
                            override fun executeImpl() {
                                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")) {
                                    Global.getSector().campaignUI.showInteractionDialog(
                                        RuleBasedInteractionDialogPluginImpl("MPC_hitByMagnetarPulseFirstTime"),
                                        Global.getSector().playerFleet
                                    )
                                }
                            }
                        }
                        MPC_hitByPulseScript(IntervalUtil(0.1f, 0.1f)).start()
                    }
                }
            }
        }
    }

    // corresponds with the opacity of the wave
    private fun getEffectMult(fleet: CampaignFleetAPI?): Float {
        val progress = getProgress()
        val casted = getCastedParams()
        val progressOutsideMaxRange = (progress - casted.maxEffectRange).coerceAtLeast(0f)
        val lifespanOutsideRange = casted.distance - casted.maxEffectRange
        val mult = 1 - (progressOutsideMaxRange / lifespanOutsideRange)
        return mult
    }

    fun getProgress(): Float = MathUtils.getDistance(entity.location, getMiddle())

    fun fleetIsImmune(fleet: CampaignFleetAPI): Boolean {
        if (fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_MAGNETAR_PULSE) && getCastedParams().respectIgnore) return true
        if (getCastedParams().sourceFleet == fleet) return true

        return false
    }

    fun fleetIsBlocked(fleet: CampaignFleetAPI): Boolean {
        val location = fleet.location
        val angle = VectorUtils.getAngle(entity.location, location)
        val containing = entity.containingLocation ?: return false
        val blockers = containing.planets + containing.getEntitiesWithTag(SHIELDS_FROM_PULSE_TAG)
        if (blockers.any { MathUtils.getDistance(it, fleet) <= 0f }) return true

        val range = blockerUtil.getBlockedRangeAt(angle)
        val dist = MathUtils.getDistance(entity.location, location)

        if (dist > range.first && dist < range.second) return true
        return false
    }

    override fun getEffectCategory(): String? {
        return null
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    override fun hasAIFlag(flag: Any?): Boolean {
        return false // it just passes over fleets
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
        return EnumSet.of(CampaignEngineLayers.ABOVE_STATIONS)
    }

    override fun hasTooltip(): Boolean = true

    override fun isTooltipExpandable(): Boolean {
        return false
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(tooltip, expanded)

        if (tooltip == null) return

        val baseName = nameForTooltip
        tooltip.addTitle(baseName)
        tooltip.addPara(Global.getSettings().getDescription(spec.id, Description.Type.TERRAIN).text1, 5f)
        // nothing else here. most of the intel on what these do and how to avoid them will be in a system-spanning terrain
    }

    override fun getNameColor(): Color? {
        val base = getCastedParams().color
        val special = Misc.getNegativeHighlightColor()

        return Misc.interpolateColor(base, special, Global.getSector().campaignUI.sharedFader.brightness * 1f)
    }

    override fun getAuroraInnerRadius(): Float {
        return (getCastedParams().middleRadius - (getCastedParams().bandWidthInEngine / 2f)).coerceAtLeast(0f)
    }

    override fun getAuroraOuterRadius(): Float {
        return getCastedParams().middleRadius + (getCastedParams().bandWidthInEngine / 2f)
    }

    override fun getAuroraCenterLoc(): Vector2f? {
        return entity.location
    }

    override fun getAuroraColorForAngle(angle: Float): Color {
        val color = getCastedParams().color
        val ourColor = Color(color.red, color.green, color.blue, (color.alpha))
        return ourColor
    }

    override fun getAuroraAlphaMultForAngle(angle: Float): Float {
        return getEffectMult(null)
    }

    val faders = HashMap<Float, FaderUtil>()

    fun getFader(angle: Float): FaderUtil {
        if (faders[angle] == null) {
            val newFader = FaderUtil(
                MathUtils.getRandomNumberInRange(0f, 1f),
                1f
            )
            faders[angle] = newFader
        }
        return faders[angle]!!
    }

    override fun getAuroraShortenMult(angle: Float): Float {
        val fader = getFader(angle)
        return fader.brightness.coerceAtLeast(0f)
    }

    override fun getAuroraInnerOffsetMult(angle: Float): Float {
        return 100f
    }

    override fun getAuroraThicknessMult(angle: Float): Float {
        return 1f
    }

    override fun getAuroraThicknessFlat(angle: Float): Float {
        return 0f
    }

    override fun getAuroraTexPerSegmentMult(): Float {
        return 2f
    }

    override fun getAuroraBandWidthInTexture(): Float {
        return 256f
    }

    override fun getAuroraTexture(): SpriteAPI? {
        if (texture == null) {
            texture = Global.getSettings().getSprite("terrain", "MPC_reversedAurora")
        }
        return texture
    }

    override fun getAuroraBlocker(): RangeBlockerUtil? {
        return null
    }
}