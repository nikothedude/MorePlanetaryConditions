package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI

/** Base class for any script that uses deltatime, or the stored time between frames, to operate. */
abstract class niko_MPC_deltaTimeScript: niko_MPC_baseNikoScript() {
    /** If true, [canAdvance] will always return true when the player's location is the same as [getPrimaryLocation].*/
    open val onlyUseDeltaIfPlayerNotNear: Boolean = false
    /** If true, [canAdvance] will short-circuit to comparing [deltaTime] to 1f.*/
    open val doOneSecondDelayIfPlayerNotNear: Boolean = false
    /** The default value of which [deltaTime] will be compared to, and will be subtracted by if it is indeed more or equal to.*/
    abstract val thresholdForAdvancement: Float
    /** The time, in seconds, since the last [advance] call. */
    var deltaTime: Float = 0f

    /** Must be called before any major operation in [advance] to operate, and said [advance] must return if this returns false.
     * If it short circuits due to [onlyUseDeltaIfPlayerNotNear], [deltaTime] is unmodified. Else, [deltaTime] is incremented by
     * [amount]. After that, if [deltaTime] exceeds or meets the designated threshold, [deltaTime] subtracts the threshold
     * from itself and returns true. Otherwise, it returns false.*/
    fun canAdvance(amount: Float): Boolean {
        val playerLocation = Global.getSector()?.playerFleet?.location // if no fleet exists, it has no location, so this works
        val playerOrOurLocationNull = (playerLocation == null || getPrimaryLocation() == null)
        val playerLocationEqualsOurLocation = (playerLocation == getPrimaryLocation())
        if (onlyUseDeltaIfPlayerNotNear && (!playerOrOurLocationNull || playerLocationEqualsOurLocation)) return true
        deltaTime += amount
        var threshold = thresholdForAdvancement
        if (doOneSecondDelayIfPlayerNotNear && (playerOrOurLocationNull || !playerLocationEqualsOurLocation)) threshold = 1f
        if (deltaTime >= threshold) {
            deltaTime -= threshold
            return true
        }
        return false
    }
}