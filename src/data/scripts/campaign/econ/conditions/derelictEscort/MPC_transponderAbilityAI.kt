package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.impl.campaign.abilities.ai.BaseAbilityAI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.util.IntervalUtil

class MPC_transponderAbilityAI: BaseAbilityAI() {
    private val interval = IntervalUtil(0.05f, 0.15f)

    override fun advance(days: Float) {
        interval.advance(days)
        if (!interval.intervalElapsed()) return

        val assignmentTarget = fleet.ai?.currentAssignment?.target
        if (assignmentTarget == null) {
            if (ability.isActive) ability.deactivate()
            return
        } else {
            val targetTransponderOn = assignmentTarget.isTransponderOn
            if (targetTransponderOn) {
                if (!ability.isActive) {
                    ability.activate()
                }
            } else if (ability.isActive) {
                ability.deactivate()
            }
            return
        }
    }
}