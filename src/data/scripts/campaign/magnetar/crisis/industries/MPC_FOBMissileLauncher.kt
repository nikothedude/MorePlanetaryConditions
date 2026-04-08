package data.scripts.campaign.magnetar.crisis.industries

import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.abilities.MPC_missileStrikeAbility
import data.scripts.campaign.abilities.MPC_missileStrikeAbility.Missile
import data.scripts.campaign.econ.industries.MPC_missileLauncherIndustry
import data.scripts.campaign.econ.industries.missileLauncher.MPC_marketBasedMissileLauncherScript
import data.scripts.campaign.econ.industries.missileLauncher.MPC_missileEntityPlugin
import data.scripts.campaign.econ.industries.missileLauncher.MPC_orbitalMissileLauncher

class MPC_FOBMissileLauncher: MPC_missileLauncherIndustry() {

    companion object {
        const val
    }

    override fun applyImpl() {
        TODO("Not yet implemented")
    }

    override fun missilesEnabled(): Boolean = true

    override fun updateDefenseGrid() {
        var reloadMult = 1f
        if (!isFunctional) reloadMult = 0f
        rocketHandler?.reloadRateMult = (reloadMult * getRelevantDeficitMult())
        rocketHandler?.maxMissilesLoaded = if (!isFunctional) 0f else getMaxRockets()
        rocketHandler?.minSensorProfile = getMinSensorProfile()
    }

    override fun getRelevantDeficitMult(): Float {
        TODO("Not yet implemented")
    }

    override fun createNewDefenseGrid() {
        rocketHandler = MPC_FOBMissileLauncherScript(market, this)
    }

    override fun getMinSensorProfile(): Float = 600f

    override fun getBaseReloadRate(): Float {
        return 20f
    }

    override fun getMaxRockets(): Float {
        return 18f
    }

    override fun getMaxRange(): Float {
        return 40000f
    }

    class MPC_FOBMissileLauncherScript(market: MarketAPI, industry: MPC_FOBMissileLauncher) : MPC_marketBasedMissileLauncherScript(market, industry) {

        companion object {
            val eligibleSpecs = hashMapOf<MPC_missileStrikeAbility.Missile, Float>(
                Pair(MPC_missileStrikeAbility.Missile.EXPLOSIVE, 100f),
                Pair(MPC_missileStrikeAbility.Missile.INTERDICT, 8f),
                Pair(MPC_missileStrikeAbility.Missile.SENSOR, 40f),
                Pair(Missile.EXPLOSIVE_HEAVY, 7f)
            )

            fun getPicker(): WeightedRandomPicker<MPC_missileStrikeAbility.Missile> {
                val picker = WeightedRandomPicker<MPC_missileStrikeAbility.Missile>()
                eligibleSpecs.forEach { picker.add(it.key, it.value) }
                return picker
            }

            const val RELOAD_COUNT = 12
        }

        override fun getSpec(target: SectorEntityToken): MPC_missileStrikeAbility.Missile {
            return getPicker().pick()
        }

        override fun getTerrainName(): String? {
            return "${market.name} ISBM Platform"
        }

        override fun doReload() {
            missilesLoaded = (missilesLoaded + RELOAD_COUNT).coerceAtMost(maxMissilesLoaded)
        }
    }

}