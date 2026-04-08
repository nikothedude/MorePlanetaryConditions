package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.abilities.MPC_missileStrikeAbility
import data.scripts.campaign.abilities.MPC_missileStrikeAbility.Missile
import data.scripts.campaign.abilities.MPC_missileStrikeReactionScript
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods
import data.scripts.campaign.econ.industries.MPC_missileLauncherIndustry
import data.utilities.niko_MPC_miscUtils.isOrbitalStation

abstract class MPC_marketBasedMissileLauncherScript(val market: MarketAPI, val industry: MPC_missileLauncherIndustry): MPC_orbitalMissileLauncher() {

    override var maxMissilesLoaded: Float = 0f // defined in the industry
    override var missilesLoaded: Float = 0f
        get() {
            field = field.coerceAtMost(maxMissilesLoaded)
            return field
        }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!industry.market.hasIndustry(industry.spec.id)) {
            delete()
        }
    }

    override fun getHost(): SectorEntityToken {
        return market.primaryEntity
    }

    override fun getMaxTargettingRange(): Float {
        return industry.getMaxRange()
    }

    override fun getFaction(): FactionAPI {
        return market.faction
    }

    override fun getDescNoun(): String? {
        val stationOrPlanet = if (market.primaryEntity.isOrbitalStation()) "station" else "planet"
        return "a $stationOrPlanet-based ISBM complex"
    }

    override fun createMissile(spec: MPC_missileStrikeAbility.Missile, target: SectorEntityToken): MPC_missileEntityPlugin {
        val missile = MPC_missileStrikeAbility.Missile.getMissileFrom(spec, getHost(), target, market = market)

        industry.missileCreated(missile, target)

        return missile
    }
}