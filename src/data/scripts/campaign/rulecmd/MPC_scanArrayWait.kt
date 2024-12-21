package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.IsSeenByAnyFleet
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.RuleException
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class MPC_scanArrayWait: BaseCommandPlugin() {
    companion object {
        const val MAX_DIST_FROM_TARGET = 50f
    }

    private var waitScript: EveryFrameScript? = null
    private var indicator: CampaignProgressIndicatorAPI? = null
    private var handle: Misc.VarAndMemory? = null
    private var finished: Misc.VarAndMemory? = null
    private var interrupted: Misc.VarAndMemory? = null
    private var inProgress: Misc.VarAndMemory? = null

    //Wait $handle duration $finished $interrupted $inProgress $text
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI,
        params: List<Misc.Token>,
        memoryMap: Map<String?, MemoryAPI?>?
    ): Boolean {
        if (params.size != 6) {
            throw RuleException("Wait usage: Wait \$handle duration \$finished \$interrupted \$inProgress <text>")
        }
        handle = params[0].getVarNameAndMemory(memoryMap)
        //val durationDays = params[1].string.toFloat()
        val durationDays: Float = dialog.interactionTarget.memoryWithoutUpdate[params[1].string] as Float
        finished = params[2].getVarNameAndMemory(memoryMap)
        interrupted = params[3].getVarNameAndMemory(memoryMap)
        inProgress = params[4].getVarNameAndMemory(memoryMap)
        var text: String? = "Waiting"
        if (params.size >= 5) {
            text = params[5].getString(memoryMap)
        }

        //Global.getSoundPlayer().playUISound("ui_wait_start", 1, 1);
        val target = dialog.interactionTarget
        val playerFleet = Global.getSector().playerFleet
        playerFleet.interactionTarget = null
        val offset = Vector2f.sub(playerFleet.location, target.location, Vector2f())
        //		float dir = Misc.getAngleInDegrees(offset);
//		offset = Misc.getUnitVectorAtDegreeAngle(dir)
        val len = offset.length()
        val radSum = playerFleet.radius + target.radius - 1f
        if (len > 0) {
            offset.scale(radSum / len)
        } else {
            offset[radSum] = 0f
        }
        indicator = Global.getFactory().createProgressIndicator(text, target, durationDays)
        target.containingLocation.addEntity(indicator)
        target.removeTag(Tags.HAS_INTERACTION_DIALOG)
        waitScript = object : BaseCampaignEventListenerAndScript(durationDays + 0.1f) {
            private var elapsedDays = 0f
            private var done = false
            private var battleOccured = false
            private var interactedWithSomethingElse = false
            override fun runWhilePaused(): Boolean {
                return false
            }

            override fun isDone(): Boolean {
                return done
            }

            override fun advance(amount: Float) {
                val clock = Global.getSector().clock
                //Global.getSector().campaignUI.setDisallowPlayerInteractionsForOneFrame()
                val days = clock.convertToDays(amount)
                elapsedDays += days
                inProgress!!.memory[inProgress!!.name] = true
                inProgress!!.memory.expire(inProgress!!.name, 0.1f)

//				float sinceLastBattle = clock.getElapsedDaysSince(Global.getSector().getLastPlayerBattleTimestamp());
//				if (sinceLastBattle <= elapsedDays) {
                val tooFar = (MathUtils.getDistance(playerFleet, target) > MAX_DIST_FROM_TARGET)
                val seen = IsSeenByAnyFleet().execute(ruleId, dialog, ArrayList<Misc.Token>(), memoryMap)
                if (tooFar || battleOccured || interactedWithSomethingElse || seen) {
                    done = true
                    target.addTag(Tags.HAS_INTERACTION_DIALOG)
                    interrupted!!.memory[interrupted!!.name] = true
                    interrupted!!.memory.expire(interrupted!!.name, 2f)
                    handle!!.memory.unset(handle!!.name)
                    indicator!!.interrupt()
                    Global.getSoundPlayer().playUISound("ui_wait_interrupt", 1f, 1f)
                    if (seen) {
                        playerFleet.addFloatingText("Seen by fleet", Misc.getNegativeHighlightColor(), 1f, true)
                    }
                } else if (elapsedDays >= durationDays && !Global.getSector().campaignUI.isShowingDialog) {
                    done = true
                    target.addTag(Tags.HAS_INTERACTION_DIALOG)
                    finished!!.memory[finished!!.name] = true
                    finished!!.memory.expire(finished!!.name, 0f)
                    inProgress!!.memory.unset(inProgress!!.name)
                    handle!!.memory.unset(handle!!.name)
                    indicator!!.getContainingLocation().removeEntity(indicator)
                    Global.getSector().campaignUI.showInteractionDialog(target)
                    Global.getSoundPlayer().playUISound("ui_wait_finish", 1f, 1f)
                }
            }

            override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI, battle: BattleAPI) {
                if (battle.getSnapshotSideFor(playerFleet) != null || target is CampaignFleetAPI && battle.getSnapshotSideFor(
                        target as CampaignFleetAPI
                    ) != null
                ) {
                    battleOccured = true
                }
            }

            override fun reportShownInteractionDialog(dialog: InteractionDialogAPI) {
                interactedWithSomethingElse = interactedWithSomethingElse or (dialog.interactionTarget !== target)
            }

            override fun reportFleetDespawned(fleet: CampaignFleetAPI, reason: CampaignEventListener.FleetDespawnReason, param: Any?) {
                if (fleet == playerFleet || fleet == target) {
                    battleOccured = true
                }
            }
        }
        handle!!.memory[handle!!.name] = this
        Global.getSector().addScript(waitScript)
        Global.getSector().isPaused = false
        dialog.dismiss()
        return true
    }

    fun getWaitScript(): EveryFrameScript? {
        return waitScript
    }

    fun getIndicator(): CampaignProgressIndicatorAPI? {
        return indicator
    }

    fun getHandle(): Misc.VarAndMemory? {
        return handle
    }

    fun getFinished(): Misc.VarAndMemory? {
        return finished
    }

    fun getInProgress(): Misc.VarAndMemory? {
        return inProgress
    }

    fun getInterrupted(): Misc.VarAndMemory? {
        return interrupted
    }
}