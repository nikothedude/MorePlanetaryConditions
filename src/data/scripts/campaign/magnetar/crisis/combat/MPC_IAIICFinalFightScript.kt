package data.scripts.campaign.magnetar.crisis.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import data.utilities.niko_MPC_ids
import org.lwjgl.util.vector.Vector2f

class MPC_IAIICFinalFightScript: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        // TODO station augments needs a refactor to handle this? test
        val engine = Global.getCombatEngine()
        val playerFleet = Global.getSector().playerFleet ?: return
        val battle = playerFleet.battle ?: return
        if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICFinalBattle"] != battle) return
        // we are in the final battle. just have to find the stations
        // note one may be dead while the other may be ifghting even if we join

        val ark = engine.ships.find { it.isStation && it.owner == 1 }
        val ours = engine.ships.find { it.isStation && it.owner == 0 }

        if (ark != null && ours != null) {
            // do placement
            val dist = 1500f

            ark.fixedLocation.set(0f, dist)
            ark.location.set(0f, dist)

            ours.fixedLocation.set(0f, -dist)
            ours.location.set(0f, -dist)
        }

        engine.removePlugin(this)
    }

}