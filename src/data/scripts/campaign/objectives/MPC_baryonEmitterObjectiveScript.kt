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
import data.utilities.niko_MPC_ids

class MPC_baryonEmitterObjectiveScript: BaseCampaignObjectivePlugin() {
    var script: MPC_coronaResistObjectiveScript? = null

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        if (this.entity == null) return
        instantiateScript()
    }

    override fun printEffect(text: TooltipMakerAPI?, pad: Float) {
        if (text == null) return

        val highlightString = if (isPostcollapse()) "Heavily Reduces" else "Reduces"
        text.addPara(
            "$INDENT%s terrain movement and terrain-derived CR loss for all friendly fleets (including trade) in-system (inferior to the planet-based alternative)",
            pad,
            Misc.getHighlightColor(),
            highlightString
        )
    }

    fun isPostcollapse(): Boolean {
        return entity.hasTag(niko_MPC_ids.BARYON_EMITTER_POSTCOLLAPSE_TAG)
    }

    override fun printNonFunctionalAndHackDescription(text: TextPanelAPI?) {
        if (text == null) return

        if (entity.memoryWithoutUpdate.getBoolean(MemFlags.OBJECTIVE_NON_FUNCTIONAL)) {
            text.addPara("This one, however, does not appear to be emitting any baryons. This is not a surprise, given how high-maintenance it's components are.")
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