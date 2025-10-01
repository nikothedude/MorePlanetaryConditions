package data.scripts.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.Industry.AICoreDescriptionMode
import com.fs.starfarer.api.campaign.econ.Industry.ImprovementDescriptionMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.MPC_constructionAcceleratorIndustry.Companion.IMPROVED_BONUS
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisRocketPodsScript
import data.scripts.campaign.econ.industries.missileLauncher.MPC_orbitalMissileLauncher
import data.scripts.campaign.rulecmd.MPC_remnantMissileCarrierCMD.Companion.getComplexMarket
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_stringUtils
import java.util.*

class MPC_aegisRocketPods: baseNikoIndustry() {

    var rocketHandler: MPC_orbitalMissileLauncher? = null

    companion object {
        const val GROUND_DEFENSE_MULT = 1f
        const val SIZE_TO_RANGE_MULT = 0.5f
        const val RANGE_PER_MARKET_SIZE = 3500f
        const val EXTRA_MISSILES_PER_MARKET_SIZE = 1

        const val IMPROVED_RELOAD_BONUS_INCR = 0.5f

        const val MARKET_WITH_AEGIS_MEMKEY = "\$MPC_marketWithAegis"

        const val ALPHA_CORE_MAX_MISSILES_INC = 1

        const val RELOAD_INC_PER_MARKET_SIZE = 0.1f
    }

    override fun buildingFinished() {
        super.buildingFinished()

        setupDefenseGrid()
    }

    override fun apply() {
        super.apply(true)

        val size = market.size

        val mult = getDeficitMult(Commodities.SUPPLIES, Commodities.MARINES, Commodities.HAND_WEAPONS)
        if (complexInstalled()) {
            demand(Commodities.SUPPLIES, size + 1)
            demand(Commodities.HAND_WEAPONS, size + 2)
            demand(Commodities.MARINES, size + 1)

            if (isFunctional) {
                var extra = ""
                if (mult != 1f) {
                    val com = getMaxDeficit(Commodities.SUPPLIES, Commodities.MARINES, Commodities.HAND_WEAPONS).one
                    extra = " (" + getDeficitText(com).lowercase(Locale.getDefault()) + ")"
                }
                val bonus = GROUND_DEFENSE_MULT
                market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(modId, 1f + bonus * mult, nameForModifier + extra)

                if (!reapplying) {
                    setupDefenseGrid()
                }
            }

            var reloadMult = 1f
            if (isImproved) reloadMult += IMPROVED_RELOAD_BONUS_INCR
            reloadMult += getSizeBasedReloadInc()
            if (!isFunctional) reloadMult = 0f
            rocketHandler?.reloadRateMult = (reloadMult * mult)
            rocketHandler?.maxMissilesLoaded = if (!isFunctional) 0f else getMaxRockets()
            //rocketHandler?.missilesLoaded = getMaxRockets()
        } else {
            dismantleDefenseGrid()
        }
    }

    override fun unapply() {
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(modId)
        /*if (!reapplying) {
            dismantleDefenseGrid()
        }*/
    }

    private fun setupDefenseGrid() {
        if (rocketHandler != null) return

        rocketHandler = MPC_aegisRocketPodsScript(market, this)
        rocketHandler?.minSensorProfile = getMinSensorProfile()
        rocketHandler?.missileReloadInterval = IntervalUtil(getReloadRate(), getReloadRate())
        rocketHandler?.start()
    }

    private fun dismantleDefenseGrid() {
        rocketHandler?.delete()
        rocketHandler = null
    }

    fun getMinSensorProfile(): Float = 300f
    fun getReloadRate(): Float {
        return 5f
    }
    fun getSizeBasedReloadInc(): Float {
        val effectiveSize = market.size - 3
        val bonus = RELOAD_INC_PER_MARKET_SIZE * effectiveSize

        return 0f + bonus
    }

    override fun addRightAfterDescriptionSection(tooltip: TooltipMakerAPI?, mode: Industry.IndustryTooltipMode?) {
        super.addRightAfterDescriptionSection(tooltip, mode)

        if (tooltip == null) return

        val buildMenu = mode == Industry.IndustryTooltipMode.ADD_INDUSTRY
        if (complexInstalled() || buildMenu) {

            tooltip.addSectionHeading("Effects", Alignment.MID, 5f)
            tooltip.addPara(
                "Increases ground defense by %s",
                5f,
                Misc.getHighlightColor(),
                "${(1 + GROUND_DEFENSE_MULT).trimHangingZero()}x"
            )

            tooltip.addPara(
                "Targets in-system hostile fleets with %s capable of %s. Will only detect fleets with a sensor profile of at least %s.",
                5f,
                Misc.getHighlightColor(),
                "cruise missiles", "severe damage", "${getMinSensorProfile().toInt()}"
            )

            tooltip.addPara(
                "Has a range of %s, increasing by %s for each market size over %s.",
                5f,
                Misc.getHighlightColor(),
                "${getMaxRange().roundNumTo(1).trimHangingZero()}su*", "${RANGE_PER_MARKET_SIZE.trimHangingZero()}", "3"
            )

            val numMissiles = rocketHandler?.missilesLoaded ?: 0f
            val numColor = if (numMissiles > 0) Misc.getHighlightColor() else Misc.getNegativeHighlightColor()
            tooltip.addPara(
                "Can load up to %s missiles at a time. Currently loaded: %s",
                5f,
                Misc.getHighlightColor(),
                "${getMaxRockets().toInt()}", "${numMissiles.toInt()}"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                numColor
            )
            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            tooltip.addPara(
                "Increases by %s per market size above %s",
                0f,
                Misc.getHighlightColor(),
                "$EXTRA_MISSILES_PER_MARKET_SIZE", "3"
            )
            tooltip.setBulletedListMode(null)

            val baseReloadRate = getReloadRate()
            tooltip.addPara(
                "Reloads a missile every %s days.",
                5f,
                Misc.getHighlightColor(),
                getReloadRate().toInt().toString()
            )
            val marketReload = getSizeBasedReloadInc()
            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            tooltip.addPara(
                "Each market size above %s decreases reload time by %s, currently at %s",
                0f,
                Misc.getHighlightColor(),
                "3", niko_MPC_stringUtils.toPercent(RELOAD_INC_PER_MARKET_SIZE), niko_MPC_stringUtils.toPercent(marketReload)
            )
            tooltip.setBulletedListMode(null)

            val mult = getDeficitMult(Commodities.SUPPLIES, Commodities.MARINES, Commodities.HAND_WEAPONS)
            if (mult < 1f) {
                tooltip.addPara(
                    "Ground defense bonus and reload rate reduced by %s due to deficits.",
                    5f,
                    Misc.getNegativeHighlightColor(),
                    niko_MPC_stringUtils.toPercent(1 - mult)
                )
            }

            tooltip.addSpacer(5f)
            val gray = Misc.getGrayColor()
            tooltip.addPara("*2000 units = 1 map grid cell", gray, 5f)
        } else {
            val extraBit: String
            val existingMarket = getComplexMarket()
            if (existingMarket != null) {
                extraBit = " A complex is currently installed on ${existingMarket.name}."
            } else {
                extraBit = " With a missile carrier in your fleet, interact with this colony to receive the option to install the complex."
            }
            tooltip.addPara(
                "No missile complex is installed on ${market.name}. This industry is useless until that changes.$extraBit",
                10f
            ).color = Misc.getNegativeHighlightColor()
        }

        if (buildMenu) {
            if (complexInstalled()) {
                tooltip.addPara(
                    "A missile complex is installed on ${market.name}, allowing this industry to function.",
                    10f
                ).color = Misc.getHighlightColor()
            } else {
                val extraBit: String
                val existingMarket = getComplexMarket()
                if (existingMarket != null) {
                    extraBit = " A complex is currently installed on ${existingMarket.name}."
                } else {
                    extraBit = " Once the structure is built, with a missile carrier in your fleet, interact with this colony to receive the option to install the complex."
                }
                tooltip.addPara(
                    "No missile complex is installed on ${market.name}. This industry is useless until that changes.$extraBit",
                    10f
                ).color = Misc.getNegativeHighlightColor()
            }
        }
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
        if (!complexInstalled()) return 0f
        val effectiveSize = (market.size - 3)
        var bonus = effectiveSize * EXTRA_MISSILES_PER_MARKET_SIZE
        if (aiCoreId == Commodities.ALPHA_CORE) bonus += ALPHA_CORE_MAX_MISSILES_INC

        return 3f + bonus
    }

    fun getMaxRange(): Float {
        val effectiveSize = (market.size - 3)

        val bonus = effectiveSize * RANGE_PER_MARKET_SIZE

        return 5000f + bonus
    }

    override fun addAlphaCoreDescription(tooltip: TooltipMakerAPI?, mode: Industry.AICoreDescriptionMode?) {
        val opad = 10f
        val highlight = Misc.getHighlightColor()

        var pre = "Alpha-level AI core currently assigned. "
        if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
            pre = "Alpha-level AI core. "
        }
        if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP || mode == AICoreDescriptionMode.MANAGE_CORE_TOOLTIP) {
            val coreSpec = Global.getSettings().getCommoditySpec(aiCoreId)
            val text = tooltip!!.beginImageWithText(coreSpec.getIconName(), 48f)
            text.addPara(
                pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                        "Increases max missiles by %s.", 0f, highlight,
                "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
                "" + ALPHA_CORE_MAX_MISSILES_INC
            )
            tooltip.addImageWithText(opad)
            return
        }

        tooltip!!.addPara(
            pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
                    "Increases max missiles by %s.", opad, highlight,
            "" + ((1f - UPKEEP_MULT) * 100f).toInt() + "%", "" + DEMAND_REDUCTION,
            "" + ALPHA_CORE_MAX_MISSILES_INC
        )
    }

    override fun addImproveDesc(info: TooltipMakerAPI, mode: Industry.ImprovementDescriptionMode) {
        val opad = 10f
        val highlight = Misc.getHighlightColor()
        val bonus = IMPROVED_BONUS

        val str = niko_MPC_stringUtils.toPercent(IMPROVED_RELOAD_BONUS_INCR)
        if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
            info.addPara("Reload rate increased by %s.", 0f, highlight, str)
        } else {
            info.addPara("Increases reload rate by %s.", 0f, highlight, str)
        }

        info.addSpacer(opad)
        super.addImproveDesc(info, mode)
    }

    override fun canImprove(): Boolean {
        return true
    }

    fun complexInstalled(): Boolean {
        val existing = getComplexMarket() ?: return false
        return existing.id == market.id
    }
}