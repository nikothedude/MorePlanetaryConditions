package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.abilities.MPC_missileStrikeAbility
import data.scripts.campaign.abilities.MPC_missileStrikeAbility.Missile
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods
import data.scripts.campaign.econ.industries.MPC_missileLauncherIndustry

class MPC_aegisPlatformScript(market: MarketAPI, industry: MPC_aegisRocketPods): MPC_marketBasedMissileLauncherScript(market, industry) {

    companion object {
        val eligibleSpecs = hashMapOf<MPC_missileStrikeAbility.Missile, Float>(
            Pair(MPC_missileStrikeAbility.Missile.EXPLOSIVE, 100f),
            Pair(MPC_missileStrikeAbility.Missile.INTERDICT, 8f),
            Pair(MPC_missileStrikeAbility.Missile.SENSOR, 40f),
            Pair(Missile.EXPLOSIVE_HEAVY, 3f)
        )

        fun getPicker(): WeightedRandomPicker<MPC_missileStrikeAbility.Missile> {
            val picker = WeightedRandomPicker<MPC_missileStrikeAbility.Missile>()
            eligibleSpecs.forEach { picker.add(it.key, it.value) }
            return picker
        }

        const val HIGH_RES_SENSORS_BONUS_MULT = 3f
    }

    override fun getTerrainName(): String? {
        return "${market.name} Aegis Platform"
    }

    override fun getSpec(target: SectorEntityToken): MPC_missileStrikeAbility.Missile {
        return getPicker().pick()
    }
}