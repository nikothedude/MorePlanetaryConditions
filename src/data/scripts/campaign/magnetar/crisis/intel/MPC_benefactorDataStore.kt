package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class MPC_benefactorDataStore {

    companion object {
        fun get(): MPC_benefactorDataStore {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_benefactorDataStore"] == null) {
                Global.getSector().memoryWithoutUpdate["\$MPC_benefactorDataStore"] = MPC_benefactorDataStore()
            }
            return Global.getSector().memoryWithoutUpdate["\$MPC_benefactorDataStore"] as MPC_benefactorDataStore
        }
    }

    data class benefactorData(
        val factionId: String,
        val name: String = Global.getSector().getFaction(factionId).displayName,
        val color: Color = Global.getSector().getFaction(factionId).baseUIColor,
        val addBullet: (info: TooltipMakerAPI) -> Unit = { info -> info.addPara(name, color, 0f) }
    )

    val probableBenefactors = mutableSetOf(
        benefactorData(Factions.HEGEMONY),
        benefactorData(Factions.LUDDIC_CHURCH),
        benefactorData(Factions.INDEPENDENT, "KKL", addBullet = { info -> info.addPara("%s (Stationed in %s)", 0f,  Global.getSector().getFaction(Factions.INDEPENDENT).baseUIColor, "KKL", "Nova Maxios") }),
        benefactorData(Factions.LUDDIC_PATH, addBullet = { info -> info.addPara("%s (Possible)", 0f,  Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor, "Luddic Path") }),
        benefactorData(Factions.TRITACHYON),
        benefactorData(Factions.DIKTAT),
        // the league is actually not involved but you'll get silly dialogue if you investigate them
        benefactorData(Factions.PERSEAN, addBullet = { info -> info.addPara("%s (Possible)", 0f, Global.getSector().getFaction(Factions.PERSEAN).baseUIColor, "Persean League") })
    )
}