package data.scripts.shipsystems

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript

class niko_MPC_overclockedTargettingSystemStats: niko_MPC_coreOverclockedTargettingSystemStats() {

    override val overloadTime = 10f
    
}