package data.scripts.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.impl.combat.MoteControlScript
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.prob
import data.utilities.niko_MPC_settings
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_spatialBreachStats: BaseShipSystemScript() {

    companion object {
        val PHASE_COLOR = Color(115, 11, 185, 255)

        const val SHIP_ALPHA_MULT: Float = 0.1f
        const val VULNERABLE_FRACTION: Float = 0f
        const val BASE_FLAT_SPEED_BOOST: Float = 75f

        const val MAX_TIME_MULT: Float = 3f
        const val FLUX_LEVEL_AFFECTS_SPEED: Boolean = true
        const val MIN_SPEED_MULT: Float = 0.65f
        const val BASE_FLUX_LEVEL_FOR_MIN_SPEED: Float = 0.75f

        const val MIN_INITIAL_MOTES_TO_SPAWN = 1
        const val MAX_INITIAL_MOTES_TO_SPAWN = 3

        const val SHIP_MOTE_SPAWNER_KEY = "MPC_omegaZigMoteSpawner"
        const val PARTICLE_SCRIPT_KEY = "MPC_omegaZigParticleScript"

        const val MAX_MOTES = 100

        fun ShipAPI.omegaPhaseStart() {
            val existing = getMoteSpawner()
            if (existing == null) {
                val new = MPC_omegaZigMoteSpawner(this)
                Global.getCombatEngine().addPlugin(new)
                setCustomData(SHIP_MOTE_SPAWNER_KEY, new)
            }

            val particle = getOmegaZipParticleSpawner()
            if (particle == null) {
                val new = MPC_omegaZigParticleSpawner(this)
                Global.getCombatEngine().addPlugin(new)
                setCustomData(PARTICLE_SCRIPT_KEY, new)
            }
        }

        fun ShipAPI.omegaPhaseEnd() {
            val existing = getMoteSpawner()
            existing?.ship?.setCustomData(SHIP_MOTE_SPAWNER_KEY, null)
            existing?.delete()

            val particle = getOmegaZipParticleSpawner()
            particle?.ship?.setCustomData(PARTICLE_SCRIPT_KEY, null)
            particle?.delete()
        }

        fun ShipAPI.getMoteSpawner(): MPC_omegaZigMoteSpawner? {
            return customData[SHIP_MOTE_SPAWNER_KEY] as? MPC_omegaZigMoteSpawner
        }

        fun ShipAPI.getOmegaZipParticleSpawner(): MPC_omegaZigParticleSpawner? {
            return customData[PARTICLE_SCRIPT_KEY] as? MPC_omegaZigParticleSpawner
        }
    }

    private var isPhased = false

    private var afterImageTimer = 0f

    protected var STATUSKEY1: Any = Any()
    protected var STATUSKEY2: Any = Any()
    protected var STATUSKEY3: Any = Any()
    protected var STATUSKEY4: Any = Any()


    fun getMaxTimeMult(stats: MutableShipStatsAPI): Float {
        return 1f + (MAX_TIME_MULT - 1f) * stats.dynamic.getValue(Stats.PHASE_TIME_BONUS_MULT)
    }

    protected fun getDisruptionLevel(ship: ShipAPI): Float {
        if (FLUX_LEVEL_AFFECTS_SPEED) {
            val threshold = ship.mutableStats.dynamic.getMod(
                Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD
            ).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED)
            if (threshold <= 0) return 1f
            var level = ship.hardFluxLevel / threshold
            if (level > 1f) level = 1f
            return level
        }
        return 0f
    }

    protected fun maintainStatus(playerShip: ShipAPI, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        val level = effectLevel
        val f = VULNERABLE_FRACTION

        var cloak = playerShip.phaseCloak
        if (cloak == null) cloak = playerShip.system
        if (cloak == null) return

        if (level > f) {
//			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
//					cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "can not be hit", false);
            Global.getCombatEngine().maintainStatusForPlayerShip(
                STATUSKEY2,
                cloak.specAPI.iconSpriteName, cloak.displayName, "time flow altered", false
            )
        } else {
//			float INCOMING_DAMAGE_MULT = 0.25f;
//			float percent = (1f - INCOMING_DAMAGE_MULT) * getEffectLevel() * 100;
//			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
//					spec.getIconSpriteName(), cloak.getDisplayName(), "damage mitigated by " + (int) percent + "%", false);
        }

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (level > f) {
                if (getDisruptionLevel(playerShip) <= 0f) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(
                        STATUSKEY3,
                        cloak.specAPI.iconSpriteName, "phase coils stable", "top speed at 100%", false
                    )
                } else {
                    //String disruptPercent = "" + (int)Math.round((1f - disruptionLevel) * 100f) + "%";
                    //String speedMultStr = Strings.X + Misc.getRoundedValue(getSpeedMult());
                    val speedPercentStr =
                        Math.round(getSpeedMult(playerShip, effectLevel) * 100f).toString() + "%"
                    Global.getCombatEngine().maintainStatusForPlayerShip(
                        STATUSKEY3,
                        cloak.specAPI.iconSpriteName,  //"phase coils at " + disruptPercent,
                        "phase coil stress",
                        "top speed at " + speedPercentStr, true
                    )
                }
            }
        }
    }

    fun getSpeedMult(ship: ShipAPI, effectLevel: Float): Float {
        if (getDisruptionLevel(ship) <= 0f) return 1f
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel)
    }

    override fun apply(
        stats: MutableShipStatsAPI,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        var id = id
        var ship = stats.entity as? ShipAPI ?: return
        var player = ship == Global.getCombatEngine().playerShip
        id = id + "_" + ship.id

        if (player) {
            maintainStatus(ship, state, effectLevel)
        }

        if (Global.getCombatEngine().isPaused) {
            return
        }

        if (state == ShipSystemStatsScript.State.COOLDOWN || state == ShipSystemStatsScript.State.IDLE) {
            isPhased = false
            afterImageTimer = 0f
            unapply(stats, id)
            return
        }

        val baseSpeedBonus = BASE_FLAT_SPEED_BOOST

        val level = effectLevel

        //float f = VULNERABLE_FRACTION;
        var levelForAlpha = level

        var cloak = ship.system
        if (!cloak.specAPI.isPhaseCloak) cloak = ship.phaseCloak

        if (FLUX_LEVEL_AFFECTS_SPEED) {
            if (state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.OUT || state == ShipSystemStatsScript.State.IN) {
                val mult = getSpeedMult(ship, effectLevel)
                if (mult < 1f) {
                    stats.maxSpeed.modifyMult(id + "_2", mult)
                } else {
                    stats.maxSpeed.unmodifyMult(id + "_2")
                }
                (cloak as PhaseCloakSystemAPI).minCoilJitterLevel = getDisruptionLevel(ship)
            }
        }

        val speedPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(baseSpeedBonus)
        val accelPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(baseSpeedBonus)
        stats.maxSpeed.modifyPercent(id, speedPercentMod * effectLevel)
        stats.maxTurnRate.modifyPercent(id, accelPercentMod * effectLevel)
        stats.turnAcceleration.modifyPercent(id, accelPercentMod * effectLevel)
        stats.acceleration.modifyPercent(id, accelPercentMod * effectLevel)
        stats.deceleration.modifyPercent(id, accelPercentMod * effectLevel)

        val speedMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult()
        val accelMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult()
        stats.maxSpeed.modifyMult(id, speedMultMod * effectLevel)
        stats.maxTurnRate.modifyPercent(id, accelMultMod * effectLevel)
        stats.turnAcceleration.modifyPercent(id, accelMultMod * effectLevel)
        stats.acceleration.modifyMult(id, accelMultMod * effectLevel)
        stats.deceleration.modifyMult(id, accelMultMod * effectLevel)

        stats.shieldUpkeepMult.modifyMult(id, 0.5f)

        if (state == ShipSystemStatsScript.State.IN || state == ShipSystemStatsScript.State.ACTIVE) {
            ship.isPhased = true
            levelForAlpha = level
            if (!isPhased) {
                val p = RiftCascadeMineExplosion.createStandardRiftParams(
                    PHASE_COLOR,
                    ship.shieldRadiusEvenIfNoShield * 1f
                )
                p.fadeOut = 0.15f
                p.hitGlowSizeMult = 0.25f
                p.underglow = Color(255, 175, 255, 50)
                p.withHitGlow = false
                p.noiseMag = 1.25f

                val e = Global.getCombatEngine().addLayeredRenderingPlugin(NegativeExplosionVisual(p))
                e.location.set(ship.location)

                //if (SotfModPlugin.GLIB) {
                if (niko_MPC_settings.graphicsLibEnabled) {
                    val ripple = RippleDistortion(ship.location, ship.velocity)
                    ripple.setIntensity(ship.collisionRadius * 0.75f)
                    ripple.setSize(ship.shieldRadiusEvenIfNoShield)
                    ripple.fadeInSize(0.15f)
                    ripple.fadeOutIntensity(0.5f)
                    DistortionShader.addDistortion(ripple)
                }

                spawnInitialMotes(ship)
                ship.omegaPhaseStart()

                isPhased = true
            }
        } else if (state == ShipSystemStatsScript.State.OUT) {
            if (level > 0.5f) {
                ship.isPhased = true
            } else {
                ship.isPhased = false
            }
            levelForAlpha = level
            ship.omegaPhaseEnd()
        }

        ship.extraAlphaMult = 1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha
        ship.setApplyExtraAlphaToEngines(true)

        val shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha
        var perceptionMult = shipTimeMult
        if (player) {
            perceptionMult = 1f + ((getMaxTimeMult(stats) - 1f) * 0.65f) * levelForAlpha
        }
        stats.timeMult.modifyMult(id, shipTimeMult)
        if (player) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / perceptionMult)
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }

    }

    private fun spawnInitialMotes(ship: ShipAPI) {
        var amount = MathUtils.getRandomNumberInRange(MIN_INITIAL_MOTES_TO_SPAWN, MAX_INITIAL_MOTES_TO_SPAWN)
        while (amount-- > 0) {
            MPC_omegaZigMoteSpawner.spawnMote(ship)
        }
    }


    override fun unapply(stats: MutableShipStatsAPI, id: String?) {
        var ship = stats.entity as? ShipAPI ?: return

        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)

        ship.isPhased = false
        ship.extraAlphaMult = 1f

        stats.maxSpeed.unmodify(id)
        stats.maxTurnRate.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.shieldUpkeepMult.unmodify(id)

        var cloak = ship.system as? PhaseCloakSystemAPI ?: return
        cloak.minCoilJitterLevel = 0f

        ship.omegaPhaseEnd()
    }

    class MPC_omegaZigMoteSpawner(
        val ship: ShipAPI
    ): BaseEveryFrameCombatPlugin() {

        companion object {
            const val MIN_SECONDS_BETWEEN_SPAWNS = 0.04f
            const val MAX_SECONDS_BETWEEN_SPAWNS = 0.07f

            const val CHANCE_FOR_MOTE_TO_BE_HOSTILE = 20 // percent
            const val WEAPON_ID = "motelauncher_hf"
            const val WEAPON_ID_TRAITOR = "motelauncher"

            fun spawnMote(source: ShipAPI): CombatEntityAPI? {
                val location = getMoteSpawnLoc(source)
                val moteData = MoteControlScript.getSharedData(source)
                if (moteData != null) {

                    for (mote in moteData.motes.toMutableList()) {
                        if (!Global.getCombatEngine().isEntityInPlay(mote)) {
                            moteData.motes.remove(mote)
                        }
                    }

                    if (moteData.motes.size >= MAX_MOTES) {
                        return null
                    }
                }

                var traitor = prob(CHANCE_FOR_MOTE_TO_BE_HOSTILE)
                val weaponId = if (traitor) WEAPON_ID_TRAITOR else WEAPON_ID
                val engine = Global.getCombatEngine()
                val mote = engine.spawnProjectile(
                    source,
                    null,
                    weaponId,
                    location,
                    MathUtils.getRandomNumberInRange(0f, 360f),
                    null
                ) as MissileAPI
                mote.setWeaponSpec(WEAPON_ID)
                mote.missileAI = MPC_omegaMoteAIScriptImpl(mote)
                mote.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)
                // if they could flame out/be affected by emp, that'd be bad since they don't expire for a
                // very long time so they'd be stuck disabled permanently, for practical purposes
                // thus: total emp resistance (which can't target them anyway, but if it did.)
                mote.empResistance = 10000

                val pitchOne = 0.75f
                val pitchTwo = if (traitor) 4f else 3f

                Global.getSoundPlayer().playSound("mote_attractor_launch_mote", pitchOne, 0.25f, location, Vector2f())
                Global.getSoundPlayer().playSound("rifttorpedo_explosion", pitchTwo, 0.2f, location, Vector2f())
                val p = RiftCascadeMineExplosion.createStandardRiftParams(
                    Color(100,165,255,175),
                    mote.collisionRadius * 0.5f
                )
                p.fadeOut = 0.15f
                p.hitGlowSizeMult = 0.25f
                p.underglow = Color(255, 175, 255, 50)
                p.withHitGlow = false
                p.noiseMag = 1.25f

                val e = Global.getCombatEngine().addLayeredRenderingPlugin(NegativeExplosionVisual(p))
                e.location.set(mote.location)

                if (traitor) {
                    val enemy = if (source.owner == 0) 1 else 0
                    mote.owner = enemy
                }

                Global.getCombatEngine().addPlugin(MPC_omegaMoteTargetScript(mote))
                //val nearestTarget = getNearestMoteTarget(mote) ?: return mote

                moteData.motes.add(mote)

                return mote
            }

            fun getMoteSpawnLoc(ship: ShipAPI): Vector2f {
                val collisionRadius = ship.collisionRadius
                var point = MathUtils.getRandomPointInCircle(ship.location, collisionRadius)
                for (entity in Global.getCombatEngine().allObjectGrid.getCheckIterator(ship.location, collisionRadius, collisionRadius)) {
                    if (entity is ShipAPI) {
                        if (entity.owner == ship.owner) continue
                    }
                    if (entity !is CombatEntityAPI) continue
                    val dist = MathUtils.getDistance(entity.location, point)
                    if (dist <= entity.collisionRadius) {
                        point = ship.location // failsafe, lol
                        break
                    }
                }

                return point
            }
        }

        val interval = IntervalUtil(MIN_SECONDS_BETWEEN_SPAWNS, MAX_SECONDS_BETWEEN_SPAWNS)

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            var amount = amount * engine.timeMult.modified
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                spawnMote(ship)
            }
        }

        fun delete() {
            Global.getCombatEngine().removePlugin(this)
        }
    }

    class MPC_omegaMoteTargetScript(
        val mote: MissileAPI
    ): BaseEveryFrameCombatPlugin() {

        val interval = IntervalUtil(0.2f, 0.3f)

        init {
            tryTarget()
        }

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return
            if (!engine.isEntityInPlay(mote)) {
                engine.removePlugin(this)
                return
            }

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                tryTarget()
            }
        }

        private fun tryTarget() {
            val missileAI = mote.unwrappedMissileAI as? MPC_omegaMoteAIScriptImpl ?: return
            if (missileAI.target != null) return
            if (mote.source != null && mote.source.owner != 100) {
                if (mote.owner != mote.source.owner) {
                    if (!mote.source.isPhased) {
                        missileAI.target = mote.source
                        mote.source = null
                        Global.getCombatEngine().removePlugin(this)
                    }
                    return
                }
            }
            val nearestTarget = AIUtils.getNearestEnemy(mote) ?: return
            missileAI.target = nearestTarget

            //Global.getCombatEngine().removePlugin(this)
        }
    }

    class MPC_omegaZigParticleSpawner(
        val ship: ShipAPI
    ): BaseEveryFrameCombatPlugin() {

        val interval = IntervalUtil(0.01f, 0.02f)

        init {
            makeParticles()
        }

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            if (Global.getCombatEngine().isPaused) return

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                makeParticles()
            }
        }

        private fun makeParticles() {
            val engine = Global.getCombatEngine()
            val dur = 30f
            val loc = MathUtils.getRandomPointInCircle(ship.location, ship.collisionRadius * 0.8f)
            engine.addNegativeNebulaParticle(
                loc,
                MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                ship.collisionRadius * 0.3f,
                0.1f,
                0.5f,
                0.5f,
                MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                Color(215, 30, 19, 60),
            )
            engine.addNegativeNebulaParticle(
                loc,
                MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                ship.collisionRadius * 0.3f,
                0.1f,
                0.5f,
                0.1f,
                MathUtils.getRandomNumberInRange(2.0f, 3.4f),
                Color(215, 30, 19, 60),
            )
        }

        fun delete() {
            Global.getCombatEngine().removePlugin(this)
        }
    }

}