package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import data.utilities.niko_MPC_debugUtils

/** A generic base class filled with funcs and vars that I use a lot in my common scripting. */
abstract class niko_MPC_baseNikoScript: EveryFrameScriptWithCleanup{
    private var done: Boolean = false
    private var deleted: Boolean = false

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

    abstract fun start()
    abstract fun stop()
}
