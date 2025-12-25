package data.scripts.weapons

import com.fs.starfarer.api.combat.ShieldAPI
import java.awt.Color

class MPC_malletMissileScript: MPC_shieldMissileScript() {
    override val id: String = "MPC_malletMissile"

    override fun getShieldType(): ShieldAPI.ShieldType {
        return ShieldAPI.ShieldType.FRONT
    }

    override fun getShieldArc(): Float {
        return 90f
    }

    override fun getFluxCapacity(): Float {
        return 200f
    }

    override fun getFluxDissipation(): Float {
        return 30f
    }

    override fun getShieldOuterColor(): Color? {
        return Color(255, 255, 255, 255)
    }

    override fun getShieldInnerColor(): Color? {
        return Color(255, 125, 125, 75)
    }

}