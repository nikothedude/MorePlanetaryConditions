package data.scripts.everyFrames

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.hasDeletionScript

abstract class deletionScript(open val hasDeletionScript: hasDeletionScript<out deletionScript?>): niko_MPC_baseNikoScript() {
    var runs = 0
    open var thresholdTilEnd = 250

    abstract fun deleteItem()

    override fun advance(amount: Float) {
        runs++

        if (shouldDelete()) {
            deleteItem()
        }
        delete()
    }

    abstract fun shouldDelete(): Boolean
}