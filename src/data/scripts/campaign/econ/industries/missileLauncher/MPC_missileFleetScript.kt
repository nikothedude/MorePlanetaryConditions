package data.scripts.campaign.econ.industries.missileLauncher

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_missileFleetScript(
    val missiles: CampaignFleetAPI,
    val target: SectorEntityToken
): niko_MPC_baseNikoScript() {

    companion object {
        fun setupMissileFleet(missiles: CampaignFleetAPI) {
            missiles.isNoFactionInName = true
            missiles.name = "Cruise Missile"

            missiles.removeAbility(Abilities.SENSOR_BURST)
            missiles.removeAbility(Abilities.GO_DARK)
            missiles.removeAbility(Abilities.EMERGENCY_BURN)

            missiles.isDoNotAdvanceAI = true
        }

        fun setupMissileTarget(missiles: CampaignFleetAPI, target: SectorEntityToken) {
            missiles.clearAssignments()
            missiles.addAssignment(
                FleetAssignment.DELIVER_CREW,
                target,
                Float.MAX_VALUE,
                "seeking",
                null
            )
        }
    }

    override fun startImpl() {
        missiles.addScript(this)
    }

    override fun stopImpl() {
        missiles.removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        missiles.stats
    }
}