package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_aloofTargetAssignmentAI
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel.TargetHouse
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyMilitaristicHouseEventIntel
import org.magiclib.kotlin.getSourceMarket

class MPC_IAIICHegeCMD: BaseCommandPlugin() {

    companion object {
        fun createDuelFleet(isPlayerFleet: Boolean): CampaignFleetAPI {
            val startFac = if (isPlayerFleet) Factions.PLAYER else Factions.HEGEMONY
            val fleet = FleetFactory.createEmptyFleet(startFac, null, null)
            val name = if (isPlayerFleet) "Your Fleet" else "Duel Target (INTERIM NAME)"

            fleet.isNoFactionInName = true
            fleet.name = name

            val ourShip = fleet.fleetData.addFleetMember("afflictor_MPC_duel")
            val newVariant = ourShip.variant.clone()
            newVariant.addPermaMod(HullMods.HARDENED_SUBSYSTEMS, true)
            newVariant.addPermaMod(HullMods.REINFORCEDHULL, true)
            newVariant.addPermaMod(HullMods.ADAPTIVE_COILS, true)
            ourShip.setVariant(newVariant, true, true) // just temporary
            fleet.commander = if (isPlayerFleet) Global.getSector().playerPerson else MPC_People.getImportantPeople()[MPC_People.HEGE_MORALIST_ARISTO_REP]
            ourShip.captain = if (isPlayerFleet) Global.getSector().playerPerson else MPC_People.getImportantPeople()[MPC_People.HEGE_MORALIST_ARISTO_REP]

            fleet.fleetData.setSyncNeeded()
            fleet.fleetData.syncIfNeeded()

            ourShip.repairTracker.cr = ourShip.repairTracker.maxCR

            fleet.cargo.addCrew(12)

            val eventide = Global.getSector().economy.getMarket("eventide")!!
            eventide.primaryEntity.containingLocation.addEntity(fleet)
            fleet.setLocation(eventide.primaryEntity.location.x, eventide.primaryEntity.location.y)

            return fleet
        }
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val interactionTarget = dialog.interactionTarget
        //val market = interactionTarget.market ?: return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "canAddMeetDaudInitialOption" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.HEGEMONY }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialDaudMeet")) return false
                //if (!market.isFractalMarket()) return false
                return true
            }

            "pullOut" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                val toRemove = intel.getContributionById(Factions.HEGEMONY) ?: return false
                intel.removeContribution(toRemove, false, dialog)
            }

            "aristoComms" -> {
                val person = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR) ?: return false
                Global.getSettings().loadTexture("graphics/portraits/MPC_aristoComms.png")
                person.portraitSprite = "graphics/portraits/MPC_aristoComms.png"
            }
            "aristoNormal" -> {
                val person = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR) ?: return false
                person.portraitSprite = "graphics/portraits/portrait_hegemony10.png"
            }

            "canDoEventideMeeting" -> {
                return MPC_hegemonyContributionIntel.get(false)?.state == MPC_hegemonyContributionIntel.State.GO_TO_EVENTIDE_INIT
            }

            "startQuest" -> {
                MPC_hegemonyContributionIntel.get()?.state = MPC_hegemonyContributionIntel.State.CONVINCE_HOUSES
                MPC_hegemonyContributionIntel.get()?.sendUpdateIfPlayerHasIntel(
                    MPC_hegemonyContributionIntel.State.CONVINCE_HOUSES,
                    dialog.textPanel
                )

                val eventide = Global.getSector().economy.getMarket("eventide") ?: throw RuntimeException("EVENTIDE DOESNT EXIST HOW THE FUCK DID THIS HAPPEN")
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_MORALIST_ARISTO_REP))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_MILITARIST_ARISTO_REP))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_OPPORTUNISTIC_ARISTO_REP))
            }

            "dealingWithAHouse" -> {
                return MPC_hegemonyContributionIntel.get(false)?.currentHouse != TargetHouse.NONE
            }
            "cooldownActive" -> {
                return MPC_hegemonyContributionIntel.get(false)?.cooldownActive == true
            }

            "newHouse" -> {
                val house = params[1].getString(memoryMap)
                val newHouse = TargetHouse.valueOf(house) ?: return false

                MPC_hegemonyContributionIntel.get()?.setNewHouse(newHouse, dialog.textPanel)
            }

            "createIntel" -> {
                val intel = MPC_hegemonyContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel("Rumors of involvement", dialog.textPanel)
            }

            "houseTurned" -> {
                MPC_hegemonyContributionIntel.get()?.turnedHouse(dialog.textPanel)
                Global.getSector().memoryWithoutUpdate.set("\$MPC_hegeIAIICHouseCooldown", true, 30f)
            }

            "houseIs" -> {
                val houseName = params[1].getString(memoryMap)
                return MPC_hegemonyContributionIntel.get()?.currentHouse?.name == houseName
            }

            "readyToConfrontMil" -> {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get(false) ?: return false
                return intel.progress >= intel.maxProgress
            }

            "OPPgoToReadings" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.GO_TO_MESON_READINGS
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }
            "OPPsetCourse" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                val readings = MPC_hegemonyContributionIntel.getAlphaSite().getCustomEntitiesWithTag("MPC_riftRemnant").firstOrNull() ?: return false

                Global.getSector().layInCourseFor(readings)
            }
            "OPPspawnOmega" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.spawnWormholeOmega()
            }

            "OPPgoToMesonPlanet" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.GO_TO_MESON_PLANET
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }

            "OPPfoundZiggComplex" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.APPROACH_ZIGG_COMPLEX
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }

            "OPPshowZiggComplexFleet" -> {
                val planet = MPC_hegemonyContributionIntel.getZiggComplexPlanet()
                val fleet = planet.memoryWithoutUpdate.getFleet("\$defenderFleet") ?: return false
                dialog.visualPanel.showFleetInfo(
                    "Your Fleet",
                    Global.getSector().playerFleet,
                    "Hostile Contact",
                    fleet
                )
            }
            "OPPapproachedZiggComplex" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.CLEAR_ZIGG_COMPLEX
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }

            "OPPgotData" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.RETURN_WITH_DATA
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }

            "OPPgenZigLoot" -> {
                val planet = MPC_hegemonyContributionIntel.getZiggComplexPlanet()
                planet.memoryWithoutUpdate[MemFlags.SALVAGE_SPEC_ID_OVERRIDE] = "MPC_omega_zigg_complex"
            }

            "getFractalCoreColonyName" -> {
                val colony = getFractalColony() ?: return false
                Global.getSector().memoryWithoutUpdate["\$MPC_fractalColonyName"] = colony.name
                return true
            }

            "OPPisReadyToTurnIn" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                return intel.opportunisticState == MPC_hegemonyContributionIntel.OpportunisticState.RETURN_WITH_DATA
            }

            "OPPtraitor" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.state = MPC_hegemonyContributionIntel.State.FAILED
                intel.sendUpdateIfPlayerHasIntel(intel.state, dialog.textPanel)
                intel.endAfterDelay()

                val fobIntel = MPC_IAIICFobIntel.get() ?: return false
                fobIntel.retaliate(
                    MPC_IAIICFobIntel.RetaliateReason.BETRAYED_LINDUNBERG,
                    dialog.textPanel
                )
            }

            "genericFail" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.state = MPC_hegemonyContributionIntel.State.FAILED
                intel.sendUpdateIfPlayerHasIntel(intel.state, dialog.textPanel)
                intel.endAfterDelay()
            }

            "ALOgetIntel" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.GET_INTEL
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }
            "ALOstateIs" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                val check = params[1].getString(memoryMap)
                return (intel.aloofState?.name == check)
            }
            "ALOincrementEvidence" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.evidencePieces++
                intel.sendUpdateIfPlayerHasIntel("EVIDENCE_PIECES", dialog.textPanel)
                if (intel.evidencePieces >= MPC_hegemonyContributionIntel.EVIDENCE_NEEDED) {
                    intel.aloofState = MPC_hegemonyContributionIntel.AloofState.GOT_EVIDENCE
                    intel.sendUpdateIfPlayerHasIntel(MPC_hegemonyContributionIntel.AloofState.GOT_EVIDENCE, dialog.textPanel)
                }
            }
            "ALOevidenceDelivered" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.WAIT_FOR_ALOOF
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)

                intel.aloofTimer = if (Global.getSettings().isDevMode) 0.1f else 3f
            }
            "ALOeliminateTarget" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.ELIMINATE_TARGET
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }
            "ALOhegeIsFriendly" -> {
                return Global.getSector().getFaction(Factions.HEGEMONY).relToPlayer.isAtWorst(RepLevel.FRIENDLY)
            }
            "ALOtargetBribed" -> {
                val target: CampaignFleetAPI = dialog.interactionTarget as? CampaignFleetAPI ?: return false
                val ai: MPC_aloofTargetAssignmentAI =
                    (target.scripts.firstOrNull { it is MPC_aloofTargetAssignmentAI } ?: return false) as MPC_aloofTargetAssignmentAI
                ai.delete()

                target.clearAssignments()
                target.addAssignmentAtStart(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    target.getSourceMarket().primaryEntity,
                    Float.MAX_VALUE,
                    null
                )

                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.ELIMINATE_TARGET_FINISHED
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }

            "HONbeginFirstDuel" -> {

            }
            "HONbeginShipDuel" -> {
                if (dialog == null) return false

                val sector = Global.getSector() ?: return false
                val oldFleet = sector.playerFleet

                val newFleet = createDuelFleet(true)
                val targetFleet = createDuelFleet(false)
                sector.playerFleet = newFleet

                Global.getSector().memoryWithoutUpdate["\$MPC_playerFleetStorage"] = oldFleet

                dialog.interactionTarget = targetFleet

                val config = FIDConfig()
                config.leaveAlwaysAvailable = true
                config.showCommLinkOption = false
                config.showEngageText = false
                config.showFleetAttitude = false
                config.showTransponderStatus = false
                config.showWarningDialogWhenNotHostile = false
                config.alwaysAttackVsAttack = true
                config.impactsAllyReputation = false
                config.impactsEnemyReputation = false
                config.pullInAllies = false
                config.pullInEnemies = false
                config.pullInStations = false
                config.lootCredits = false

                config.firstTimeEngageOptionText = "DUEL!"
                config.afterFirstTimeEngageOptionText = "DUEL!"
                config.noSalvageLeaveOptionText = "Continue"

                config.dismissOnLeave = false
                config.printXPToDialog = true

                val entity = dialog.interactionTarget
                val originalPlugin = dialog.plugin
                val plugin = FleetInteractionDialogPluginImpl(config)

                config.delegate = object : FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    override fun notifyLeave(dialog: InteractionDialogAPI) {
                        super.notifyLeave(dialog)

                        targetFleet.memoryWithoutUpdate.clear()
                        // there's a "standing down" assignment given after a battle is finished that we don't care about
                        targetFleet.clearAssignments()
                        targetFleet.deflate()
                        targetFleet.despawn()

                        dialog.plugin = originalPlugin
                        dialog.interactionTarget = entity

                        Global.getSector().playerFleet = Global.getSector().memoryWithoutUpdate.getFleet("\$MPC_playerFleetStorage")
                        Global.getSector().memoryWithoutUpdate.unset("\$MPC_playerFleetStorage")

                        newFleet.memoryWithoutUpdate.clear()
                        // there's a "standing down" assignment given after a battle is finished that we don't care about
                        newFleet.clearAssignments()
                        newFleet.deflate()
                        newFleet.despawn()

                        if (plugin.context is FleetEncounterContext) {
                            val context = plugin.context as FleetEncounterContext
                            if (context.didPlayerWinEncounterOutright()) {
                                FireBest.fire(null, dialog, memoryMap, "MPC_IAIICHegeHonWonDuel")
                            } else {
                                FireBest.fire(null, dialog, memoryMap, "MPC_IAIICHegeHonLostDuel")
                            }
                        }
                    }
                }

                dialog.plugin = plugin
                plugin.init(dialog)
            }
            "HONendShipDuel" -> {
                Global.getSector().playerFleet = Global.getSector().memoryWithoutUpdate.getFleet("\$MPC_playerFleetStorage")
                Global.getSector().memoryWithoutUpdate.unset("\$MPC_playerFleetStorage")
            }
            "HONbeginCOMSECDuel" -> {

            }
            "HONfuckingDie" -> {
                val campaignUI = Global.getSector().campaignUI ?: return false
                campaignUI.cmdExitWithoutSaving()
            }
        }

        return false
    }
}