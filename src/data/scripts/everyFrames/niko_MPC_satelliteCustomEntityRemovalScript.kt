package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_memoryUtils.deleteMemoryKey
import data.utilities.niko_MPC_satelliteUtils.purgeSatellitesFromEntity

class niko_MPC_satelliteCustomEntityRemovalScript(var market: MarketAPI, var conditionId: String, val entity: SectorEntityToken = market.primaryEntity)
    : EveryFrameScript {

    protected var done = false

    init {
        //market.memoryWithoutUpdate[niko_MPC_ids.satelliteCustomEntityRemoverScriptId] = this
    }

    override fun isDone(): Boolean {
        return done
    }

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun advance(amount: Float) {
        var shouldRemove = true
        if (!entity.isAlive || entity.tags.contains(Tags.FADING_OUT_AND_EXPIRING)) { //currently in the process of deleting
            if (market != null) {
                for (condition in market.conditions) {
                    if (condition.id == conditionId) { //compare each condition and see if its ours
                        shouldRemove = false //if it is, we dont need to remove the satellites
                        break
                    }
                }
            } else shouldRemove = false //to tell the truth ive got no idea of waht to do if the entity has no market. that should never happen ever
        }
        if (shouldRemove) { //if we should remove it, we completely remove all parts of the satellite framework from the entity
            purgeSatellitesFromEntity(entity!!)
        }
        prepareForGarbageCollection() //the point of this script is simply to check on the next frame if the condition is still present
        done = true //and ONLY the next frame. this works because unapply() is always called on condition removal of a market,
        // meaning that no matter what, if a condition is removed, this will be added, and will check on the next frame if it was a removal or reapply.
        // since its only for the next frame, we should ALWAYS remove it, even if a condition wasnt changed, to avoid unneccessary performance loss
    }

    private fun prepareForGarbageCollection() {
        if (entity != null) {
            deleteMemoryKey(entity!!.memoryWithoutUpdate, niko_MPC_ids.satelliteCustomEntityRemoverScriptId)
            entity!!.removeScript(this)
            entity = null
        }
        conditionId = null
        done = true
    }
}