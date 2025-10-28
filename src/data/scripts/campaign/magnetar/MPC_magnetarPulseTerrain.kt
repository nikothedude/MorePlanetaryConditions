package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.RingBandAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin.ExplosionFleetDamage
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.ShoveFleetScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.fs.starfarer.campaign.CampaignTerrain
import com.fs.starfarer.campaign.CustomCampaignEntity
import com.fs.starfarer.campaign.RingBand
import com.fs.starfarer.combat.CombatViewport
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.MIN_DAYS_PER_PULSE
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.DRIVE_BUBBLE_DESTROYED
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_stringUtils.toPercent
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_magnetarPulseTerrain(): BaseRingTerrain() {

    companion object {
        fun createPulse(source: SectorEntityToken, params: MPC_magnetarPulseParams): MPC_magnetarPulseTerrain {
            val ring = source.containingLocation.addRingBand(
                source,
                "planets",
                "MPCatmosphere3_blue",
                params.bandWidthInEngine,
                1,
                BASE_COLOR,
                params.bandWidthInEngine,
                params.middleRadius, //starts at the center
                4f // rotation speed i think?
            )
            ring.alwaysUseSensorFaderBrightness = true
            ring.setLocation(source.location.x, source.location.y)
            val terrain = source.containingLocation.addTerrain(
                "MPC_magnetarPulseTerrain",
                params
            ) as CampaignTerrainAPI
            val plugin = terrain.plugin as MPC_magnetarPulseTerrain
            plugin.ring = ring
            terrain.setLocation(source.location.x, source.location.y)
            return terrain.plugin as MPC_magnetarPulseTerrain
        }

        const val BASE_SHOCKWAVE_DURATION = 40f
        val BASE_COLOR = Color(37, 245, 200, 190)
        const val BASE_STRIKE_DAMAGE = 25

        /** At max interdict effectiveness, immobility is reduced by this percent. */
        const val MAX_INTERDICT_REINFORCEMENT = 1f
        /** MAX_INTERDICT_REINFORCEMENT is reached at this progress level of interdiction. */
        const val MAX_INTERDICT_PROGRESS_NEEDED = 0.6f
        const val INTERDICT_EXTRA_COOLDOWN = 3f

        const val DIST_NEEDED_TO_HIT = 500f

        const val BASE_SHOCKWAVE_SPEED = 250f
        const val INTERDICT_COMPLETE_GRACE_DAYS = 0.1f

        const val SHIELDS_FROM_TAG = "MPC_blocksMagnetarPulse"
        const val IGNORES_TAG = niko_MPC_ids.IMMUNE_TO_MAGNETAR_PULSE
    }

    class MPC_magnetarPulseParams(
        /** The distance this pulse will travel before completely expiring. */
        val distance: Float,
        /** Works with distance. When within this many SU of the source, the pulse does the max effect possible. Outside, it will fade from 100% to 0% opacity - hitting 0% at the end of lifespan.*/
        val maxEffectRange: Float,
        bandWidth: Float,
        relatedEntity: SectorEntityToken? = null,
        startingRange: Float = 0f,
        val spawnSounds: List<String> = listOf("mote_attractor_targeted_ship", "gate_explosion"),
        /** In SU. Does not increase the distance of the wave. */
        val speed: Float = BASE_SHOCKWAVE_SPEED,
        var damage: ExplosionEntityPlugin.ExplosionFleetDamage = ExplosionFleetDamage.LOW,
        var explosionDamageMult: Float = 1f,
        var explosionDisruptionMult: Float = 1f,
        var sourceFleet: CampaignFleetAPI? = null,
        var baseRepLoss: Float = 7f,
        val respectIgnore: Boolean = true
    ): RingParams(bandWidth, startingRange, relatedEntity)

    lateinit var ring: RingBandAPI

    @Transient
    var blockerUtil: MPC_rangeBlockerWithEnds = MPC_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_TAG)
        get() {
            if (field == null) field = MPC_rangeBlockerWithEnds(1440, 10000f, SHIELDS_FROM_TAG)
            return field
        }

    val alreadyHit = HashSet<CampaignFleetAPI>()

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        if (entity == null) return
        val casted = getCastedParams()
        if (relatedEntity?.containingLocation?.isCurrentLocation == true || entity.containingLocation?.isCurrentLocation == true) {
            for (id in casted.spawnSounds) {
                Global.getSoundPlayer().playSound(id, 1f, 1f, entity.location, Misc.ZERO)
            }
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val mult = getEffectMult(null)
        entity.forceSensorFaderBrightness(mult)

        val casted = getCastedParams()
        val days = Misc.getDays(amount)
        val adjustPos = casted.speed * days
        ring.middleRadius += adjustPos
        params.middleRadius = ring.middleRadius
        val castedRing = ring as RingBand
        for (layer in castedRing.activeLayers) {
            castedRing.render(layer, Global.getSector().viewport as CombatViewport)
        }

        if (mult <= 0f) {
            delete()
        }
    }

    private fun delete() {
        val containing = entity.containingLocation
        containing.removeEntity(entity)
        containing.removeEntity(ring)
    }

    fun getCastedParams(): MPC_magnetarPulseParams = params as MPC_magnetarPulseParams

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        if (entity !is CampaignFleetAPI) return

        if (entity.memoryWithoutUpdate.getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE) && !entity.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION)) {
            entity.memoryWithoutUpdate.set(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION, true, INTERDICT_COMPLETE_GRACE_DAYS)
        }

        if (!shouldAffectFleet(entity)) return
        affectFleet(entity)
    }

    fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        var dist = MathUtils.getDistance(getMiddle(), fleet.location)
        dist -= fleet.radius
        dist -= params.middleRadius
        if (dist > 0f) return false
        if (fleetIsImmune(fleet)) return false
        if (fleet in alreadyHit) return false
        if (fleetIsBlocked(fleet)) return false

        return true
    }

    fun getMiddle(): Vector2f {
        return (Vector2f(entity.location).translate(params.middleRadius, params.middleRadius))
    }

    fun affectFleet(fleet: CampaignFleetAPI) {
        val mult = getEffectMult(fleet)
        val casted = getCastedParams()

        val members = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return

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
                val volumeMult = 6f * damageFraction
                Global.getSoundPlayer()
                    .playSound("gate_explosion_fleet_impact", 1f, volumeMult, fleet.location, Misc.ZERO)
            }
        }

        //float strikeValue = totalValue * damageFraction * (0.5f + (float) Math.random() * 0.5f);


        //float strikeValue = totalValue * damageFraction * (0.5f + (float) Math.random() * 0.5f);
        val picker = WeightedRandomPicker<FleetMemberAPI>()
        for (member in members) {
            var w = 1f
            if (member.isFrigate) w *= 0.1f
            if (member.isDestroyer) w *= 0.2f
            if (member.isCruiser) w *= 0.5f
            picker.add(member, w)
        }

        val numStrikes = picker.items.size

        for (i in 0 until numStrikes) {
            val member = picker.pick() ?: return
            val crPerDep = member.deployCost
            //if (crPerDep <= 0) continue;
            val suppliesPerDep = member.stats.suppliesToRecover.modifiedValue
            if (suppliesPerDep <= 0 || crPerDep <= 0) return
            val suppliesPer100CR = suppliesPerDep * 1f / Math.max(0.01f, crPerDep)

            // half flat damage, half scaled based on ship supply cost cost
            var strikeSupplies = ((BASE_STRIKE_DAMAGE) + suppliesPer100CR) * 0.5f * damageFraction
            strikeSupplies *= casted.explosionDamageMult
            //strikeSupplies = suppliesPerDep * 0.5f * damageFraction;
            var strikeDamage = strikeSupplies / suppliesPer100CR * (0.75f + Math.random().toFloat() * 0.5f)
            strikeDamage *= 0.6f

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

        var shatterTimeMult = mult
        val interdictAbility = fleet.getAbility(Abilities.INTERDICTION_PULSE) as? InterdictionPulseAbility
        /** Non-null if an interdiction was attempted. */
        var interdictionEffectiveness: Float? = null
        if (interdictAbility != null) {
            val progressFraction: Float = interdictAbility.progressFraction

            if (fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION)) {
                interdictionEffectiveness = MAX_INTERDICT_REINFORCEMENT
            } else if (interdictAbility.isInProgress) {
                interdictionEffectiveness =
                    (progressFraction / MAX_INTERDICT_PROGRESS_NEEDED).coerceAtMost(MAX_INTERDICT_REINFORCEMENT)
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

        val immobileDur = ((10f * shatterTimeMult * casted.explosionDisruptionMult).roundNumTo(1)).coerceAtMost(MIN_DAYS_PER_PULSE * 0.8f)
        val immobileFromDays = Global.getSector().clock.convertToSeconds(immobileDur)
        val desc = "Drive field destroyed (${immobileDur.trimHangingZero()} days to repair)"

        for (ability in fleet.abilities.values) {
            if (!ability.spec.hasTag(Abilities.TAG_BURN + "+") && ability.id != Abilities.TRANSVERSE_JUMP) continue
            ability.deactivate()
            ability.cooldownLeft = ability.cooldownLeft.coerceAtLeast(immobileDur)
        }

        if (immobileFromDays > 0) {
            for (view in fleet.views) {
                view.setJitter(0.1f, immobileFromDays, BASE_COLOR, 2, 3f)
                view.setUseCircularJitter(true)
                view.setJitterDirection(Misc.ZERO)
                view.setJitterLength(immobileFromDays)
                view.setJitterBrightness(0.2f)
            }
        }

        fleet.stats.addTemporaryModMult(immobileDur, entity.id + "_magnetPulseAftermathEngines", desc, -500f, fleet.stats.fleetwideMaxBurnMod)
        fleet.memoryWithoutUpdate.set(DRIVE_BUBBLE_DESTROYED, true, immobileDur)

        val shoveDir = Misc.getAngleInDegrees(entity.location, fleet.location)
        val shoveIntensity = (damageFraction * 20f).coerceAtMost((casted.speed/625f)) // not arbitrary, it mostly keeps it locked to the speed of the pulse

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

        if (interdictionEffectiveness == null || interdictionEffectiveness < 1f) {
            fleet.addScript(ShoveFleetScript(fleet, shoveDir, shoveIntensity)) // EDIT
        }

        if (fleet.isInCurrentLocation) {
            val vis: VisibilityLevel = fleet.visibilityLevelToPlayerFleet
            if (fleet.isPlayerFleet || vis == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS || vis == VisibilityLevel.COMPOSITION_DETAILS) {
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

        val sourceFleet = casted.sourceFleet
        if (sourceFleet?.isPlayerFleet == true && fleet.knowsWhoPlayerIs() && sourceFleet.faction != fleet.faction) {
            val repParams = CustomRepImpact()
            repParams.delta = -casted.baseRepLoss * (mult * 10f)
            repParams.limit = RepLevel.INHOSPITABLE
            Global.getSector().adjustPlayerReputation(
                RepActionEnvelope(RepActions.CUSTOM, repParams, null, false),
                fleet.faction.id
            )
        }
    }

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
        if (fleet.hasTag(IGNORES_TAG) && getCastedParams().respectIgnore) return true

        return false
    }

    fun fleetIsBlocked(fleet: CampaignFleetAPI): Boolean {
        val location = fleet.location
        val angle = VectorUtils.getAngle(entity.location, location)
        val containing = entity.containingLocation ?: return false
        val blockers = containing.planets + containing.getEntitiesWithTag(SHIELDS_FROM_TAG)
        if (blockers.any { MathUtils.getDistance(it, fleet) <= 0f }) return true

        val range = blockerUtil.getBlockedRangeAt(angle)
        val dist = MathUtils.getDistance(entity.location, location)

        if (dist > range.first && dist < range.second) return true

        return false
    }

    override fun getEffectCategory(): String? {
        return null
    }

    val UID = Misc.genUID()
    override fun getTerrainId(): String? {
        return UID
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    override fun hasAIFlag(flag: Any?): Boolean {
        return false
    }
}