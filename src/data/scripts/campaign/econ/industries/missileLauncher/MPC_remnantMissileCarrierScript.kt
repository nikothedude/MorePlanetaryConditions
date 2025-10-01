package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.util.IntervalUtil
import data.scripts.campaign.abilities.MPC_missileStrikeAbility

class MPC_remnantMissileCarrierScript(
    val fleet: CampaignFleetAPI
): MPC_orbitalMissileLauncher() {
    override var maxMissilesLoaded: Float = 2f
    override var missilesLoaded: Float = 2f
    override var missileReloadInterval = IntervalUtil(6.2f, 6.3f) // days
    override var renderTerrain: Boolean = false

    override var minSensorProfile: Float = 800f
    override var maxSensorProfile: Float = 3000f
    override var maxDetectionRate: Float = 0.2f

    override fun getHost(): SectorEntityToken {
         return fleet
    }

    override fun getMaxTargettingRange(): Float {
        return 50000f
    }

    override fun getDetectionIncrement(fleet: CampaignFleetAPI, amount: Float): Float {
        if (canTargetFleet(fleet) && fleet.isVisibleToSensorsOf(getHost())) return maxDetectionRate * amount

        return super.getDetectionIncrement(fleet, amount)
    }

    override fun createMissile(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin {
        val missile = MPC_aegisMissileEntityPlugin.createNewFromEntity(
            fleet,
            params
        )

        return missile
    }

    override fun getMissileParams(target: SectorEntityToken): MPC_aegisMissileEntityPlugin.MissileParams {
        val params = MPC_missileStrikeAbility.getStandardAbilityParams(
            fleet,
            target,
            "MPC_remnantCarrierMissile"
        )
        MPC_missileStrikeAbility.Missile.EXPLOSIVE.adjustMissileParams(params)
        params.explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        return params
    }

    override fun getFaction(): FactionAPI {
        return fleet.faction
    }

    override fun getTerrainName(): String? {
        return "Missile Platform"
    }

    override fun getDescNoun(): String? {
        return "an unknown missile complex"
    }
}