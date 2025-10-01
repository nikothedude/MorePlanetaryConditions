package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods.Companion.MARKET_WITH_AEGIS_MEMKEY
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_industryIds
import lunalib.lunaExtensions.getMarketsCopy
import org.magiclib.kotlin.getStorageCargo

class MPC_remnantMissileCarrierCMD: BaseCommandPlugin() {

    companion object {
        val itemsToWeight = listOf(
            Pair(Items.CATALYTIC_CORE, 10f),
            Pair(Items.BIOFACTORY_EMBRYO, 10f),
            Pair(Items.MANTLE_BORE, 10f),
            Pair(Items.SOIL_NANITES, 10f)
        )

        fun getComplexMarket(): MarketAPI? {
             return Global.getSector().economy.getMarket(Global.getSector().memoryWithoutUpdate.getString(MARKET_WITH_AEGIS_MEMKEY))
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
            "canAddAegisConvertOpt" -> {
                if (!Global.getSector().playerFaction.knowsIndustry(niko_MPC_industryIds.AEGIS_ROCKET_INDID)) return false
                val playerFleet = Global.getSector().playerFleet
                val ship = playerFleet.fleetData.membersListCopy.find { it.hullId.contains("MPC_lockbow") } ?: return false
                val market = dialog.interactionTarget.market ?: return false
                if (!market.isPlayerOwned) return false
                val installed = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_carrierIsAegis")
                val existing = getComplexMarket()

                if (installed) {
                    if (existing == null) return true // fallback
                    if (existing.id != market.id) return false
                } else {
                    if (!market.hasIndustry(niko_MPC_industryIds.AEGIS_ROCKET_INDID)) return false
                }

                return true
            }
            "addSpecialItem" -> {
                val targetMarket = Global.getSector().economy.getMarket("chicomoztoc") ?: Global.getSector().getFaction(Factions.HEGEMONY).getMarketsCopy().randomOrNull() ?: Global.getSector().economy.marketsCopy.random()
                val storage = targetMarket.getStorageCargo() ?: return false
                val picker = WeightedRandomPicker<String>()
                itemsToWeight.forEach { picker.add(it.first, it.second) }
                val picked = picker.pick()
                storage.addSpecial(
                    SpecialItemData(
                        picked,
                        null
                    ),
                    1f
                )
                val submarket = targetMarket.getSubmarket(Submarkets.SUBMARKET_STORAGE)
                val plugin = submarket.plugin as StoragePlugin
                plugin.setPlayerPaidToUnlock(true)
            }
            "addAegisBP" -> {
                val playerFleet = Global.getSector().playerFleet
                val cargo = playerFleet.cargo
                cargo.addSpecial(
                    SpecialItemData(Items.INDUSTRY_BP, niko_MPC_industryIds.AEGIS_ROCKET_INDID), 1f
                )
            }
            "convertToCrew" -> {
                val playerFleet = Global.getSector().playerFleet
                val ship = playerFleet.fleetData.membersListCopy.find { it.hullId.contains("MPC_lockbow") } ?: return false
                ship.variant.removePermaMod(HullMods.AUTOMATED)
            }
            "convertToAutomated" -> {
                val playerFleet = Global.getSector().playerFleet
                val ship = playerFleet.fleetData.membersListCopy.find { it.hullId.contains("MPC_lockbow") } ?: return false
                ship.variant.addTag(Tags.AUTOMATED_RECOVERABLE)
            }

            "convertShipToAegis" -> {
                val playerFleet = Global.getSector().playerFleet
                val ship = playerFleet.fleetData.membersListCopy.find { it.hullId.contains("MPC_lockbow") } ?: return false

                ship.variant.addTag("MPC_missileCarrierDisarmed")
                Global.getSector().memoryWithoutUpdate["\$MPC_carrierIsAegis"] = true
                Global.getSector().memoryWithoutUpdate[MARKET_WITH_AEGIS_MEMKEY] = dialog.interactionTarget.market.id
                dialog.interactionTarget.market?.reapplyIndustries()
            }

            "convertAegisToShip" -> {
                val playerFleet = Global.getSector().playerFleet
                val ship = playerFleet.fleetData.membersListCopy.find { it.hullId.contains("MPC_lockbow") } ?: return false

                ship.variant.removeTag("MPC_missileCarrierDisarmed")
                Global.getSector().memoryWithoutUpdate["\$MPC_carrierIsAegis"] = false
                Global.getSector().memoryWithoutUpdate.unset(MARKET_WITH_AEGIS_MEMKEY)
                dialog.interactionTarget.market?.reapplyIndustries()
            }
        }

        return false
    }
}