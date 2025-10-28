package data.compatability

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import data.niko_MPC_modPlugin.Companion.currVersion
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_ids

object MPC_compatabilityUtils {

    fun run(version: String) {
        if (version == "4.2.0") {
            val system = Global.getSector()?.getStarSystem("Perseus NM 2231+9CB") ?: return
            system.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "MPC_magnetarAmbience"
        }
        if (version == "4.5.0") {
            val magnetar = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as? StarSystemAPI ?: return
            val star = magnetar.star ?: return

        }
    }
}