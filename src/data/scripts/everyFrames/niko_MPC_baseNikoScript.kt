package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScript

abstract class niko_MPC_baseNikoScript: EveryFrameScript{
    var done: Boolean = false

    override fun isDone(): Boolean {
        return done
    }

    protected open fun prepareForGarbageCollection() {
        done = true
    }
}
