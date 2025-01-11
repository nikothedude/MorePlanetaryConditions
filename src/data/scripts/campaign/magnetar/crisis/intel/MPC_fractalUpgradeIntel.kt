package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.AddText
import com.fs.starfarer.api.impl.campaign.rulecmd.AddTextSmall
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.utilities.niko_MPC_ids

class MPC_fractalUpgradeIntel: BaseIntelPlugin() {

    companion object {
        fun upgradeFractalCore(dialog: InteractionDialogAPI? = null) {
            val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()
            fractalColony?.admin?.stats?.setSkillLevel(niko_MPC_ids.ROUTING_OPTIMIZATION_SKILL_ID, 1f)
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.FRACTAL_CORE_UPGRADED] = true

            if (dialog != null) {
                AddTextSmall().execute(null, dialog, Misc.tokenize("Fractal core upgraded, highlight"), null)
            }
        }
    }
}