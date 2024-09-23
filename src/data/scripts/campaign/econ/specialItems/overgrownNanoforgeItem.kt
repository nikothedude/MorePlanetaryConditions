package data.scripts.campaign.econ.specialItems

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.campaign.impl.items.GenericSpecialItemPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.util.*

class MPC_overgrownNanoforgeItem: GenericSpecialItemPlugin() {
    override fun addInstalledInSection(tooltip: TooltipMakerAPI?, pad: Float) {
        tooltip?.addPara("Everything", pad, Misc.getGrayColor(), Misc.getBasePlayerColor(), "Everything")
    }
}