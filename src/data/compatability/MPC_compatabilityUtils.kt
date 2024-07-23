package data.compatability

import com.fs.starfarer.api.Global
import data.niko_MPC_modPlugin.Companion.currVersion
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler

object MPC_compatabilityUtils {

    fun run(version: String) {
        if (version == "3.5.0") {
            // 3.4.0 introduced a bug where go dark and transponder was duplicated
            val playerFleet = Global.getSector().playerFleet
            if (playerFleet != null) {
                playerFleet.removeAbility("MPC_escort_transponder")
                playerFleet.removeAbility("MPC_escort_go_dark")
            }
        }
    }
}