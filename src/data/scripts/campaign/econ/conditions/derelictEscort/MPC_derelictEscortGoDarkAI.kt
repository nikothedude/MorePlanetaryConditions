package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility
import com.fs.starfarer.api.impl.campaign.abilities.ai.BaseAbilityAI
import com.fs.starfarer.api.impl.campaign.abilities.ai.EmergencyBurnAbilityAI
import com.fs.starfarer.api.impl.campaign.abilities.ai.GoDarkAbilityAI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils

class MPC_derelictEscortGoDarkAI: GoDarkAbilityAI() {

    override fun advance(days: Float) {
        val assignmentTarget = fleet.ai?.currentAssignment?.target
        if (assignmentTarget != null) {
            if (!ability.isActive && fleet.getAbility(Abilities.SUSTAINED_BURN)?.isActive != true) {
                val targetGoneDark = (assignmentTarget.getAbility(Abilities.GO_DARK)?.isActive == true)
                if (targetGoneDark) {
                    val dist = MathUtils.getDistance(fleet, assignmentTarget)
                    if (dist <= MPC_derelictEscortAssignmentAI.DERELICT_ESCORT_CATCH_UP_DIST) {
                        ability.activate()
                    }
                }
            }
            return
        }

        super.advance(days)
    }
}