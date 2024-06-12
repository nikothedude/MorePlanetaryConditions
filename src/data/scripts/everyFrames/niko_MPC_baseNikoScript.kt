package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.campaign.LocationAPI
import data.utilities.niko_MPC_debugUtils

/** A generic base class filled with funcs and vars that I use a lot in my common scripting. */
abstract class niko_MPC_baseNikoScript: EveryFrameScriptWithCleanup{
    var started: Boolean = false
    private var done: Boolean = false
    var deleted: Boolean = false

    override fun isDone(): Boolean {
        return done
    }

    override fun cleanup() {
        delete()
    }

    /** Returns false in an error state. Make sure to return false if super returned false before you do anything. */
    open fun delete(): Boolean {
        done = true
        stop()
        if (deleted) {
            niko_MPC_debugUtils.displayError("$this was deleted despite having deleted set to true")
            return false
        }
        return true
    }

    open fun getPrimaryLocation(): LocationAPI? {
        return null
    }

    protected abstract fun startImpl()
    protected abstract fun stopImpl()

    open fun start() {
        if (!started) startImpl()
        started = true
    }

    fun stop() {
        stopImpl()
        started = false
    }
}
