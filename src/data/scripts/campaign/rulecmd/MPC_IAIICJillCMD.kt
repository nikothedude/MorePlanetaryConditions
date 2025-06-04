package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RuleBasedDialog
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.AddTextSmall
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.MPC_incomeTallyListener
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import indevo.exploration.minefields.MineBeltTerrainPlugin
import indevo.ids.Ids

class MPC_IAIICJillCMD: BaseCommandPlugin() {

    companion object {
        const val RECENT_INCOME_SHARE = 4f
        const val MINIMUM_CREDIT_COST = 4000000f

        fun upgradeFractalCore() {
            val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()
            fractalColony?.admin?.stats?.setSkillLevel(niko_MPC_ids.BATTLEMIND_CAMPAIGN_SKILL_ID, 1f)
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.FRACTAL_CORE_UPGRADED] = true
        }

        fun reclaimFOB(market: MarketAPI, FOB: SectorEntityToken) {

            val containingLoc = FOB.containingLocation
            if (niko_MPC_settings.indEvoEnabled) {
                market.removeCondition(Ids.COND_MINERING)
                var minefield: MineBeltTerrainPlugin? = null
                for (terrain in containingLoc.terrainCopy) {
                    if (terrain.plugin is MineBeltTerrainPlugin) {
                        val localField = terrain.plugin as MineBeltTerrainPlugin
                        if (!localField.primary.id.contains("MPC_arkFOB")) continue
                        minefield = localField
                        break
                    }
                }
                if (minefield != null) {
                    containingLoc.removeEntity(minefield.entity)
                    for (mine in containingLoc.customEntities.filter { it.customEntityType == "IndEvo_mine" }) {
                        if (mine.orbitFocus == minefield.entity) {
                            containingLoc.removeEntity(mine)
                        }
                    }
                }
            }

            FOB.memoryWithoutUpdate["\$abandonedStation"] = false
            Global.getSector().economy.removeMarket(market)

            MPC_fractalCoreFactor.createMarket(FOB, true)
        }
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "daredJillToRetaliate" -> {
                MPC_IAIICFobIntel.get()?.retaliate(MPC_IAIICFobIntel.RetaliateReason.PISSED_OFF_JILL, dialog.textPanel)
            }
            "isAtPeace" -> {
                return !Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).relToPlayer.isHostile
            }

            "peacePossibilityIs" -> {
                val target = params[1].getString(memoryMap)
                return MPC_IAIICFobIntel.getPeacePossibility().name == target
            }
            "generateCreditConcessionAmount" -> {
                val tally = MPC_incomeTallyListener.MPC_incomeTally.get(true) ?: return false
                val highestRecentIncome = tally.getHighestIncome()
                val optimalIncomeShare = (highestRecentIncome * RECENT_INCOME_SHARE).coerceAtLeast(MINIMUM_CREDIT_COST)

                Global.getSector().memoryWithoutUpdate.set("\$MPC_creditConcessionAmount", optimalIncomeShare, 0f)
                Global.getSector().memoryWithoutUpdate.set("\$MPC_creditConcessionAmountDGS", Misc.getDGSCredits(optimalIncomeShare), 0f)

                return true
            }
            "disarm" -> {
                MPC_IAIICFobIntel.get()?.disarm()
            }
            "doPeace" -> {
                MPC_IAIICFobIntel.get()?.tryPeace(dialog)
            }
            "startEvent" -> {
                MPC_IAIICFobIntel(dialog)
                Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).isShowInIntelTab = true
            }
            "canColonizeArk" -> {
                return dialog.interactionTarget.market?.isInhabited() != true
            }
            "showColonizeCost" -> {
                Misc.showCost(dialog.textPanel, null, null, arrayOf(Commodities.CREW), intArrayOf(1000))
            }
            "notEnoughCrew" -> {
                return Global.getSector().playerFleet.cargo.totalCrew < 1000f
            }
            "colonizeArk" -> {
                val planet = dialog.interactionTarget
                planet.market

                val FOB = dialog.interactionTarget
                val FOBmarket = FOB.market ?: return false

                reclaimFOB(FOBmarket, FOB)
            }

            "contribExists" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                return intel.getContributionById(params[1].getString(memoryMap)) != null
            }
            "contribCustomIs" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                val contrib = intel.getContributionById(params[1].getString(memoryMap)) ?: return false
                if (params.size < 3)  {
                     return contrib.custom == null
                } else {
                    return contrib.custom == params[2].getString(memoryMap)
                }
            }
            "showFractalCorePerson" -> {
                val colony = getFractalColony() ?: return false
                dialog.interactionTarget.activePerson = colony.admin
                (dialog.plugin as RuleBasedDialog).notifyActivePersonChanged()
                dialog.visualPanel.showPersonInfo(colony.admin, false, false)
            }
            "giveEndSkillPoint" -> {
                Global.getSector().playerStats.addPoints(1)
            }
            "upgradeCore" -> {
                upgradeFractalCore()
            }
        }
        return false
    }
}