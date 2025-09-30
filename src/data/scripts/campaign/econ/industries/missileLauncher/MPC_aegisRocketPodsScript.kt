package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods
import data.utilities.niko_MPC_miscUtils.isOrbitalStation
import java.awt.Color

class MPC_aegisRocketPodsScript(val market: MarketAPI, val industry: MPC_aegisRocketPods): MPC_orbitalMissileLauncher() {
    override var maxMissilesLoaded: Float = 0f // defined in the industry
    override var missilesLoaded: Float = 0f

    override fun getHost(): SectorEntityToken {
        return market.primaryEntity
    }

    override fun getMaxTargettingRange(): Float {
        return industry.getMaxRange()
    }

    override fun getFaction(): FactionAPI {
        return market.faction
    }

    override fun getTerrainName(): String? {
        return "${market.name} Aegis Platform"
    }

    override fun getDescNoun(): String? {
        val stationOrPlanet = if (market.primaryEntity.isOrbitalStation()) "station" else "planet"
        return "a $stationOrPlanet-based ISBM complex"
    }

    override fun createMissile(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin {
        val missile = MPC_aegisMissileEntityPlugin.createNewFromMarket(
            market,
            params
        )

        return missile
    }

    override fun getMissileParams(target: SectorEntityToken): MPC_aegisMissileEntityPlugin.MissileParams {
        val explosionParams = ExplosionEntityPlugin.ExplosionParams(
            Color(255, 90, 60),
            market.containingLocation,
            market.location,
            200f,
            0.5f
        )
        explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        val params = MPC_aegisMissileEntityPlugin.MissileParams(
            target,
            Misc.genUID(),
            getHost(),
            explosionParams
        )

        return params
    }
}