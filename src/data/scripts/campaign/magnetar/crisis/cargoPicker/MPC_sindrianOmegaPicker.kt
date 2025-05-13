package data.scripts.campaign.magnetar.crisis.cargoPicker

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest

class MPC_sindrianOmegaPicker(dialog: InteractionDialogAPI, pointsNeeded: Int = POINTS_NEEDED, val memoryMap: MutableMap<String, MemoryAPI>?) : MPC_omegaWeaponPicker(dialog, pointsNeeded) {

    companion object {
        const val POINTS_NEEDED = 30
    }

    override fun satisfyPoints(pointsForWeapon: Int) {
        Global.getSector().memoryWithoutUpdate["\$MPC_sindrianOmegaPoints"] = (getPointsSatisfied() + pointsForWeapon)
    }

    override fun getPointsSatisfied(): Int {
        sanitizeMemory()
        return Global.getSector().memoryWithoutUpdate.getInt("\$MPC_sindrianOmegaPoints")
    }

    fun sanitizeMemory() {
        if (Global.getSector().memoryWithoutUpdate["\$MPC_sindrianOmegaPoints"] == null) {
            Global.getSector().memoryWithoutUpdate["\$MPC_sindrianOmegaPoints"] = 0
        }
    }

    override fun postPick() {
        FireBest.fire(null, dialog, memoryMap, "MPC_IAIICCadenItemsTurnedIn")
    }

    override fun getFaction(): FactionAPI {
        return Global.getSector().getFaction(Factions.DIKTAT)
    }
}