package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.abilities.MPC_missileStrikeAbility

class MPC_remnantMissileCarrierScript(
    val fleet: CampaignFleetAPI
): MPC_orbitalMissileLauncher() {

    companion object {
        val eligibleSpecs = hashMapOf<MPC_missileStrikeAbility.Missile, Float>(
            Pair(MPC_missileStrikeAbility.Missile.EXPLOSIVE, 20f),
            Pair(MPC_missileStrikeAbility.Missile.INTERDICT, 2f)
        )

        fun getPicker(): WeightedRandomPicker<MPC_missileStrikeAbility.Missile> {
            val picker = WeightedRandomPicker<MPC_missileStrikeAbility.Missile>()
            eligibleSpecs.forEach { picker.add(it.key, it.value) }
            return picker
        }
    }

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

    override fun createMissile(
        spec: MPC_missileStrikeAbility.Missile,
        target: SectorEntityToken
    ): MPC_aegisMissileEntityPlugin {
        val missile = MPC_missileStrikeAbility.Missile.getMissileFrom(spec, getHost(), target)

        return missile
    }

    override fun getSpec(target: SectorEntityToken): MPC_missileStrikeAbility.Missile {
        return getPicker().pick()
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