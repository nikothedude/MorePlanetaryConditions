package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetHoracioCaden
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecution
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.MPC_DKInfiltrationCondition
import data.scripts.campaign.magnetar.crisis.MPC_IAIICDKFuelHubFleetSpawner
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_sindrianOmegaPicker
import data.scripts.campaign.magnetar.crisis.intel.MPC_DKContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_TTContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.support.MPC_lionsGuardFractalSupport
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.hasUnexploredRuins
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant

class MPC_IAIICDKCMD: BaseCommandPlugin() {

    companion object {
        fun generateSyncroPlanet(): SectorEntityToken? {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] != null) return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as SectorEntityToken
            var backupTarget: PlanetAPI? = null
            var target: PlanetAPI? = null

            for (system in Global.getSector().starSystems.shuffled()) {
                if (system.hasTag(Tags.THEME_SPECIAL) || system.hasTag(Tags.THEME_UNSAFE) || system.hasTag(Tags.SYSTEM_ABYSSAL) || system.hasTag(
                        Tags.THEME_HIDDEN) || system.hasTag(Tags.THEME_CORE)) continue
                //if (!system.isProcgen) continue
                //if (abs(system.location.x) > MPC_IAIICPatherCMD.HYPERSPACE_MAX_DIST || abs(system.location.y) > MPC_IAIICPatherCMD.HYPERSPACE_MAX_DIST_Y) continue
                //if (system.hasBlackHole() || system.hasPulsar()) continue

                for (planet in system.planets) {
                    val market = planet.market ?: continue
                    if (market.isInhabited()) continue
                    backupTarget = planet
                    //if (market.hasCondition("pre_collapse_facility")) continue
                    if (market.surveyLevel == MarketAPI.SurveyLevel.FULL) continue
                    if (!market.hasUnexploredRuins()) continue
                    if (!market.hasCondition(Conditions.RUINS_EXTENSIVE) && !market.hasCondition(Conditions.RUINS_VAST)) continue
                    //if (market.hasCondition(Conditions.HABITABLE)) continue
                    //if (market.hazardValue > MPC_IAIICPatherCMD.MIN_HAZARD) continue
                    if (!market.hasCondition(Conditions.NO_ATMOSPHERE)) continue
                    target = planet
                    break
                }
            }
            if (target == null) target = backupTarget
            if (target == null) return null
            if (target.market?.hasUnexploredRuins() != true) {
                for (system in Global.getSector().starSystems) {
                    for (planet in system.planets) {
                        if (planet.market?.hasUnexploredRuins() == true) {
                            target = planet
                            break
                        }
                    }
                }
            }

            target?.makeImportant("\$MPC_IAIICDKSyncroPlanet")
            target?.starSystem?.let { MPC_IAIICDKFuelHubFleetSpawner(it).start() }

            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] = target
            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanetName"] = target?.name
            return target
        }

        fun checkDemand(): Boolean {
            Global.getSector().economy.getMarket("umbra")?.reapplyIndustries()
            Global.getSector().economy.getMarket("umbra")?.reapplyConditions()
            val output = MPC_DKInfiltrationCondition.getUmbraSupply()
            val demand = getVolatileDemand()

            return (output >= demand)
        }

        fun getVolatileDemand(): Float {
            return Global.getSector().economy.getMarket("sindria")?.industries?.firstOrNull { it.spec.hasTag(Industries.FUELPROD) }?.getDemand(Commodities.VOLATILES)?.quantity?.modifiedValue ?: 0f
        }

        fun checkDemandAndUpdate(dialog: InteractionDialogAPI? = null) {
            if (checkDemand()) {
                returnToMacarioCauseDone(dialog)
            }
        }

        fun returnToMacarioCauseDone(dialog: InteractionDialogAPI? = null) {
            val ourIntel = MPC_DKContributionIntel.get() ?: return
            ourIntel.state = MPC_DKContributionIntel.State.RETURN_TO_MACARIO_CAUSE_DONE
            if (dialog != null) {
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.RETURN_TO_MACARIO_CAUSE_DONE, dialog.textPanel)
            } else {
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.RETURN_TO_MACARIO_CAUSE_DONE, false, false)
            }
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
            "canAskUnimportantAboutIAIIC" -> {
                val fobIntel = MPC_IAIICFobIntel.get() ?: return false
                val activePerson = dialog.interactionTarget.activePerson ?: return false
                if (activePerson.faction.id != Factions.DIKTAT) return false
                if (activePerson.postId != Ranks.POST_BASE_COMMANDER && activePerson.postId != Ranks.POST_ADMINISTRATOR && activePerson.postId != Ranks.POST_STATION_COMMANDER) return false
                return true
            }
            "canAskMacarioAboutIAIIC" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (MPC_DKContributionIntel.get() != null) return false
                val activePerson = dialog.interactionTarget.activePerson ?: return false
                if (activePerson.id != "macario") return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$sdtu_missionCompleted")) return false
                if (!intel.factionContributions.any { it.factionId == Factions.DIKTAT }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICDKInvestigationStarted")) return false
                return true
            }
            "canAskCadenAboutSupport" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                //if (MPC_DKContributionIntel.get() != null) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$sdtu_missionCompleted")) return false
                if (intel.factionContributions.any { it.factionId == Factions.DIKTAT }) return false
                if (Global.getSector().intelManager.hasIntelOfClass(MPC_lionsGuardFractalSupport::class.java)) return false
                //if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICDKInvestigationStarted")) return false
                return true
            }
            "openLGOmegaWpnMenu" -> {
                val picker = MPC_sindrianOmegaPicker(dialog, memoryMap = memoryMap)
                val sourceCargo = picker.getAvailableCargo(Global.getSector().playerFleet.cargo)
                dialog.showCargoPickerDialog(
                    picker.title,
                    picker.confirmText,
                    picker.cancelText,
                    true,
                    310f,
                    sourceCargo,
                    picker
                )
            }
            "addLGSupport" -> {
                Global.getSector().intelManager.addIntel(MPC_lionsGuardFractalSupport(), false, dialog.textPanel)
            }
            "deliveringCoreToMacario" -> {
                val intel = MPC_DKContributionIntel.get() ?: return false
                return (intel.state == MPC_DKContributionIntel.State.RETURN_WITH_CORE || intel.state == MPC_DKContributionIntel.State.FIND_CORE)
            }
            "retaliate" -> {
                MPC_IAIICFobIntel.get()?.retaliate(MPC_IAIICFobIntel.RetaliateReason.KEPT_SYNCROTRON, dialog.textPanel)
            }
            "keepingCore" -> {
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKInvestigationFailed"] = true
                val intel = MPC_DKContributionIntel.get() ?: return false
                intel.state = MPC_DKContributionIntel.State.FAILED
                intel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.FAILED, dialog.textPanel)
                intel.endAfterDelay()
            }
            "installAICoreIntoFP" -> {
                val sindria = Global.getSector().economy.getMarket("sindria") ?: return false
                sindria.getIndustry(Industries.FUELPROD)?.aiCoreId = Commodities.GAMMA_CORE
            }
            "installCoreIntoFP" -> {
                val sindria = Global.getSector().economy.getMarket("sindria") ?: return false
                sindria.getIndustry(Industries.FUELPROD)?.specialItem = SpecialItemData(niko_MPC_ids.specialSyncrotronItemId, null)
            }
            "generateCore" -> {
                return generateSyncroPlanet() != null
            }
            "isFuelHub" -> {
                val target = dialog.interactionTarget ?: return false
                if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] == null) return false
                return (target == Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"])

                return true
            }
            "beginSearch" -> {
                val intel = MPC_DKContributionIntel.get(true) ?: return false
                intel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.FIND_CORE, dialog.textPanel)
                Global.getSector().importantPeople.getPerson(People.MACARIO).makeImportant("\$MPC_macarioDuringDKContribution")
                return true
            }
            "coreGot" -> {
                val intel = MPC_DKContributionIntel.get(true) ?: return false
                intel.state = MPC_DKContributionIntel.State.RETURN_WITH_CORE
                intel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.RETURN_WITH_CORE, dialog.textPanel)
                val planet = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? SectorEntityToken ?: return false
                planet.makeUnimportant("\$MPC_IAIICDKSyncroPlanet")
                return true
            }
            "diktatExpeditionActiveAndNear" -> {
                val planet = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? SectorEntityToken ?: return false
                if (dialog.interactionTarget != planet) return false
                val fleet = planet.containingLocation.fleets.firstOrNull { it.memoryWithoutUpdate.getBoolean("\$MPC_IAIICDKFuelHubExpeditionFleet") } ?: return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICDKTrickedExpeditionCaptain")) return false
                val dist = MathUtils.getDistance(fleet, planet)
                if (dist > 3000f) return false

                return true
            }
            "returnExpeditionToSindria" -> {
                val planet = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDKSyncroPlanet"] as? SectorEntityToken ?: return false
                //if (dialog.interactionTarget != planet) return false
                val fleet = planet.containingLocation.fleets.firstOrNull { it.memoryWithoutUpdate.getBoolean("\$MPC_IAIICDKFuelHubExpeditionFleet") } ?: return false
                fleet.clearAssignments()
                val sindria = Global.getSector().economy.getMarket("sindria")?.primaryEntity ?: Global.getSector().economy.marketsCopy.randomOrNull()?.primaryEntity ?: return false
                fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, sindria, Float.MAX_VALUE, null)
            }
            "doesntHaveCoreInCargo" -> {
                return (!Global.getSector().playerFleet.cargo.stacksCopy.any { it.specialDataIfSpecial?.id == niko_MPC_ids.specialSyncrotronItemId })
            }

            "startWait" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.WAIT_FOR_MACARIO
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.WAIT_FOR_MACARIO, dialog.textPanel)

                val delay = if (Global.getSettings().isDevMode) 1f else 7f
                class macarioReturnScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
                    override fun executeImpl() {
                        val intel = MPC_DKContributionIntel.get()
                        if (intel != null) {
                            intel.state = MPC_DKContributionIntel.State.RETURN_TO_MACARIO
                            intel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.RETURN_TO_MACARIO, false, false)
                        }
                    }
                }
                macarioReturnScript(IntervalUtil(delay, delay)).start()
            }

            "isWaiting" -> {
                return MPC_DKContributionIntel.get()?.state == MPC_DKContributionIntel.State.WAIT_FOR_MACARIO
            }

            "doneWaiting" -> {
                return MPC_DKContributionIntel.get()?.state == MPC_DKContributionIntel.State.RETURN_TO_MACARIO
            }

            "startGoToAgent" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.GO_TO_AGENT
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.GO_TO_AGENT, dialog.textPanel)

                Global.getSector().economy.getMarket("umbra").commDirectory.addPerson(
                    Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR)
                )
                Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR).makeImportant("\$MPC_umbraInfiltrator")

                return true
            }
            /*"isSearchingForAgent" -> {
                return MPC_DKContributionIntel.get()?.state == MPC_DKContributionIntel.State.SEARCH_FOR_AGENT
            }*/
            "startGoToSindriaForCache" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.GET_CACHE
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.GET_CACHE, dialog.textPanel)

                return true
            }
            "lookingForCache" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state == MPC_DKContributionIntel.State.GET_CACHE
            }
            "deposingQM" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state.ordinal > MPC_DKContributionIntel.State.GO_TO_AGENT.ordinal && ourIntel.state.ordinal < MPC_DKContributionIntel.State.QM_DEPOSED.ordinal
            }

            "deposingQMOrUpgrading" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                if (ourIntel.state == MPC_DKContributionIntel.State.INFILTRATE_AND_UPGRADE_UMBRA) {
                    return true
                }
                return ourIntel.state.ordinal > MPC_DKContributionIntel.State.GO_TO_AGENT.ordinal && ourIntel.state.ordinal < MPC_DKContributionIntel.State.QM_DEPOSED.ordinal
            }

            "startPlantStage" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.PLANT_EVIDENCE
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.PLANT_EVIDENCE, dialog.textPanel)

                return true
            }
            "plantingEvidence" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state == MPC_DKContributionIntel.State.PLANT_EVIDENCE
            }

            "lootedCache" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.GOT_CACHE
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.GOT_CACHE, dialog.textPanel)

                return true
            }
            "returningWithCache" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state == MPC_DKContributionIntel.State.GOT_CACHE
            }

            "fuelProdHasGoodCore" -> {
                val fuelProd = Global.getSector().economy.getMarket("sindria")?.industries?.firstOrNull { it.spec.hasTag(Industries.FUELPROD) } ?: return false
                return fuelProd.aiCoreId != null
            }
            "addCoreToFuelProd" -> {
                val fuelProd = Global.getSector().economy.getMarket("sindria")?.industries?.firstOrNull { it.spec.hasTag(Industries.FUELPROD) } ?: return false

                val coreId = params[1].getString(memoryMap)
                fuelProd.aiCoreId = coreId
                Global.getSoundPlayer().playUISound(Global.getSettings().getCommoditySpec(coreId)?.soundIdDrop, 1f, 1f)
                checkDemandAndUpdate(dialog)
                return true
            }
            "addCoreToMining" -> {
                val fuelProd = Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) } ?: return false

                val coreId = params[1].getString(memoryMap)
                fuelProd.aiCoreId = coreId
                Global.getSoundPlayer().playUISound(Global.getSettings().getCommoditySpec(coreId)?.soundIdDrop, 1f, 1f)
                checkDemandAndUpdate(dialog)
                return true
            }
            "addBoreToMining" -> {
                val fuelProd = Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) } ?: return false

                fuelProd.specialItem = SpecialItemData(Items.MANTLE_BORE, null)
                Global.getSoundPlayer().playUISound(Global.getSettings().getCommoditySpec(Items.MANTLE_BORE)?.soundIdDrop, 1f, 1f)
                checkDemandAndUpdate(dialog)
                return true
            }

            "umbraMiningIsImproved" -> {
                return Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) }?.isImproved == true
            }
            "umbraMiningHasAlpha" -> {
                return Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) }?.aiCoreId == Commodities.ALPHA_CORE
            }
            "umbraMiningHasBore" -> {
                return Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) }?.specialItem != null
            }

            "improveUmbraMining" -> {
                Global.getSector().economy.getMarket("umbra")?.industries?.firstOrNull { it.spec.hasTag(Industries.MINING) }?.isImproved = true
                checkDemandAndUpdate(dialog)
            }

            "QMdeposed" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.QM_DEPOSED
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.QM_DEPOSED, dialog.textPanel)

                val umbra = Global.getSector().economy.getMarket("umbra") ?: return false
                for (entry in umbra.commDirectory.entriesCopy) {
                    if (entry.entryData !is PersonAPI) continue
                    if ((entry.entryData as PersonAPI).postId == Ranks.POST_SUPPLY_OFFICER) {
                        umbra.commDirectory.removePerson(
                            entry.entryData as PersonAPI
                        )
                        break
                    }
                }

                Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR).postId = Ranks.POST_SUPPLY_OFFICER

                return true
            }
            "qmIsDeposed" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state == MPC_DKContributionIntel.State.QM_DEPOSED
            }

            "beginUpgradeSequence" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.INFILTRATE_AND_UPGRADE_UMBRA
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.INFILTRATE_AND_UPGRADE_UMBRA, dialog.textPanel)
                Global.getSector().economy.getMarket("umbra")?.addCondition("MPC_DKInfiltrationCondition")
                Global.getSector().economy.getMarket("umbra")?.removeCondition(Conditions.VOLATILES_DIFFUSE)
                (Global.getSector().economy.getMarket("umbra")?.getIndustry(Industries.MINING) as? BaseIndustry)?.getSupply(Commodities.VOLATILES)?.quantity?.unmodify()
                Global.getSector().economy.getMarket("umbra")?.addCondition(Conditions.VOLATILES_PLENTIFUL)

                return true
            }
            "calibriIsQM" -> {
                return Global.getSector().importantPeople.getPerson(MPC_People.UMBRA_INFILTRATOR)?.postId == Ranks.POST_SUPPLY_OFFICER
            }

            "disruptIndustriesOfUmbra" -> {
                var toDisrupt = 2f
                Global.getSector().economy.getMarket("umbra")?.industries?.shuffled()?.forEach {
                    if (toDisrupt > 0f && it.canBeDisrupted()) {
                        it.setDisrupted(90f)
                        toDisrupt--
                    }
                }
            }

            "returningToMacarioFinalTime" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                return ourIntel.state == MPC_DKContributionIntel.State.RETURN_TO_MACARIO_CAUSE_DONE
            }

            "pullOut" -> {
                val ourIntel = MPC_DKContributionIntel.get() ?: return false
                ourIntel.state = MPC_DKContributionIntel.State.DONE
                ourIntel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.DONE, dialog.textPanel)
                ourIntel.endAfterDelay()

                val intel = MPC_IAIICFobIntel.get() ?: return false
                val toRemove = intel.factionContributions.firstOrNull { it.factionId == Factions.DIKTAT } ?: return false
                intel.removeContribution(toRemove, false, dialog)

            }
        }
        return false
    }

    class MPC_IAIICHadenFleetScript(): niko_MPC_baseNikoScript() {
        val interval = IntervalUtil(1f, 2f) // days

        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().addScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            val days = Misc.getDays(amount)
            interval.advance(days)
            if (interval.intervalElapsed()) {
                val script = Global.getSector().scripts.firstOrNull { it is PersonalFleetHoracioCaden } as? PersonalFleetHoracioCaden ?: return stop()
                val fleet = script.fleet ?: return

                if (MPC_IAIICFobIntel.get() == null || Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_IAIICInNegotiationsWithCaden")) {
                    fleet.makeUnimportant("\$MPC_IAIICCadenMeet")
                    stop()
                    return
                } else {
                    fleet.makeImportant("\$MPC_IAIICCadenMeet")
                }
            }
        }
    }

}