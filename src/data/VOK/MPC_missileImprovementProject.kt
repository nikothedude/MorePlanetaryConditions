package data.VOK

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.kaysaar.aotd.vok.scripts.specialprojects.models.AoTDSpecialProject
import data.scripts.campaign.abilities.MPC_missileStrikeAbility

class MPC_missileImprovementProject: AoTDSpecialProject() {
    override fun checkIfProjectShouldUnlock(): Boolean {
        val playerFleet = Global.getSector().playerFleet ?: return false
        return MPC_missileStrikeAbility.getMissileCarriers(playerFleet).isNotEmpty()
    }

    override fun createRewardSection(tooltip: TooltipMakerAPI?, width: Float) {
        super.createRewardSection(tooltip, width)

        if (tooltip == null) return
        tooltip.addPara(
            "%s for Lockbow-class Missile Carrier",
            5f,
            Misc.getPositiveHighlightColor(),
            "+1 missile capacity"
        )
    }

    override fun grantReward(): Any? {
        Global.getSector().memoryWithoutUpdate["\$MPC_AOTDLockbowImproved"] = true

        return null
    }
}