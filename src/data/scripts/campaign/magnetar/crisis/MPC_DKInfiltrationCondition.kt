package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import kotlin.math.min

class MPC_DKInfiltrationCondition: niko_MPC_baseNikoCondition() {
    override fun apply(id: String) {
        super.apply(id)

        val mining = market.industries.firstOrNull { it.spec.hasTag(Industries.MINING) } ?: return
        val supply = mining.getSupply(Commodities.VOLATILES)
        val intSupply = getUmbraSupply()

        supply.quantity.modifyMult(modId, 0f, name)

        val sindria = getSindria() ?: return
        val sindriaPop = sindria.industries.firstOrNull { it.spec.hasTag(Industries.TAG_POPULATION) } ?: return
        if (intSupply <= 0) return
        sindriaPop.supply(modId, Commodities.VOLATILES, intSupply, "Shipments from Umbra")
    }

    companion object {
        fun get(): MPC_DKInfiltrationCondition? {
            val market = Global.getSector().economy.getMarket("umbra")
            return market.getCondition("MPC_DKInfiltrationCondition")?.plugin as? MPC_DKInfiltrationCondition
        }

        fun getUmbraSupply(): Int {
            val market = Global.getSector().economy.getMarket("umbra")
            val mining = market.industries.firstOrNull { it.spec.hasTag(Industries.MINING) } ?: return 0
            val supply = mining.getSupply(Commodities.VOLATILES)

            val modId = get()?.modId ?: return 0
            supply.quantity.unmodify(modId)
            val result = supply.quantity.modifiedInt
            supply.quantity.modifyMult(modId, 0f)

            return result
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        val mining = market.industries.firstOrNull { it.spec.hasTag(Industries.MINING) } ?: return
        val supply = mining.getSupply(Commodities.VOLATILES)
        supply.quantity.unmodify(modId)

        val sindria = getSindria() ?: return
        val sindriaPop = sindria.industries.firstOrNull { it.spec.hasTag(Industries.TAG_POPULATION) } ?: return
        sindriaPop.getSupply(Commodities.VOLATILES)?.quantity?.unmodify(modId)
    }

    fun getSindria(): MarketAPI? = Global.getSector().economy.getMarket("sindria")

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        tooltip.addPara(
            "Volatile sales delegated to %s",
            10f,
            Global.getSector().getFaction(Factions.DIKTAT).color,
            "Sindria"
        )
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addPara(
            "Currently %s production",
            0f,
            Misc.getHighlightColor(),
            getUmbraSupply().toString()
        )
        tooltip.setBulletedListMode(null)
    }
}