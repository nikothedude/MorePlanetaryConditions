package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.Nex_MarketCMD
import data.utilities.niko_MPC_settings
import exerelin.campaign.InvasionRound
import exerelin.utilities.InvasionListener

class MPC_IAIICFobInvasionListener: InvasionListener {

    companion object {
        const val KEY = "\$MPC_IAIICFobInvasionListener"

        fun get(withUpdate: Boolean = false): MPC_IAIICFobInvasionListener? {
            if (!niko_MPC_settings.nexLoaded) return null
            var result = Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICFobInvasionListener
            if (withUpdate) {
                if (result == null) {
                    MPC_IAIICFobInvasionListener()
                    result = Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICFobInvasionListener
                }
            }
            return result
        }
    }

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this

        Global.getSector().listenerManager.addListener(this, false)
    }

    override fun reportInvadeLoot(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: Nex_MarketCMD.TempDataInvasion?,
        cargo: CargoAPI?
    ) {
        return
    }

    override fun reportInvasionRound(
        result: InvasionRound.InvasionRoundResult?,
        fleet: CampaignFleetAPI?,
        defender: MarketAPI?,
        atkStr: Float,
        defStr: Float
    ) {
        if (defender != MPC_IAIICFobIntel.getFOB()) return
        if (fleet?.faction != Global.getSector().playerFaction) return

        MPC_IAIICFobIntel.get()?.retaliate(MPC_IAIICFobIntel.RetaliateReason.ATTACKED_FOB)
    }

    override fun reportInvasionFinished(
        fleet: CampaignFleetAPI?,
        attackerFaction: FactionAPI?,
        market: MarketAPI?,
        numRounds: Float,
        success: Boolean
    ) {
        return
    }

    override fun reportMarketTransfered(
        market: MarketAPI?,
        newOwner: FactionAPI?,
        oldOwner: FactionAPI?,
        playerInvolved: Boolean,
        isCapture: Boolean,
        factionsToNotify: MutableList<String>?,
        repChangeStrength: Float
    ) {
        return
    }
}