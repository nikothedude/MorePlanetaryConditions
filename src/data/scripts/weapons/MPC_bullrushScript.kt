package data.scripts.weapons

import com.fs.starfarer.api.combat.ShieldAPI

class MPC_bullrushScript() : MPC_shieldMissileScript() {

    override val id: String = "MPC_bullrush"

    override fun getShieldType(): ShieldAPI.ShieldType {
        return ShieldAPI.ShieldType.FRONT
    }

    override fun getShieldArc(): Float {
        return 120f
    }

    override fun getFluxCapacity(): Float {
        return 1900f
    }

    override fun getFluxDissipation(): Float {
        return 200f
    }
}