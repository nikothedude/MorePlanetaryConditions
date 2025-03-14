package data.compatability

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import data.niko_MPC_modPlugin.Companion.currVersion
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler

object MPC_compatabilityUtils {

    fun run(version: String) {
        if (version == "4.1.4") {
            val system = Global.getSector()?.getStarSystem("Perseus NM 2231+9CB") ?: return
            system.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "MPC_magnetarAmbience"
        }
    }
}