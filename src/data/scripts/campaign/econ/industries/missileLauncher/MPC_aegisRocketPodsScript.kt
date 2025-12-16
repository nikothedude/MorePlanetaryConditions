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
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_miscUtils.isOrbitalStation
import data.utilities.niko_MPC_settings
import org.magiclib.kotlin.getStationIndustry

class MPC_aegisRocketPodsScript(val market: MarketAPI, val industry: MPC_aegisRocketPods): MPC_orbitalMissileLauncher() {

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

        const val MIN_PROFILE = 300f
        const val HIGH_RES_SENSORS_BONUS_MULT = 3f
    }

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

    override fun getTerrainName(): String? {
        return "${market.name} Aegis Platform"
    }

    override fun getDescNoun(): String? {
        val stationOrPlanet = if (market.primaryEntity.isOrbitalStation()) "station" else "planet"
        return "a $stationOrPlanet-based ISBM complex"
    }

    override fun createMissile(spec: MPC_missileStrikeAbility.Missile, target: SectorEntityToken): MPC_aegisMissileEntityPlugin {
        val missile = MPC_missileStrikeAbility.Missile.getMissileFrom(spec, getHost(), target, market = market)

        if (market.isPlayerOwned && !Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_missileStrikeReactionPrepared")) {
            MPC_missileStrikeReactionScript.get(true)?.start()
        }

        return missile
    }

    override fun getSpec(target: SectorEntityToken): MPC_missileStrikeAbility.Missile {
        return getPicker().pick()
    }
}