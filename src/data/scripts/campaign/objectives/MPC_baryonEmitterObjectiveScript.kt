package data.scripts.campaign.objectives

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.BaseCampaignObjectivePlugin
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin.INDENT
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_coronaResistObjectiveScript

class MPC_baryonEmitterObjectiveScript: BaseCampaignObjectivePlugin() {
    var script: MPC_coronaResistObjectiveScript? = null

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        if (this.entity == null) return
        instantiateScript()
    }

    override fun printEffect(text: TooltipMakerAPI?, pad: Float) {
        if (text == null) return

        text.addPara(
            "$INDENT%s terrain movement and CR loss for all friendly fleets (including trade) in-system (is unable to resist pulsars, is inferior to the planet-based alternative)",
            pad,
            Misc.getHighlightColor(),
            "Heavily reduces"
        )
    }

    override fun printNonFunctionalAndHackDescription(text: TextPanelAPI?) {
        if (text == null) return

        if (entity.memoryWithoutUpdate.getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) {
            text.addPara("This one, however, does not appear to be emitting any baryons. The cause of its lack of function is unknown.")
        }
        if (isHacked) {
            text.addPara("You have a hack running on this baryon emitter.")
        }
    }

    private fun instantiateScript() {
        script = MPC_coronaResistObjectiveScript(entity, this)
        script!!.start()
    }

    fun getEntity(): SectorEntityToken {
        return entity
    }

    override fun advance(amount: Float) {
        if (script?.deleted == true) {
            instantiateScript()
        }
        super.advance(amount)
    }
}