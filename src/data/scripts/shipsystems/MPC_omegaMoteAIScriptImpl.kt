package data.scripts.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAIPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.impl.combat.MoteAIScript
import com.fs.starfarer.api.impl.combat.MoteControlScript
import com.fs.starfarer.api.impl.combat.MoteControlScript.SharedMoteAIData
import com.fs.starfarer.api.util.FaderUtil
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import kotlin.math.sin

class MPC_omegaMoteAIScriptImpl(
    val missile: MissileAPI
): MissileAIPlugin {

    protected var tracker: IntervalUtil = IntervalUtil(0.05f, 0.1f)

    protected var updateListTracker: IntervalUtil = IntervalUtil(0.05f, 0.1f)
    protected var missileList: MutableList<MissileAPI?> = ArrayList<MissileAPI?>()
    protected var hardAvoidList: MutableList<CombatEntityAPI> = ArrayList<CombatEntityAPI>()

    protected var r: Float = 0f

    var target: CombatEntityAPI? = null
    protected var data: SharedMoteAIData? = null

    //public void accumulate(FlockingData data, Vector2f )
    protected var flutterCheck: IntervalUtil = IntervalUtil(2f, 4f)
    protected var currFlutter: FaderUtil? = null
    protected var flutterRemaining: Float = 0f

    protected var elapsed: Float = 0f

    init {
        r = Math.random().toFloat()
        elapsed = -Math.random().toFloat() * 0.5f

        data = MoteControlScript.getSharedData(missile?.source)

        updateHardAvoidList()
    }

    fun updateHardAvoidList() {
        hardAvoidList.clear()

        var grid = Global.getCombatEngine().aiGridShips
        var iter = grid.getCheckIterator(
            missile!!.location,
            MoteAIScript.MAX_HARD_AVOID_RANGE * 2f,
            MoteAIScript.MAX_HARD_AVOID_RANGE * 2f
        )
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is ShipAPI) continue

            val ship = o

            if (ship.isFighter) continue
            hardAvoidList.add(ship)
        }

        grid = Global.getCombatEngine().aiGridAsteroids
        iter = grid.getCheckIterator(
            missile!!.location,
            MoteAIScript.MAX_HARD_AVOID_RANGE * 2f,
            MoteAIScript.MAX_HARD_AVOID_RANGE * 2f
        )
        while (iter.hasNext()) {
            val o = iter.next()
            if (o !is CombatEntityAPI) continue

            val asteroid = o
            hardAvoidList.add(asteroid)
        }
    }

    fun doFlocking() {
        if (missile!!.source == null) return

        val source = missile!!.source
        val engine = Global.getCombatEngine()

        var avoidRange = MoteAIScript.AVOID_RANGE
        val cohesionRange = MoteAIScript.COHESION_RANGE

        val sourceRejoin = source.collisionRadius + 200f

        val sourceRepel = source.collisionRadius + 50f
        val sourceCohesion = source.collisionRadius + 600f

        val sin = sin((data!!.elapsed * 1f).toDouble()).toFloat()
        val mult = 1f + sin * 0.25f
        avoidRange *= mult

        val total = Vector2f()
        val attractor = getAttractorLoc()

        if (attractor != null) {
            val dist = Misc.getDistance(missile!!.location, attractor)
            val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile!!.location, attractor))
            var f = dist / 200f
            if (f > 1f) f = 1f
            dir.scale(f * 3f)
            Vector2f.add(total, dir, total)

            avoidRange *= 3f
        }

        var hardAvoiding = false
        for (other in hardAvoidList) {
            val dist = Misc.getDistance(missile!!.location, other.location)
            val hardAvoidRange = other.collisionRadius + avoidRange + 50f
            if (dist < hardAvoidRange) {
                val dir = Misc.getUnitVectorAtDegreeAngle(
                    Misc.getAngleInDegrees(
                        other.location,
                        missile!!.location
                    )
                )
                val f = 1f - dist / (hardAvoidRange)
                dir.scale(f * 5f)
                Vector2f.add(total, dir, total)
                hardAvoiding = f > 0.5f
            }
        }


        //for (MissileAPI otherMissile : missileList) {
        for (otherMissile in data!!.motes) {
            if (otherMissile === missile) continue

            val dist = Misc.getDistance(missile!!.location, otherMissile.location)


            var w = otherMissile.maxHitpoints
            w = 1f

            val currCohesionRange = cohesionRange

            if (dist < avoidRange && otherMissile !== missile && !hardAvoiding) {
                val dir = Misc.getUnitVectorAtDegreeAngle(
                    Misc.getAngleInDegrees(
                        otherMissile.location,
                        missile!!.location
                    )
                )
                val f = 1f - dist / avoidRange
                dir.scale(f * w)
                Vector2f.add(total, dir, total)
            }

            if (dist < currCohesionRange) {
                val dir = Vector2f(otherMissile.velocity)
                Misc.normalise(dir)
                val f = 1f - dist / currCohesionRange
                dir.scale(f * w)
                Vector2f.add(total, dir, total)
            }


//			if (dist < cohesionRange && dist > avoidRange) {
//				//Vector2f dir = Utils.getUnitVectorAtDegreeAngle(Utils.getAngleInDegrees(missile.getLocation(), mote.getLocation()));
//				Vector2f dir = Utils.getUnitVectorAtDegreeAngle(Utils.getAngleInDegrees(mote.getLocation(), missile.getLocation()));
//				float f = dist / cohesionRange - 1f;
//				dir.scale(f * 0.5f);
//				Vector2f.add(total, dir, total);
//			}
        }

        if (missile!!.source != null) {
            val dist = Misc.getDistance(missile!!.location, source.location)
            if (dist > sourceRejoin) {
                val dir = Misc.getUnitVectorAtDegreeAngle(
                    Misc.getAngleInDegrees(
                        missile!!.location,
                        source.location
                    )
                )
                val f = dist / (sourceRejoin + 400f) - 1f
                dir.scale(f * 0.5f)

                Vector2f.add(total, dir, total)
            }

            if (dist < sourceRepel) {
                val dir = Misc.getUnitVectorAtDegreeAngle(
                    Misc.getAngleInDegrees(
                        source.location,
                        missile!!.location
                    )
                )
                val f = 1f - dist / sourceRepel
                dir.scale(f * 5f)
                Vector2f.add(total, dir, total)
            }

            if (dist < sourceCohesion && source.velocity.length() > 20f) {
                val dir = Vector2f(source.velocity)
                Misc.normalise(dir)
                val f = 1f - dist / sourceCohesion
                dir.scale(f * 1f)
                Vector2f.add(total, dir, total)
            }


            // if not strongly going anywhere, circle the source ship; only kicks in for lone motes
            if (total.length() <= 0.05f) {
                val offset = if (r > 0.5f) 90f else -90f
                val dir = Misc.getUnitVectorAtDegreeAngle(
                    Misc.getAngleInDegrees(missile!!.location, source.location) + offset
                )
                val f = 1f
                dir.scale(f * 1f)
                Vector2f.add(total, dir, total)
            }
        }

        if (total.length() > 0) {
            val dir = Misc.getAngleInDegrees(total)
            engine.headInDirectionWithoutTurning(missile, dir, 10000f)

            if (r > 0.5f) {
                missile!!.giveCommand(ShipCommand.TURN_LEFT)
            } else {
                missile!!.giveCommand(ShipCommand.TURN_RIGHT)
            }
            missile!!.engineController.forceShowAccelerating()
        }
    }
    override fun advance(amount: Float) {
        if (missile!!.isFizzling) return
        //if (missile!!.source == null) return

        elapsed += amount

        updateListTracker.advance(amount)
        if (updateListTracker.intervalElapsed()) {
            updateHardAvoidList()
        }


        //missile.getEngineController().getShipEngines().get(0).
        if (flutterRemaining <= 0) {
            flutterCheck.advance(amount)
            if (flutterCheck.intervalElapsed() &&
                (Math.random().toFloat() > 0.9f ||
                        (data!!.attractorLock != null && Math.random().toFloat() > 0.5f))
            ) {
                flutterRemaining = 2f + Math.random().toFloat() * 2f
            }
        }


//		if (flutterRemaining > 0) {
//			flutterRemaining -= amount;
//			if (currFlutter == null) {
//				float min = 1/15f;
//				float max = 1/4f;
//				float dur = min + (max - min) * (float) Math.random();
//				//dur *= 0.5f;
//				currFlutter = new FaderUtil(0f, dur/2f, dur/2f, false, true);
//				currFlutter.fadeIn();
//			}
//			currFlutter.advance(amount);
//			if (currFlutter.isFadedOut()) {
//				currFlutter = null;
//			}
//		} else {
//			currFlutter = null;
//		}
//
//		if (currFlutter != null) {
//			missile.setGlowRadius(currFlutter.getBrightness() * 30f);
//		} else {
//			missile.setGlowRadius(0f);
//		}
//		if (true) {
//			doFlocking();
//			return;
//		}
        if (elapsed >= 0.5f) {
            var wantToFlock = !isTargetValid()
            if (data!!.attractorLock != null) {
                val dist = Misc.getDistance(missile!!.location, data!!.attractorLock.location)
                if (dist > data!!.attractorLock.collisionRadius + MoteAIScript.ATTRACTOR_LOCK_STOP_FLOCKING_ADD) {
                    wantToFlock = true
                }
            }

            if (wantToFlock) {
                doFlocking()
            } else {
                val engine = Global.getCombatEngine()
                val targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50f)
                engine.headInDirectionWithoutTurning(
                    missile,
                    Misc.getAngleInDegrees(missile!!.location, targetLoc),
                    10000f
                )
                //AIUtils.turnTowardsPointV2(missile, targetLoc);
                if (r > 0.5f) {
                    missile!!.giveCommand(ShipCommand.TURN_LEFT)
                } else {
                    missile!!.giveCommand(ShipCommand.TURN_RIGHT)
                }
                missile!!.engineController.forceShowAccelerating()
            }
        }

        tracker.advance(amount)
        if (tracker.intervalElapsed()) {
            if (elapsed >= 0.5f) {
                acquireNewTargetIfNeeded()
            }
            //causeEnemyMissilesToTargetThis();
        }
    }


    protected fun isTargetValid(): Boolean {
        if (target == null || (target is ShipAPI && (target as ShipAPI).isPhased)) {
            return false
        }
        val engine = Global.getCombatEngine()

        if (target != null && target is ShipAPI && (target as ShipAPI).isHulk) return false

        var list: MutableList<*>? = null
        if (target is ShipAPI) {
            list = engine.ships
        } else {
            list = engine.missiles
        }
        return target != null && list.contains(target) && target!!.owner != missile!!.owner
    }

    protected fun acquireNewTargetIfNeeded() {
        if (target != null && isTargetValid()) return
        if (missile.source == null) return

        if (data!!.attractorLock != null) {
            target = data!!.attractorLock
            return
        }

        val engine = Global.getCombatEngine()


        // want to: target nearest missile that is not targeted by another two motes already
        val owner = missile!!.owner

        val maxMotesPerMissile = 2
        val maxDistFromSourceShip = MoteControlScript.MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD
        val maxDistFromAttractor = MoteControlScript.MAX_DIST_FROM_ATTRACTOR_TO_ENGAGE_AS_PD

        var minDist = Float.Companion.MAX_VALUE
        var closest: CombatEntityAPI? = null
        for (other in engine.missiles) {
            if (other.owner == owner) continue
            if (other.owner == 100) continue
            if (other.spec.typeString == "MOTE" && other.source == missile.source) continue
            val distToTarget = Misc.getDistance(missile!!.location, other.location)

            if (distToTarget > minDist) continue
            if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue

            var distFromAttractor = Float.Companion.MAX_VALUE
            if (data!!.attractorTarget != null) {
                distFromAttractor = Misc.getDistance(other.location, data!!.attractorTarget)
            }
            val distFromSource = Misc.getDistance(other.location, missile!!.source.location)
            if (distFromSource > maxDistFromSourceShip &&
                distFromAttractor > maxDistFromAttractor
            ) continue

            if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue
            if (distToTarget < minDist) {
                closest = other
                minDist = distToTarget
            }
        }

        for (other in engine.ships) {
            if (other.owner == owner) continue
            if (other.owner == 100) continue
            if (!other.isFighter) continue
            val distToTarget = Misc.getDistance(missile!!.location, other.location)
            if (distToTarget > minDist) continue
            if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue

            var distFromAttractor = Float.Companion.MAX_VALUE
            if (data!!.attractorTarget != null) {
                distFromAttractor = Misc.getDistance(other.location, data!!.attractorTarget)
            }
            val distFromSource = Misc.getDistance(other.location, missile!!.source.location)
            if (distFromSource > maxDistFromSourceShip &&
                distFromAttractor > maxDistFromAttractor
            ) continue

            if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue
            if (distToTarget < minDist) {
                closest = other
                minDist = distToTarget
            }
        }

        target = closest
    }

    protected fun getNumMotesTargeting(other: CombatEntityAPI?): Int {
        var count = 0
        for (mote in data!!.motes) {
            if (mote === missile) continue
            if (mote.unwrappedMissileAI is MoteAIScript) {
                val ai = mote.unwrappedMissileAI as MoteAIScript
                if (ai.getTarget() === other) {
                    count++
                }
            }
        }
        return count
    }

    fun getAttractorLoc(): Vector2f? {
        var attractor: Vector2f? = null
        if (data!!.attractorTarget != null) {
            attractor = data!!.attractorTarget
            if (data!!.attractorLock != null) {
                attractor = data!!.attractorLock.location
            }
        }
        return attractor
    }


    fun render() {
    }
}