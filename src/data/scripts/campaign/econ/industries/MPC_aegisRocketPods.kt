package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.econ.impl.GroundDefenses
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisRocketPodsScript
import data.scripts.campaign.econ.industries.missileLauncher.MPC_orbitalMissileLauncher
import java.util.*

class MPC_aegisRocketPods: baseNikoIndustry() {

    var rocketHandler: MPC_orbitalMissileLauncher? = null

    companion object {
        const val GROUND_DEFENSE_MULT = 2f
        const val SIZE_TO_RANGE_MULT = 0.5f
    }

    override fun apply() {
        super.apply(true)

        val size = market.size

        demand(Commodities.SUPPLIES, size + 1)
        demand(Commodities.HAND_WEAPONS, size + 2)
        demand(Commodities.MARINES, size + 1)

        if (isFunctional) {
            val mult = getDeficitMult(Commodities.SUPPLIES, Commodities.MARINES, Commodities.HAND_WEAPONS)
            var extra = ""
            if (mult != 1f) {
                val com = getMaxDeficit(Commodities.SUPPLIES, Commodities.MARINES, Commodities.HAND_WEAPONS).one
                extra = " (" + getDeficitText(com).lowercase(Locale.getDefault()) + ")"
            }
            val bonus = GROUND_DEFENSE_MULT
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(modId, bonus * mult, nameForModifier + extra)

            if (!reapplying) {
                setupDefenseGrid()
            }
        }

        rocketHandler?.maxMissilesLoaded = getMaxRockets()
        rocketHandler?.missilesLoaded = getMaxRockets()
    }

    override fun unapply() {
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(modId)
        if (!reapplying) {
            dismantleDefenseGrid()
        }
    }

    private fun setupDefenseGrid() {
        if (rocketHandler != null) return

        rocketHandler = MPC_aegisRocketPodsScript(market, this)
        rocketHandler?.start()
    }

    private fun dismantleDefenseGrid() {
        rocketHandler?.delete()
        rocketHandler = null
    }

    override fun addRightAfterDescriptionSection(tooltip: TooltipMakerAPI?, mode: Industry.IndustryTooltipMode?) {
        super.addRightAfterDescriptionSection(tooltip, mode)
        tooltip?.addPara(
            "TODO %s", 5f, Misc.getHighlightColor(), "x"
        )
    }

    override fun addGroundDefensesImpactSection(tooltip: TooltipMakerAPI?, bonus: Float, vararg commodities: String?) {
        super.addGroundDefensesImpactSection(tooltip, bonus, *commodities)
    }

    override fun getUnavailableReason(): String? {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return "Blueprint unknown"
        }
        return null
    }

    override fun showWhenUnavailable(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        return true
    }

    override fun isAvailableToBuild(): Boolean {
        if (!Global.getSector().playerFaction.knowsIndustry(getId())) {
            return false
        }
        return super.isAvailableToBuild()
    }

    fun getMaxRockets(): Float {
        return 3f
    }

    fun getMaxRange(): Float {
        val effectiveSize = (market.size - 2) * SIZE_TO_RANGE_MULT

        return 7500f * effectiveSize
    }
}