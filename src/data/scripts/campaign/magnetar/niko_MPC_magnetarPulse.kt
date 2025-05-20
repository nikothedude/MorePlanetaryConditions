package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.*
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.ShoveFleetScript
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.MPC_delayedExecution
import data.scripts.campaign.listeners.niko_MPC_saveListener
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.MIN_DAYS_PER_PULSE
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.DRIVE_BUBBLE_DESTROYED
import data.utilities.niko_MPC_ids.IMMUNE_TO_MAGNETAR_PULSE
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_stringUtils.toPercent
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.rotateAroundOrigin
import java.awt.Color

class niko_MPC_magnetarPulse: ExplosionEntityPlugin(), niko_MPC_saveListener {

    companion object {
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
    }

    class MPC_magnetarPulseParams(where: LocationAPI,
                                  loc: Vector2f,
                                  radius: Float,
                                  durationMult: Float,
                                  val shockwaveSpeed: Float = BASE_SHOCKWAVE_SPEED,
                                  val shockwaveDuration: Float = BASE_SHOCKWAVE_DURATION,
                                  val shockwaveAccel: Float? = null,
                                  color: Color = BASE_COLOR
    ): ExplosionParams(color, where, loc, radius, durationMult) {
        var baseRepLoss = 0f
        var sourceFleet: CampaignFleetAPI? = null
        var explosionDamageMult = 1f
        /** Higher = longer drive field disruption. */
        var explosionDisruptionMult = 1f
        var makeParticlesMaxVelocityImmediately = false
        var respectIgnore = true
    }

    var noEffectShockwaveDurationThreshold: Float = 0f // once we have this much duration left we dont have any effects
    var initialShockwaveDuration: Float = 0f
    @Transient
    var blockerUtil: MPC_rangeBlockerWithEnds? = null

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        doParamsCheck()

        Global.getSector().listenerManager.addListener(this)

        val castedParams = getCastedParams()
        if (castedParams.shockwaveAccel != null) {
            shockwaveAccel = castedParams.shockwaveAccel
        }
        shockwaveSpeed = castedParams.shockwaveSpeed
        initialShockwaveDuration = castedParams.shockwaveDuration
        shockwaveDuration = castedParams.shockwaveDuration
        noEffectShockwaveDurationThreshold = (shockwaveDuration * 0.15f)
        blockerUtil = MPC_rangeBlockerWithEnds(1440, 10000f)

        for (particle in particles) {
            particle.maxDur = shockwaveDuration
            particle.i = 0
            particle.j = 0
            if (castedParams.makeParticlesMaxVelocityImmediately) {
                val accel = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(particle.offset))
                accel.scale(shockwaveSpeed)
                particle.vel.x += accel.x
                particle.vel.y += accel.y
            }
        }

        sprite = getSprite()

        if (params.where.isCurrentLocation) {
            Global.getSoundPlayer().playSound("mote_attractor_targeted_ship", 1f, 1f, params.loc, Misc.ZERO)
        }

        return
    }

    private fun doParamsCheck() {
        if (params !is MPC_magnetarPulseParams) {
            niko_MPC_debugUtils.displayError("wrong param types passed into magnetar pulse, using default, logging info")
            niko_MPC_debugUtils.log.info("${params.damage}, ${params.color}")
            params = MPC_magnetarPulseParams(
                params.where,
                params.loc,
                params.radius,
                params.durationMult
            )
        }
    }

    private fun getCastedParams(): MPC_magnetarPulseParams {
        doParamsCheck()
        return params as MPC_magnetarPulseParams
    }

    override fun advance(amount: Float) {
        blockerUtil?.updateLimits(entity, entity.starSystem?.star, 0.5f)
        blockerUtil?.advance(amount, 100f, 0.5f)

        super.advance(amount)
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        if (blockerUtil != null && !blockerUtil!!.wasEverUpdated()) {
            blockerUtil!!.updateAndSync(entity, entity.starSystem?.star, 0.1f)
        }

        var alphaMult = viewport!!.alphaMult
        alphaMult *= entity.sensorFaderBrightness
        alphaMult *= entity.sensorContactFaderBrightness
        if (alphaMult <= 0) return

        val x = entity.location.x
        val y = entity.location.y


        //Color color = params.color;
        //color = Misc.setAlpha(color, 30);
        val b = alphaMult

        sprite.texWidth = 0.25f
        sprite.texHeight = 0.25f
        sprite.setAdditiveBlend()

        for (p in particles) {
            var size = p.size
            size *= p.scale

            val loc = Vector2f(x + p.offset.x, y + p.offset.y)

            //val rotated = Vector2f(loc).rotateAroundOrigin(-p.angle, loc)

            val checkLocOne = Vector2f(loc).translate((p.scale + p.size) * 2f, 0f).rotateAroundOrigin(p.angle, loc)
            val checkLocTwo = Vector2f(loc).translate(-(p.scale + p.size) * 2f, 0f).rotateAroundOrigin(p.angle, loc)

            var a = 1f
            a = 0.33f

            sprite.texX = p.i * 0.25f
            sprite.texY = p.j * 0.25f

            sprite.angle = p.angle
            sprite.setSize(size, size)
            sprite.alphaMult = b * a * p.getBrightness()
            /*if (isProtected(loc) || isProtected(checkLocOne) || isProtected(checkLocTwo)) {
                sprite.alphaMult *= 0.1f
            }*/
            sprite.color = p.color
            sprite.renderAtCenter(loc.x, loc.y)
        }
    }

    override fun applyDamageToFleets() {

        if (shockwaveDuration <= noEffectShockwaveDurationThreshold) {
            return
        }
        if (params.damage == null || params.damage == ExplosionFleetDamage.NONE) {
            return
        }
        var shockwaveDist = 0f
        for (p in particles) {
            shockwaveDist = shockwaveDist.coerceAtLeast(p.offset.length())

            /*val location = Vector2f(entity.location).translate(p.offset.x, p.offset.y)
            val protectionRating: Float = getProtectionRating(location)
            if (protectionRating > 0f) {
                p.brightness /= 2f * protectionRating
            } else {
                p.brightness *= 2f * protectionRating
            }*/
        }
        /*var maxScale = 0f
        for (particle in particles) {
            if (particle.scale > maxScale) {
                maxScale = particle.scale
            }
        }*/
        for (fleet in entity.containingLocation.fleets) {
            if (fleet.memoryWithoutUpdate[IMMUNE_TO_MAGNETAR_PULSE] == true && getCastedParams().respectIgnore) continue
            val id = fleet.id
            if (damagedAlready.contains(id)) continue
            if (fleet.memoryWithoutUpdate.getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE) && !fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION)) {
                fleet.memoryWithoutUpdate.set(niko_MPC_ids.IMMUNE_TO_PULSE_DUE_TO_RECENT_INTERDICTION, true, INTERDICT_COMPLETE_GRACE_DAYS)
            }

            val dist = Misc.getDistance(fleet, entity)
            if (dist < shockwaveDist) {
                damagedAlready.add(id)
                if (getCastedParams().sourceFleet == fleet) return
                if (fleet.isStationMode) return
                val distNeededToHit = ((shockwaveDist - DIST_NEEDED_TO_HIT)).coerceAtLeast(0f)

                if (dist <= distNeededToHit) {
                    continue
                }

                var skipBecauseOfBlocker = false
                val point = fleet.location
                if (isProtected(point)) {
                    if (fleet.isPlayerFleet) {
                        val discoveredInterdictTech = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")
                        if (discoveredInterdictTech) break
                        MPC_delayedExecution(
                            @JvmSerializableLambda {
                                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")) {
                                    Global.getSector().campaignUI.showInteractionDialog(
                                        RuleBasedInteractionDialogPluginImpl("MPC_hidMagnetarPulseFirstTime"),
                                        Global.getSector().playerFleet
                                    )
                                }
                            },
                            0.1f,
                            false,
                            useDays = true
                        ).start()
                    }
                    break
                }

                var damageMult = 1f - dist / params.radius
                if (damageMult > 1f) damageMult = 1f
                if (damageMult < 0.1f) damageMult = 0.1f
                if (dist < entity.radius + params.radius * 0.1f) damageMult = 1f
                applyDamageToFleet(fleet, damageMult)
            }
        }
    }

    fun isProtected(location: Vector2f): Boolean {
        //val actualLoc = Vector2f(entity.location).translate(location.x, location.y)
        val angle = VectorUtils.getAngle(entity.location, location)
        val range = blockerUtil!!.getBlockedRangeAt(angle)
        val dist = MathUtils.getDistance(entity.location, location)

        if (dist > range.first && dist < range.second) return true

        for (iterEntity in entity.containingLocation.planets + entity.containingLocation.getEntitiesWithTag(niko_MPC_ids.BLOCKS_MAGNETAR_PULSE_TAG)) {
            if (iterEntity.isStar) continue
            val distFromBlocker = Misc.getDistance(iterEntity.location, location)
            if (distFromBlocker <= iterEntity.radius) {
                return true
            }
        }
        return false
    }

    override fun applyDamageToFleet(fleet: CampaignFleetAPI?, damageMult: Float) {
        if (fleet == null) return
        val castedParams = getCastedParams()
        var damageMult = (shockwaveDuration / initialShockwaveDuration) * 0.15f
        val members = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return

        var totalValue = 0f
        for (member in members) {
            totalValue += member.stats.suppliesToRecover.modifiedValue
        }
        if (totalValue <= 0) return

        var damageFraction = 0f
        damageFraction = when (params.damage) {
            ExplosionFleetDamage.NONE -> return
            ExplosionFleetDamage.LOW -> 0.4f
            ExplosionFleetDamage.MEDIUM -> 0.7f
            ExplosionFleetDamage.HIGH -> 0.8f
            ExplosionFleetDamage.EXTREME -> 0.9f
            else -> return
        }

        damageFraction *= damageMult

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
            strikeSupplies *= castedParams.explosionDamageMult
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

        var shatterTimeMult = damageMult
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

        val immobileDur = ((10f * shatterTimeMult * castedParams.explosionDisruptionMult).roundNumTo(1)).coerceAtMost(MIN_DAYS_PER_PULSE * 0.8f)
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
        val shoveIntensity = (damageFraction * 20f).coerceAtMost((shockwaveSpeed/625f)) // not arbitrary, it mostly keeps it locked to the speed of the pulse

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
                        MPC_delayedExecution(
                            @JvmSerializableLambda {
                                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")) {
                                    Global.getSector().campaignUI.showInteractionDialog(
                                        RuleBasedInteractionDialogPluginImpl("MPC_hitByMagnetarPulseFirstTime"),
                                        Global.getSector().playerFleet
                                    )
                                }
                            },
                            0.1f,
                            false,
                            useDays = true
                        ).start()
                    }
                }
            }
        }

        val sourceFleet = castedParams.sourceFleet
        if (sourceFleet?.isPlayerFleet == true && fleet.knowsWhoPlayerIs() && sourceFleet.faction != fleet.faction) {
            val repParams = CustomRepImpact()
            repParams.delta = -castedParams.baseRepLoss * (damageMult * 10f)
            repParams.limit = RepLevel.INHOSPITABLE
            Global.getSector().adjustPlayerReputation(
                RepActionEnvelope(RepActions.CUSTOM, repParams, null, false),
                fleet.faction.id
            )
        }
    }

    /*override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI) {
        var alphaMult = viewport.alphaMult
        alphaMult *= entity.sensorFaderBrightness
        alphaMult *= entity.sensorContactFaderBrightness
        if (alphaMult <= 0) return
        val x = entity.location.x
        val y = entity.location.y

        //Color color = params.color;
        //color = Misc.setAlpha(color, 30);
        val b = alphaMult
        sprite.setTexWidth(0.25f)
        sprite.setTexHeight(0.25f)
        sprite.setAdditiveBlend()
        for (p in particles) {
            var size = p.size
            size *= p.scale
            val loc = Vector2f(x + p.offset.x, y + p.offset.y)
            var a = 1f
            a = 0.33f
            sprite.setTexX(p.i * 0.25f)
            sprite.setTexY(p.j * 0.25f)
            sprite.angle = p.angle
            sprite.setSize(size, size)
            sprite.alphaMult = a // EDIT
            sprite.color = p.color
            sprite.renderAtCenter(loc.x, loc.y)
        }
    }*/

    override fun getRenderRange(): Float {
        return shockwaveRadius * 2
    }

    override fun beforeGameSave() {
        return
    }

    override fun onGameLoad() {
        sprite = getSprite()
        blockerUtil = MPC_rangeBlockerWithEnds(1440, 10000f)
    }

    fun getSprite(): SpriteAPI = Global.getSettings().getSprite("misc", "nebula_particles")

    override fun afterGameSave() {
        return
    }

    override fun onGameSaveFailed() {
        return
    }

}