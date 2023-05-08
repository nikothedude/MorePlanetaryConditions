package data.scripts.campaign.econ.conditions

import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript

interface hasDeletionScript<T: deletionScript?> {
    var deletionScript: T?

    fun startDeletionScript(vararg args: Any): T? {
        val script = getRealDeletionScript(*args)
        script?.start()

        return script
    }

    /** Returns either [deletionScript] or, if it is null, a new script from [createNewDeletionScript], and sets
     * [deletionScript] to it. */
    open fun getRealDeletionScript(vararg args: Any): T? {
        val script = deletionScript ?: createNewDeletionScript(*args) ?: return null
        deletionScript = script

        return script
    }

    open fun createNewDeletionScript(vararg args: Any): T? {
        val script = createDeletionScriptInstance(*args)

        return script
    }

    abstract fun createDeletionScriptInstance(vararg args: Any): T
}