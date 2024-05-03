package data.scripts.campaign.econ.conditions.terrain.meson

import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import java.awt.Color

class niko_MPC_mesonField: BaseRingTerrain() {
    companion object {
        val SENSOR_INCREASE_MULT = 3.5f
        val SENSOR_PROFILE_MULT = 1.5f

        val MESON_COLOR = Color(43, 239, 8, 45)
    }
}