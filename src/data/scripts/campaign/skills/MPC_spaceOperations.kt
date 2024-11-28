package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_spaceOperations {

    companion object {
        const val SHIP_QUALITY_PERCENT = 0.25f
        fun getPatrol(market: MarketAPI): MilitaryBase? = (market.getIndustry(Industries.MILITARYBASE) ?: market.getIndustry(Industries.HIGHCOMMAND) ?: market.getIndustry(Industries.PATROLHQ)) as? MilitaryBase
    }

    class Market1: MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            MPC_respawnAcceleratorScript(market).start()
            market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyFlat(id, SHIP_QUALITY_PERCENT, "Space Operations")
        }

        override fun unapply(market: MarketAPI, id: String) {
            val script = market.primaryEntity?.scripts?.firstOrNull { it is MPC_respawnAcceleratorScript }
            (script as? MPC_respawnAcceleratorScript)?.stop()
            market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "${100f + SHIP_QUALITY_PERCENT}% ship quality, as well as hastened patrol respawns"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.GOVERNED_OUTPOST
        }
    }


    class MPC_respawnAcceleratorScript(
        val market: MarketAPI,
        val amountMult: Float = 1f
    ): niko_MPC_baseNikoScript() {
        override fun startImpl() {
            market.primaryEntity?.addScript(this)
        }

        override fun stopImpl() {
            market.primaryEntity?.removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            val militaryBase = getPatrol(market) ?: return
            militaryBase.advance(amount * amountMult)
        }
    }
}