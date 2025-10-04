package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods
import data.scripts.campaign.econ.industries.MPC_aegisRocketPods.Companion.MARKET_WITH_AEGIS_MEMKEY
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.overgrownNanoforgeFleetFactionId
import data.utilities.niko_MPC_industryIds
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
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

        fun getMissileCarriers(): List<FleetMemberAPI> = Global.getSector().playerFleet?.fleetData?.membersListCopy?.filter { it.variant.hasHullMod("MPC_missileCarrier") } ?: listOf()

        fun createCarrierHunterFleet() {

            val r = MathUtils.getRandom()
            val e = DelayedFleetEncounter(r, "MPC_missileCarrierRetaliation")

            if (Global.getSettings().isDevMode) {
                e.setDelayNone()
            } else {
                e.setDelay(40f, 45f)
            }

            e.setDoNotAbortWhenPlayerFleetTooStrong() // small ships, few FP, but a strong fleet
            e.setLocationCoreOnly(true, Factions.HEGEMONY)
            e.beginCreate()
            e.triggerCreateFleet(
                FleetSize.MAXIMUM,
                HubMissionWithTriggers.FleetQuality.SMOD_2,
                Factions.HEGEMONY,
                FleetTypes.PATROL_LARGE,
                Vector2f()
            )
            e.triggerSetFleetCombatFleetPoints((Global.getSector().playerFleet.fleetPoints * 1.15f).coerceAtLeast(200f))
            //e.triggerSetFleetMaxShipSize(1)
            //e.triggerSetFleetDoctrineOther(4, 4)

            //e.triggerSetFleetDoctrineComp(4, 1, 0)

            e.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
            e.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1)
            e.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1)
            e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER)

            e.triggerFleetMakeFaster(true, 0, true)

            e.triggerSetFleetFaction(Factions.HEGEMONY)
            e.triggerMakeNoRepImpact()
            e.triggerSetStandardAggroInterceptFlags()
            e.triggerMakeFleetIgnoreOtherFleets()
            e.triggerSetFleetGenericHailPermanent("MPC_missileCarrierRetaliation")
            e.triggerSetFleetFlagPermanent("\$MPC_missileCarrierRetaliation")
            e.triggerFleetSetName("Kill-Fleet")
            e.endCreate()
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
                val carriers = getMissileCarriers()
                if (carriers.isEmpty()) return false

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
                val carriers = getMissileCarriers()
                val ship = carriers.firstOrNull() ?: return false

                ship.variant.addTag("MPC_missileCarrierDisarmed")
                Global.getSector().memoryWithoutUpdate["\$MPC_carrierIsAegis"] = true
                Global.getSector().memoryWithoutUpdate[MARKET_WITH_AEGIS_MEMKEY] = dialog.interactionTarget.market.id
                dialog.interactionTarget.market?.reapplyIndustries()
            }

            "convertAegisToShip" -> {
                val carriers = getMissileCarriers()
                val ship = carriers.firstOrNull() ?: return false

                ship.variant.removeTag("MPC_missileCarrierDisarmed")
                Global.getSector().memoryWithoutUpdate["\$MPC_carrierIsAegis"] = false
                Global.getSector().memoryWithoutUpdate.unset(MARKET_WITH_AEGIS_MEMKEY)
                dialog.interactionTarget.market?.reapplyIndustries()
            }

            "giveCarrierToHege" -> {
                // handled in the rules
            }

            "giveAegisToHege" -> {
                val market = getComplexMarket()
                if (market != null) {
                    val chico = Global.getSector().economy.getMarket("chicomoztoc")
                    if (chico != null) {
                        Global.getSector().memoryWithoutUpdate[MARKET_WITH_AEGIS_MEMKEY] = "chicomoztoc"
                        chico.constructionQueue.addToEnd("niko_MPC_aegisMissileSystem", 100000)
                    }

                    val storage = chico.getSubmarket(Submarkets.SUBMARKET_STORAGE).plugin as StoragePlugin
                    storage.setPlayerPaidToUnlock(true)
                    val cargo = storage.cargo
                    var i = 0f
                    while (i++ < 1) {
                        cargo.addMothballedShip(
                            FleetMemberType.SHIP,
                            "onslaught_xiv_Elite",
                            null
                        )
                    }
                    i = 0f
                    while (i++ < 1) {
                        cargo.addMothballedShip(
                            FleetMemberType.SHIP,
                            "eagle_xiv_Elite",
                            null
                        )
                    }
                    i = 0f
                    while (i++ < 2) {
                        cargo.addMothballedShip(
                            FleetMemberType.SHIP,
                            "falcon_xiv_Elite",
                            null
                        )
                    }
                    i = 0f
                    while (i++ < 2) {
                        cargo.addMothballedShip(
                            FleetMemberType.SHIP,
                            "dominator_XIV_Elite",
                            null
                        )
                    }
                    i = 0f
                    while (i++ < 5) {
                        cargo.addMothballedShip(
                            FleetMemberType.SHIP,
                            "enforcer_XIV_Elite",
                            null
                        )
                    }

                    cargo.addCommodity(Commodities.HAND_WEAPONS, 350f)
                    cargo.addCommodity(Commodities.METALS, 5000f)
                    cargo.addCommodity(Commodities.FUEL, 2000f)
                    cargo.addCommodity(Commodities.SUPPLIES, 750f)
                }

                val carriers = getMissileCarriers()
                val ship = carriers.firstOrNull() ?: return false

                ship.variant.addTag("MPC_missileCarrierDisarmed")
                Global.getSector().memoryWithoutUpdate["\$MPC_carrierIsAegis"] = true

            }

            "canDoReactionInBar" -> {
                val market = dialog.interactionTarget.market ?: return false
                val complex = getComplexMarket()
                val carriers = getMissileCarriers()
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didHegeMissileCarrierReaction") && (carriers.any { !it.variant.hasTag("MPC_missileCarrierDisarmed") } && market.factionId == Factions.HEGEMONY)) {
                    return true
                }
                val repcheck = Global.getSector().getFaction(Factions.HEGEMONY).relToPlayer.isAtWorst(RepLevel.INHOSPITABLE)
                if (!repcheck) return false
                if (!market.isPlayerOwned) return false

                return (! Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didHegeMissileCarrierReactionAegis") && (complex?.id != null && market.id == complex.id))
            }

            "retaliate" -> createCarrierHunterFleet()
        }

        return false
    }
}