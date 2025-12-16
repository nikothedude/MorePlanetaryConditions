package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.eventide.Actions
import com.fs.starfarer.api.impl.campaign.eventide.DuelDialogDelegate
import com.fs.starfarer.api.impl.campaign.eventide.DuelPanel
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseIntel
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseManager
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.respawnAllFleets
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyUnrestScript
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_aloofTargetAssignmentAI
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel.TargetHouse
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyMilitaristicHouseEventIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.IAIIC_QUEST
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.magiclib.kotlin.getFactionMarkets
import org.magiclib.kotlin.getSourceMarket
import org.magiclib.kotlin.getStationIndustry
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import sound.int

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

            val playerFleet = Global.getSector().playerFleet
            playerFleet.containingLocation.addEntity(fleet)
            fleet.setLocation(playerFleet.location.x, playerFleet.location.y)

            return fleet
        }

        fun playerSkilledDuelist(): Boolean = Global.getSector().playerMemoryWithoutUpdate.getBoolean("\$bladeSkill")
        fun createCOMSECDuel(playerSkilled: Boolean, enemySkilled: Boolean, ambienceLoopID: String?): DuelPanel {
            val panel = DuelPanel.createDefault(playerSkilled, enemySkilled, ambienceLoopID)
            val enemy = panel.enemy
            enemy.maxHealth = 7
            enemy.health = 7

            val speedMult = 1.2f

            enemy.actionSpeedMult[Actions.ATTACK_HIGH] = speedMult
            enemy.actionSpeedMult[Actions.ATTACK] = speedMult

            return panel
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
                if (Global.getSector().economy.getMarket("eventide")?.isInhabited() != true) return false
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
                Global.getSector().importantPeople.getPerson(People.DAUD).makeUnimportant(IAIIC_QUEST)
                Global.getSector().economy.getMarket("eventide").primaryEntity.makeImportant(IAIIC_QUEST)
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
            "generalStateIs" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                val check = params[1].getString(memoryMap)
                return (intel.state.name == check)
            }
            "beginFinalWait" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.state = MPC_hegemonyContributionIntel.State.WAIT
                intel.sendUpdateIfPlayerHasIntel(intel.state, dialog.textPanel)

                MPC_hegemonyUnrestScript().start()
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
            "ALOgivenRaidTask" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.CREATE_SCAPEGOAT
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }
            "ALOscapegoatMade" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.INSERT_EVIDENCE
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }
            "ALOevidencePlanted" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.aloofState = MPC_hegemonyContributionIntel.AloofState.FINALIZE
                intel.sendUpdateIfPlayerHasIntel(intel.aloofState, dialog.textPanel)
            }
            "ALObeginOrthusPayback" -> {
                MPC_OrthusPaybackScript().start()
            }

            "HONswitchToEntity" -> {

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

                val entity = dialog.interactionTarget
                dialog.interactionTarget = targetFleet

                val config = FIDConfig()
                config.leaveAlwaysAvailable = false
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

                val originalPlugin = dialog.plugin
                val plugin = object : FleetInteractionDialogPluginImpl(config) {
                    override fun updateEngagementChoice(withText: Boolean) {
                        super.updateEngagementChoice(withText)

                        options.removeOption(OptionId.ATTEMPT_TO_DISENGAGE)
                        options.removeOption(OptionId.DISENGAGE)
                        options.removeOption(OptionId.CLEAN_DISENGAGE)
                    }
                }

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
            "HONbeginDuelTutorial" -> {
                val skilled = playerSkilledDuelist()
                val duelPanel = DuelPanel.createTutorial(skilled, null)
                dialog.showCustomVisualDialog(1024f, 700f, MPC_IAIICHonorableDuelDelegate(null, duelPanel, dialog, memoryMap, true))
            }
            "HONbeginNormalDuel" -> {
                /*val skilled = playerSkilledDuelist()
                val duelPanel = DuelPanel.createDefault(skilled, )
                dialog.showCustomVisualDialog(1024f, 700f, DuelDialogDelegate(null, duelPanel, dialog, memoryMap, true))*/
            }
            "HONbeginCOMSECDuel" -> {
                val skilled = playerSkilledDuelist()
                val duelPanel = createCOMSECDuel(skilled, true, "soe_ambience")
                dialog.showCustomVisualDialog(1024f, 700f, MPC_IAIICHonorableDuelDelegate(null, duelPanel, dialog, memoryMap, false))
            }
            "HONforceSave" -> {
                val campaignUI = Global.getSector().campaignUI ?: return false
                //campaignUI.cmdSave()
            }
            "HONfuckingDie" -> {
                val campaignUI = Global.getSector().campaignUI ?: return false
                campaignUI.cmdExitWithoutSaving()
            }
            "HONisEnoughHonor" -> {
                val intel = MPC_hegemonyContributionIntel.get() ?: return false
                return intel.honor >= MPC_hegemonyContributionIntel.HONOR_NEEDED
            }
            "HONincrementHonor" -> {
                val intel = MPC_hegemonyContributionIntel.get() ?: return false
                intel.incrementHonor(dialog.textPanel)
            }
            "HONsendGenericUpdate" -> {
                val intel = MPC_hegemonyContributionIntel.get() ?: return false
                val gen = params[1].getString(memoryMap)
                intel.sendUpdateIfPlayerHasIntel(gen, dialog.textPanel)
            }
            "HONbeginKillingTraitor" -> {
                val hesp = Global.getSector().economy.getMarket("hesperus")
                hesp.primaryEntity.makeImportant(IAIIC_QUEST)

                val intel = MPC_hegemonyContributionIntel.get() ?: return false
                intel.sendUpdateIfPlayerHasIntel("MPC_IAIICHegeHonKillingTraitor", dialog.textPanel)
            }
            "HONgetTargetPather" -> {
                var intelRaw: List<LuddicPathBaseIntel> = Global.getSector().intelManager.getIntel(LuddicPathBaseIntel::class.java) as List<LuddicPathBaseIntel>
                var intel = intelRaw.filter { !it.market.starSystem.isEnteredByPlayer }
                if (intel.isEmpty()) intel = intelRaw
                val manager = LuddicPathBaseManager.getInstance()
                if (intel.isEmpty()) {
                    manager.advance(100000f)
                    intel = Global.getSector().intelManager.getIntel(LuddicPathBaseIntel::class.java) as List<LuddicPathBaseIntel>
                }
                val target = intel.random().market
                val fp = Global.getSector().playerFleet.fleetPoints.coerceAtLeast(350).toFloat()

                val params = FleetParamsV3(
                    target,
                    target.locationInHyperspace,
                    Factions.LUDDIC_PATH,
                    null,
                    FleetTypes.PATROL_LARGE,
                    fp,  // combatPts
                    0f,  // freighterPts
                    0f,  // tankerPts
                    0f,  // transportPts
                    0f,  // linerPts
                    0f,  // utilityPts
                    0f // qualityMod
                )
                params.officerNumberMult = 3f
                val fleet = FleetFactoryV3.createFleet(params)

                target.containingLocation.addEntity(fleet)
                fleet.setLocation(target.primaryEntity.location.x, target.primaryEntity.location.y)

                class AfterMarketGoneAssignment(): Script {
                    override fun run() {
                        val despawnLoc = Global.getSector().getFaction(Factions.LUDDIC_PATH).getFactionMarkets().randomOrNull() ?: Global.getSector().economy.marketsCopy.random()

                        fleet.clearAssignments()
                        fleet.addAssignment(
                            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                            despawnLoc.primaryEntity,
                            Float.MAX_VALUE
                        )
                    }

                }

                fleet.isNoFactionInName = true
                fleet.name = "${target.name} Sacred Backguard"
                fleet.addAssignment(
                    FleetAssignment.ORBIT_AGGRESSIVE,
                    target.primaryEntity,
                    9999f,
                    "in eternal vigil",
                    AfterMarketGoneAssignment()
                )

                target.primaryEntity.customDescriptionId = "MPC_IAIICIronPathDefectorMkt"

                target.primaryEntity.makeImportant(IAIIC_QUEST)
                var admin = target.admin
                if (admin == null || admin.name.first == "") {
                    admin = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson()
                    target.admin = admin
                    target.commDirectory.addPerson(admin)
                }
                admin.makeImportant(IAIIC_QUEST)
                admin.stats.setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1f)
                admin.memoryWithoutUpdate.set("\$MPC_IAIICHegeHonIronPathDefector", true)

                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICHonPatherTargetMarketId"] = target.id
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICHonPatherTargetMarketName"] = target.name
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICHonPatherTargetMarketSysName"] = target.starSystem.name
                MPC_IAIICHegeHonPatherTargetChecker(target).start()

                val station = target.getStationIndustry()
                if (station != null) {
                    target.removeIndustry(station.id, null, false)
                }
                target.addIndustry(Industries.STARFORTRESS)
                target.removeIndustry(Industries.PATROLHQ, null, false)
                target.removeIndustry(Industries.MILITARYBASE, null, false)
                target.removeIndustry(Industries.HIGHCOMMAND, null, false)
                target.addIndustry(Industries.HIGHCOMMAND)
                target.getIndustry(Industries.HIGHCOMMAND).isImproved = true
                target.removeIndustry(Industries.GROUNDDEFENSES, null, false)
                target.removeIndustry(Industries.HEAVYBATTERIES, null, false)
                target.addIndustry(Industries.HEAVYBATTERIES)
                target.addIndustry(Industries.WAYSTATION)

                target.respawnAllFleets()

            }
            "HONbeginDuelPrep" -> {
                val contribIntel = MPC_hegemonyContributionIntel.get() ?: return false
                contribIntel.honorableState = MPC_hegemonyContributionIntel.HonorableState.WIN_FINAL_DUEL
                contribIntel.sendUpdateIfPlayerHasIntel(contribIntel.honorableState, dialog.textPanel)
            }
            "HONbeginPatherHunt" -> {
                val contribIntel = MPC_hegemonyContributionIntel.get() ?: return false
                contribIntel.sendUpdateIfPlayerHasIntel("MPC_killingJerusDeserters", dialog.textPanel)
            }
            "HONstateIs" -> {
                val intel = MPC_hegemonyContributionIntel.get() ?: return false
                val check = params[1].getString(memoryMap)
                return (intel.honorableState?.name == check)
            }
        }

        return false
    }

    class MPC_IAIICHegeHonPatherTargetChecker(val market: MarketAPI): niko_MPC_baseNikoScript() {
        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            if (!market.isInEconomy) {
                val intel = MPC_hegemonyContributionIntel.get()
                intel?.sendUpdateIfPlayerHasIntel("HON_KILLED_DESERTERS", null)
                intel?.incrementHonor(null)
                Global.getSector().playerPerson.memoryWithoutUpdate["\$MPC_didJerusDesertersTask"] = true
                delete()
            }
        }

    }

    class MPC_OrthusPaybackScript(): niko_MPC_baseNikoScript() {
        val interval = IntervalUtil(30f, 35f) // days

        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            interval.advance(Misc.getDays(amount))
            if (interval.intervalElapsed()) {
                Global.getSector().memoryWithoutUpdate["\$MPC_doOrthusRetaliation"] = true
                delete()
            }
        }

    }

    class MPC_IAIICHonorableDuelDelegate(
        musicId: String?, duelPanel: DuelPanel, dialog: InteractionDialogAPI,
        memoryMap: MutableMap<String, MemoryAPI>?, tutorialMode: Boolean
    ): DuelDialogDelegate(musicId, duelPanel, dialog, memoryMap, tutorialMode) {
        override fun reportDismissed(option: Int) {

//		Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false);
//		Global.getSoundPlayer().restartCurrentMusic();
            if (memoryMap != null) { // null when called from the test dialog
                if (!tutorialMode) {
                    if (duelPanel.getPlayer().health > 0) {
                        memoryMap[MemKeys.LOCAL]!!.set("\$MPC_IAIICHonPlayerWonCOMSECDuel", true, 0f)
                    } else {
                        memoryMap[MemKeys.LOCAL]!!.set("\$MPC_IAIICHonPlayerLostCOMSECDuel", true, 0f)
                    }
                    FireBest.fire(null, dialog, memoryMap, "MPC_IAIICHonCOMSECDuelFinished")
                } else {
                    FireBest.fire(null, dialog, memoryMap, "MPC_IAIICHonTutorialDuelFinished")
                }
            }
        }
    }
}