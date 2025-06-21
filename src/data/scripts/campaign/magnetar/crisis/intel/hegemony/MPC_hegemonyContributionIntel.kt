package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyMilitaristicHouseEventIntel.Companion.addStartLabel
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.IAIIC_QUEST
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeNonStoryCritical
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color
import java.lang.Math.random

class MPC_hegemonyContributionIntel: BaseIntelPlugin() {

    companion object {
        fun getAlphaSite(): StarSystemAPI = Global.getSector().getStarSystem("Unknown Location")

        fun get(withUpdate: Boolean = false): MPC_hegemonyContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_hegemonyContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_hegemonyContributionIntel
        }

        fun retaliate(panel: TextPanelAPI? = null) {
            val intel = MPC_IAIICFobIntel.Companion.get() ?: return
            intel.retaliate(
                MPC_IAIICFobIntel.RetaliateReason.TURNING_HOUSES_AGAINST_HEGEMONY,
                panel
            )
        }

        fun getZiggComplexPlanet(): PlanetAPI {
            return (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as StarSystemAPI).planets.first { it.id == "MPC_magnetarSystemPlanetTwo" }
        }

        fun createEliminationTargetFleet(): CampaignFleetAPI {
            val eventide = Global.getSector().economy.getMarket("eventide")!!

            val params = FleetParamsV3(
                eventide,
                FleetTypes.ACADEMY_FLEET,
                50f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
            )
            val fleet = FleetFactoryV3.createFleet(params)!!
            eventide.containingLocation.addEntity(fleet)
            fleet.setLocation(eventide.primaryEntity.location.x, eventide.primaryEntity.location.y)
            //fleet.flagship.captain = MPC_People.getImportantPeople()[MPC_People.HEGE_ALOOF_SISTER]
            fleet.commander = MPC_People.getImportantPeople()[MPC_People.HEGE_ALOOF_SISTER]
            fleet.isNoFactionInName = true
            fleet.name = "Aristocratic Delegation"
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MISSION_IMPORTANT] = true
            fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS] = true
            fleet.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            MPC_aloofTargetAssignmentAI(fleet).start()

            return fleet
        }

        const val KEY = "\$MPC_hegeContributionIntel"
        const val HOUSES = 4
        const val EVIDENCE_NEEDED = 3
    }

    var housesTurned = 0
    var currentHouse = TargetHouse.NONE
        set(value) {
            field.unapply()
            value.apply()

            field = value
        }

    enum class State {
        GO_TO_EVENTIDE_INIT,
        CONVINCE_HOUSES,
        DONE,
        FAILED
    }
    enum class TargetHouse(val stringName: String) {
        NONE("None"),
        ALOOF("Youn"),
        OPPORTUNISTIC("Lindunberg") {
            override fun apply(text: TextPanelAPI?) {
                //val intel = MPC_hegemonyMilitaristicHouseEventIntel.get(true)!!
                if (text != null) {
                    get(true)!!.opportunisticState = OpportunisticState.GO_TO_ALPHA_SITE
                    get(true)!!.sendUpdateIfPlayerHasIntel(OpportunisticState.GO_TO_ALPHA_SITE, text)
                }
                Global.getSector().memoryWithoutUpdate["\$MPC_investigatingAlphaSiteAgain"] = true
            }
        },
        MILITARISTIC("Mellour") {
            override fun apply(text: TextPanelAPI?) {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get(true)!!
                if (text != null) {
                    intel.sendUpdateIfPlayerHasIntel(null, text)
                }
            }
            override fun unapply(text: TextPanelAPI?) {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get() ?: return
                intel.endAfterDelay()
            }
        },
        HONORABLE("FOUR");

        open fun apply(text: TextPanelAPI? = null) {}
        open fun unapply(text: TextPanelAPI? = null) {}
    }

    enum class AloofState {
        GET_INTEL {
            override fun apply() {
                val chicoRelay = Global.getSector().getEntityById("aztlan_relay")
                chicoRelay?.makeImportant(niko_MPC_ids.IAIIC_QUEST)

                val chico = Global.getSector().economy.getMarket("chicomoztoc")
                chico?.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)

                val fractalColony = getFractalColony()!!
                val system = fractalColony.containingLocation as? StarSystemAPI

                system?.getCustomEntitiesWithTag(Tags.COMM_RELAY)?.forEach { it.makeImportant(niko_MPC_ids.IAIIC_QUEST) }
            }
            override fun unapply() {
                val chicoRelay = Global.getSector().getEntityById("aztlan_relay")
                chicoRelay?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)

                val chico = Global.getSector().economy.getMarket("chicomoztoc")
                chico?.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)

                val fractalColony = getFractalColony()!!
                val system = fractalColony.containingLocation as? StarSystemAPI

                system?.getCustomEntitiesWithTag(Tags.COMM_RELAY)?.forEach { it.makeUnimportant(niko_MPC_ids.IAIIC_QUEST) }
            }
        },
        GOT_EVIDENCE {

        },
        WAIT_FOR_ALOOF {

        },
        ELIMINATE_TARGET {
            override fun apply() {
                createEliminationTargetFleet()
            }
        },
        ELIMINATE_TARGET_FINISHED {

        };

        open fun apply() {}
        open fun unapply() {}
    }

    enum class OpportunisticState {
        GO_TO_ALPHA_SITE {
            override fun apply() {
                super.apply()

                getAlphaSite().planets.firstOrNull { it.id == "site_alpha" }?.makeImportant("\$MPC_IAIICImportant")
            }

            override fun unapply() {
                super.unapply()

                getAlphaSite().planets.firstOrNull { it.id == "site_alpha" }?.makeUnimportant("\$MPC_IAIICImportant")
            }
        },
        GO_TO_MESON_READINGS {
            override fun apply() {
                super.apply()

                val loc = getAlphaSite()

                val readings = loc.addCustomEntity(
                    "MPC_mesonReadings",
                    "Meson Readings",
                    Entities.MISSION_LOCATION,
                    Factions.NEUTRAL,
                )
                readings.customDescriptionId = "MPC_riftRemnant"
                readings.addTag("MPC_riftRemnant")
                readings.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "music_campaign_alpha_site"
                readings.makeImportant(IAIIC_QUEST)
                readings.setLocation(-12000f, 1000f)
            }

            override fun unapply() {
                val loc = getAlphaSite()
                val readings = loc.getCustomEntitiesWithTag("MPC_riftRemnant").firstOrNull() ?: return
                readings.makeUnimportant(IAIIC_QUEST)
            }
        },
        GO_TO_MESON_PLANET,
        APPROACH_ZIGG_COMPLEX {
            override fun apply() {
                getZiggComplexPlanet().makeImportant(IAIIC_QUEST)
            }
        },
        CLEAR_ZIGG_COMPLEX {
            override fun unapply() {
                getZiggComplexPlanet().makeUnimportant(IAIIC_QUEST)
            }
        },
        RETURN_WITH_DATA;

        open fun apply() {}
        open fun unapply() {}
    }
    var opportunisticState: OpportunisticState? = null
        set(value) {
            field?.unapply()
            value?.apply()

            field = value
        }
    var aloofState: AloofState? = null
        set(value) {
            field?.unapply()
            value?.apply()

            field = value
        }
    var evidencePieces = 0
        set(value) {
            field = value
            if (value == 2 || value == 3) {
                spawnHunterKiller()
            }
        }

    private fun spawnHunterKiller() {
        val r = MathUtils.getRandom()
        val e = DelayedFleetEncounter(r, "MPC_aloofHitSquad_${evidencePieces}")

        if (Global.getSettings().isDevMode) {
            e.setDelayNone()
        } else {
            e.setDelay(20f, 25f)
        }

        e.setDoNotAbortWhenPlayerFleetTooStrong() // small ships, few FP, but a strong fleet
        e.setLocationCoreOnly(true, Factions.HEGEMONY)
        e.beginCreate()
        e.triggerCreateFleet(
            FleetSize.MAXIMUM,
            HubMissionWithTriggers.FleetQuality.SMOD_3,
            Factions.HEGEMONY,
            FleetTypes.PATROL_LARGE,
            Vector2f()
        )
        e.triggerSetFleetCombatFleetPoints((Global.getSector().playerFleet.fleetPoints * 0.85f).coerceAtLeast(200f))
        //e.triggerSetFleetMaxShipSize(1)
        e.triggerSetFleetDoctrineOther(4, 4)

        e.triggerSetFleetDoctrineComp(4, 1, 0)

        e.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
        e.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1)
        e.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1)
        e.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1)
        e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.UNUSUALLY_HIGH)

        e.triggerFleetMakeFaster(true, 0, true)

        e.triggerSetFleetFaction(Factions.HEGEMONY)
        e.triggerMakeNoRepImpact()
        e.triggerSetStandardAggroInterceptFlags()
        e.triggerMakeFleetIgnoreOtherFleets()
        e.triggerSetFleetGenericHailPermanent("MPC_IAIICAloofHunter")
        e.triggerSetFleetFlagPermanent("\$MPC_IAIICAloofHunter")
        e.triggerFleetSetName("Kill-Fleet")
        e.endCreate()
    }

    fun spawnWormholeOmega() {
        val loc = getAlphaSite()
        val readings = loc.getCustomEntitiesWithTag("MPC_riftRemnant").firstOrNull() ?: return

        val combat = (Global.getSector().playerFleet.fleetPoints * 0.8f).coerceAtLeast(50f).coerceAtMost(600f)

        val params = FleetParamsV3(
            Global.getSector().playerFleet.locationInHyperspace,
            Factions.OMEGA,
            null,
            FleetTypes.PATROL_SMALL,
            combat.toFloat(),  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f
        )
        params.ignoreMarketFleetSizeMult = true
        params.qualityOverride = 1.2f
        params.maxShipSize = 2
        params.aiCores = OfficerQuality.AI_OMEGA

        val omega = Global.getSector().getFaction(Factions.OMEGA)
        val fleet = FleetFactoryV3.createFleet(params)

        fleet.memoryWithoutUpdate.set("\$genericHail", true)
        fleet.memoryWithoutUpdate.set("\$genericHail_openComms", "MPC_IAIICwormholeDefenseFleetHail")

        //fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE_ONE_BATTLE_ONLY, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true)

        for (member in fleet.membersWithFightersCopy) {
            // to "perm" the variant so it gets saved and not recreated
            member.setVariant(member.variant.clone(), false, false)
            member.variant.source = VariantSource.REFIT
            member.variant.addTag(Tags.SHIP_LIMITED_TOOLTIP)
        }

        loc.addEntity(fleet)
        fleet.setLocation(9999f, -9999f)
        val dest = JumpDestination(readings, null)
        Global.getSector().doHyperspaceTransition(fleet, null, dest)
        fleet.stats.fleetwideMaxBurnMod.modifyFlat("MPC_MAXSPEED", 20f)

        class despawner(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
            override fun executeImpl() {
                if (fleet.isAlive) fleet.despawn()
            }
        }
        despawner(IntervalUtil(14f, 14f)).start()
    }

    fun setNewHouse(house: TargetHouse, text: TextPanelAPI) {
        currentHouse.unapply()
        currentHouse = house
        house.apply(text)
    }

    var state: State = State.GO_TO_EVENTIDE_INIT

    var cooldownActive = false
    var cooldownDays = IntervalUtil(30f, 30f)
    var aloofTimer: Float? = null

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.HEGEMONY).crest
    }

    override fun getName(): String = "Hegemony involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.HEGEMONY, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        when (listInfoParam) {
            State.GO_TO_EVENTIDE_INIT -> {
                info.addPara(
                    "Go to %s",
                    5f,
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                    "eventide"
                )
            }
            State.CONVINCE_HOUSES -> {
                info.addPara(
                    "Convince %s to turn against the %s",
                    0f,
                    Misc.getHighlightColor(),
                    "noble houses",
                    "hegemony"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    factionForUIColors.baseUIColor
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )

                info.addPara(
                    "Current target: %s",
                    5f,
                    Misc.getHighlightColor(),
                    currentHouse.stringName
                )
            }
            TargetHouse.MILITARISTIC -> {
                info.addPara(
                    "Disrupt %s or destroy %s",
                    10f,
                    factionForUIColors.baseUIColor,
                    "hegemony industries", "hegemony fleets"
                )
            }
            TargetHouse.OPPORTUNISTIC -> {
            }
            OpportunisticState.GO_TO_ALPHA_SITE -> {
                info.addPara(
                    "Go to the %s",
                    10f,
                    Misc.getHighlightColor(),
                    "provided hyperspace coordinates"
                )
            }
            OpportunisticState.GO_TO_MESON_READINGS -> {
                info.addPara(
                    "Investigate the %s",
                    10f,
                    Misc.getHighlightColor(),
                    "meson readings"
                )
            }
            OpportunisticState.GO_TO_MESON_PLANET -> {
                info.addPara(
                    "Go to the %s system",
                    10f,
                    Misc.getHighlightColor(),
                    "magnetar"
                )
                info.addPara(
                    "Find and scan a planet with a %s",
                    0f,
                    Misc.getHighlightColor(),
                    "meson field"
                )
            }
            OpportunisticState.APPROACH_ZIGG_COMPLEX -> {
                info.addPara(
                    "Approach the %s",
                    0f,
                    Misc.getHighlightColor(),
                    "orbital complex"
                )
            }
            OpportunisticState.CLEAR_ZIGG_COMPLEX -> {
                info.addPara(
                    "Defeat the %s",
                    0f,
                    Misc.getHighlightColor(),
                    "garrison"
                )
            }
            OpportunisticState.RETURN_WITH_DATA -> {
                info.addPara(
                    "Return to %s",
                    0f,
                    Misc.getHighlightColor(),
                    "Eventide"
                )
            }
            AloofState.GET_INTEL -> {
                info.addPara(
                    "%s/%s pieces of proof",
                    0f,
                    Misc.getHighlightColor(),
                    evidencePieces.toString(), EVIDENCE_NEEDED.toString()
                )
            }
            AloofState.GOT_EVIDENCE -> {
                info.addPara(
                    "Return to %s",
                    0f,
                    Misc.getHighlightColor(),
                    "eventide"
                )
            }
            AloofState.WAIT_FOR_ALOOF -> {
                info.addPara(
                    "Wait %s",
                    0f,
                    Misc.getHighlightColor(),
                    "one week"
                )
            }
            "EVIDENCE_PIECES" -> {
                info.addPara(
                    "%s/%s pieces of proof",
                    0f,
                    Misc.getHighlightColor(),
                    evidencePieces.toString(), EVIDENCE_NEEDED.toString()
                )
            }
            "HOUSES_TURNED" -> {
                info.addPara(
                    "%s turned against the %s",
                    0f,
                    Misc.getHighlightColor(),
                    currentHouse.stringName,
                    "Hegemony"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    factionForUIColors.baseUIColor
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )
            }
            "COOLDOWN_EXPIRED" -> {
                info.addPara(
                    "Next mission now available",
                    5f
                )
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.HEGEMONY)
    }

    fun getAloofRep(): PersonAPI = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR)

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the hegemony may be involved in the IAIIC.",
            5f
        )

        when (state) {
            State.GO_TO_EVENTIDE_INIT -> {
                info.addPara(
                    "Following your fruitless meeting with the high hegemon, a strange individual claiming to be a eventide aristocrat " +
                    "claimed to have %s of %s involvement with the %s, and asked you to meet them at %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "evidence", "IAIIC", "hegemony", "eventide"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                    factionForUIColors.baseUIColor,
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                )
            }
            State.CONVINCE_HOUSES -> {
                val aloofRep = getAloofRep()
                info.addPara(
                    "You've met with %s - an aristocrat from the Eventide house %s, who has tasked you with turning %s against the %s. " +
                    "Supposedly, these houses are each uniquely positioned to be able to %s on the %s to pull out from the %s.",
                    5f,
                    Misc.getHighlightColor(),
                    aloofRep.nameString, aloofRep.name.last, "noble houses", "Hegemony", "put pressure", "Hegemony", "IAIIC"
                ).setHighlightColors(
                    aloofRep.faction.baseUIColor, aloofRep.faction.baseUIColor, Misc.getHighlightColor(), factionForUIColors.baseUIColor,
                    Misc.getHighlightColor(), factionForUIColors.baseUIColor, Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )

                info.addPara("She listed the following targets: ", 5f)
                info.setBulletedListMode(BULLET)
                info.addPara("Youn", 0f).color = Misc.getHighlightColor()
                info.addPara("Lindunberg", 0f).color = Misc.getHighlightColor()
                info.addPara("Mellour", 0f).color = Misc.getHighlightColor()
                info.addPara("Alotera", 0f).color = Misc.getHighlightColor()
                info.setBulletedListMode(null)

                info.addPara(
                    "These houses can be contacted through the %s comms panel - but only %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "eventide", "one at a time"
                ).setHighlightColors(
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                    Misc.getHighlightColor()
                )

                info.addPara(
                    "%s/%s houses turned",
                    5f,
                    Misc.getHighlightColor(),
                    "$housesTurned", "$HOUSES"
                )

                info.addSectionHeading("Operations", Alignment.MID, 10f)

                if (currentHouse != TargetHouse.NONE) {
                    info.addPara(
                        "Your current target is %s.",
                        5f,
                        factionForUIColors.baseUIColor,
                        currentHouse.stringName
                    )
                }
                info.addSpacer(5f)

                when (currentHouse) {
                    TargetHouse.NONE -> {
                        info.addPara(
                            "You are not currently dealing with %s house.",
                            5f,
                            Misc.getHighlightColor(),
                            "any"
                        )

                        if (cooldownActive) {
                            info.addPara(
                                "INTSEC is watching eventide nobility closely for any signs of defection. It would be unwise to engage any house at the moment.",
                                5f,
                                Misc.getNegativeHighlightColor()
                            )
                        }
                    }
                    TargetHouse.ALOOF -> {
                        val player = Global.getSector().getFaction(Factions.PLAYER)
                        val faction = Global.getSector().getFaction(Factions.HEGEMONY)
                        val hege = faction.baseUIColor

                        val fractalColony = getFractalColony()
                        val fractalSystem = fractalColony?.starSystem!!

                        val IAIICfac = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
                        val IAIIC = IAIICfac.baseUIColor

                        info.addPara(
                            "%s herself has asked for help with convincing her own house to oppose the IAIIC's occupation of your space," +
                            " though for entirely self-serving reasons.",
                            5f,
                            Misc.getHighlightColor(),
                            "Aleratus Youn"
                        )

                        when (aloofState) {
                            null -> {}
                            AloofState.GET_INTEL -> {
                                info.addPara(
                                    "You've been instructed to obtain %s on the hegemony's involvement with the IAIIC, and to return " +
                                    "that intel to %s when you're done. You currently have %s/%s pieces.",
                                    5f,
                                    Misc.getHighlightColor(),
                                    "verifiable intelligence", "Aleratus Youn", evidencePieces.toString(), EVIDENCE_NEEDED.toString()
                                )

                                info.addPara(
                                    "The following targets are likely sources of intelligence:",
                                    5f
                                )
                                val arkFOB = Global.getSector().economy.getMarket("MPC_arkFOB")!!
                                val chico = Global.getSector().economy.getMarket("chicomoztoc")
                                info.setBulletedListMode(BaseIntelPlugin.BULLET)
                                info.addPara("Aztlan Comms Relay", 0f).color = Misc.getHighlightColor()
                                info.addPara("Raiding %s for intel (Difficult, but can give extra intel)", 0f, arkFOB.faction.baseUIColor, arkFOB.name).color = Misc.getHighlightColor()
                                if (chico != null) {
                                    info.addPara("Raiding %s for intel (UNIMPLEMENTED)", 0f, chico.faction.baseUIColor, chico.name).color = Misc.getHighlightColor()
                                }
                                info.addPara("%s comms relay (UNIMPLEMENTED)", 0f, player.baseUIColor, fractalSystem.name).color = Misc.getHighlightColor()
                                //info.addPara("High level %s contact", 0f, hege, "hegemony").color = Misc.getHighlightColor()
                                info.addPara("The hacker, Gargoyle (UNIMPLEMENTED)", 0f).color = Misc.getHighlightColor()
                                //info.addPara("Aztlan Comms Relay", 0f).color = Misc.getHighlightColor()
                                info.setBulletedListMode(null)
                            }

                            AloofState.GOT_EVIDENCE -> {
                                info.addPara(
                                    "You have everything you need. Return to %s on %s.",
                                    5f,
                                    Misc.getHighlightColor(),
                                    "Aleratus Youn",
                                    "Eventide"
                                )
                            }

                            AloofState.WAIT_FOR_ALOOF -> {
                                info.addPara(
                                    "You have given %s your intelligence, and must now wait a %s for her to convince her sister.",
                                    5f,
                                    Misc.getHighlightColor(),
                                    "Aleratus Youn",
                                    "week"
                                )
                            }

                            AloofState.ELIMINATE_TARGET -> TODO()
                            AloofState.ELIMINATE_TARGET_FINISHED -> TODO()
                        }
                    }
                    TargetHouse.OPPORTUNISTIC -> {
                        val faction = Global.getSector().getFaction(Factions.HEGEMONY)
                        val hege = faction.baseUIColor
                        val tritach = Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor

                        val IAIICfac = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
                        val IAIIC = IAIICfac.baseUIColor

                        info.addPara(
                            "The noble house %s of Eventide has expressed \"dismay\" at the involvement the %s has with the %s, " +
                                    "but requires you to obtain data on a \"confidential %s\". More than likely, their pacifism is just a ruse; " +
                                    "but obtaining this data might be the only way to get them on your side.",
                            10f,
                            Misc.getHighlightColor(),
                            "Lindunberg", "hegemony", "IAIIC", "Tri-Tachyon project"
                        ).setHighlightColors(
                            hege, hege, IAIIC, tritach
                        )

                        when (opportunisticState) {
                            OpportunisticState.GO_TO_ALPHA_SITE -> {
                                val hasBeenToAlphaSite = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_playerWentToAlphaSite")
                                if (hasBeenToAlphaSite) {
                                    info.addPara(
                                            "You have been provided a set of hyperspace and ground coordinates, pointing you to a " +
                                                    "specific location on %s likely harboring unharvested data.",
                                    5f,
                                    Misc.getHighlightColor(),
                                    "alpha site",
                                    )
                                } else {
                                    info.addPara(
                                        "You have been provided a set of hyperspace and ground coordinates, pointing you to a " +
                                                "strange hyperspace location nearby %s.",
                                        5f,
                                        Misc.getHighlightColor(),
                                        "Hybrasil",
                                    )
                                }
                            }
                            OpportunisticState.GO_TO_MESON_READINGS -> {
                                info.addPara(
                                    "You've detected a trail of stable antimatter leading to a source of mesons. If you want to find whoever - or whatever - " +
                                    "took the data, you'll need to follow this trail.",
                                    5f
                                )
                            }
                            OpportunisticState.GO_TO_MESON_PLANET -> {
                                info.addPara(
                                    "The meson emissions came from a %s near %s, which seems to connect directly to the %s system. " +
                                    "In order to find the data, it seems you must return to that hellish place and search for a planet meeting two " +
                                    "%s:",
                                    5f,
                                    Misc.getHighlightColor(),
                                    "spatial rift",
                                    "Alpha Site",
                                    "magnetar",
                                    "criteria"
                                )
                                info.setBulletedListMode(BULLET)
                                info.addPara("Has a small gravitational profile", 0f).color = Misc.getHighlightColor()
                                info.addPara("Is surrounded by a meson field", 0f).color = Misc.getHighlightColor()
                                info.setBulletedListMode(null)
                                info.addSpacer(5f)

                                info.addPara(
                                    "Once you're there, you should begin a %s on the planet's surface. Hopefully, if everything goes well, you should find what you're looking for.",
                                    0f,
                                    Misc.getHighlightColor(),
                                    "depth scan",
                                )
                            }
                            null -> {}
                            OpportunisticState.APPROACH_ZIGG_COMPLEX -> {
                                info.addPara(
                                    "You've located a small structure in orbit of %s. You need to get closer and investigate.",
                                    0f,
                                    Misc.getHighlightColor(),
                                    "${getZiggComplexPlanet().name}"
                                )
                            }
                            OpportunisticState.CLEAR_ZIGG_COMPLEX -> {
                                info.addPara(
                                    "The structure in orbit of %s is guarded by a strange fleet. You must defeat them to continue.",
                                    0f,
                                    Misc.getHighlightColor(),
                                    "${getZiggComplexPlanet().name}"
                                )
                            }
                            OpportunisticState.RETURN_WITH_DATA -> {
                                info.addPara(
                                    "You've salvaged a database with a header of \"%s\", and electronics has confirmed it as the target data. " +
                                    "You must now return it to your %s contact on %s - or keep it for yourself, if you wish.",
                                    0f,
                                    Misc.getHighlightColor(),
                                    "Project Ziggurat",
                                    "Lindunberg",
                                    "Eventide"
                                )
                            }
                        }
                    }
                    TargetHouse.MILITARISTIC -> {
                        addStartLabel(info)
                    }
                    TargetHouse.HONORABLE -> TODO()
                }
            }
            State.FAILED -> {
                info.addPara("You have failed to drive a wedge between the IAIIC and the hegemony.", 5f)
            }
            State.DONE -> {
                info.addPara("The %s has been forced to disengage from the %s, delivering a crippling blow to their operations.",
                    5f,
                    Misc.getHighlightColor(),
                    "hegemony", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null

        Global.getSector().economy.getMarket("eventide")?.makeNonStoryCritical("\$MPC_IAIICEvent")
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        if (cooldownActive && state == State.CONVINCE_HOUSES) {
            cooldownDays.advance(Misc.getDays(amount))
            if (cooldownDays.intervalElapsed()) {
                cooldownActive = false
                sendUpdateIfPlayerHasIntel(
                    "COOLDOWN_EXPIRED",
                    false
                )
            }
        }

        if (aloofTimer != null && aloofTimer!! > 0f) {
            aloofTimer = aloofTimer!! - Misc.getDays(amount)
            if (aloofTimer!! <= 0f) {
                aloofTimer = 0f
                aloofTimerExpired()
            }
        }
    }

    private fun aloofTimerExpired() {
        //aloofState = MPC_hegemonyContributionIntel.AloofState.ALOOF_TIME_EXPIRED
        //sendUpdateIfPlayerHasIntel(aloofState, null)

        if (!Global.getSector().campaignUI.showInteractionDialog(
            RuleBasedInteractionDialogPluginImpl(
                "MPC_IAIICaloofPanicComms"
            ),
                Global.getSector().playerFleet
        )) {
            panicTime() // sanity
        }
    }

    fun panicTime(text: TextPanelAPI? = null) {
        aloofState = MPC_hegemonyContributionIntel.AloofState.ELIMINATE_TARGET
        sendUpdateIfPlayerHasIntel(aloofState, text)
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        return when (state) {
            State.GO_TO_EVENTIDE_INIT -> Global.getSector().economy.getMarket("eventide").primaryEntity
            State.CONVINCE_HOUSES -> {
                when (currentHouse) {
                    TargetHouse.NONE -> null
                    TargetHouse.ALOOF -> null
                    TargetHouse.OPPORTUNISTIC -> {

                        when (opportunisticState) {
                            OpportunisticState.GO_TO_ALPHA_SITE -> getAlphaSite().hyperspaceAnchor
                            OpportunisticState.GO_TO_MESON_READINGS -> getAlphaSite().getCustomEntitiesWithTag("MPC_riftRemnant").firstOrNull()
                            OpportunisticState.GO_TO_MESON_PLANET -> {
                                val system = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as? StarSystemAPI ?: return null
                                return system.hyperspaceAnchor
                            }
                            OpportunisticState.APPROACH_ZIGG_COMPLEX -> return getZiggComplexPlanet()
                            OpportunisticState.CLEAR_ZIGG_COMPLEX -> return getZiggComplexPlanet()
                            OpportunisticState.RETURN_WITH_DATA -> Global.getSector().economy.getMarket("eventide").primaryEntity
                            null -> null
                        }
                    }
                    TargetHouse.MILITARISTIC -> null
                    TargetHouse.HONORABLE -> null
                }
            }
            State.DONE -> null
            State.FAILED -> null
        }
    }

    fun turnedHouse(text: TextPanelAPI?) {
        housesTurned++

        if (text == null) {
            sendUpdateIfPlayerHasIntel(
                "HOUSES_TURNED",
                false
            )
        } else {
            sendUpdateIfPlayerHasIntel(
                "HOUSES_TURNED",
                text
            )
        }

        if (housesTurned == 2) {
            Global.getSector().memoryWithoutUpdate.set("\$MPC_IAIICHegeCOMSECIntercepting", true, 0f)
        } else if (housesTurned == 3) {
            text?.addPara("Suddenly, MilSec lights up with alarming reports of IAIIC activity - it seems INTSEC is not happy about your attempts to sabotage their efforts.")
            retaliate(text)
        }

        currentHouse = TargetHouse.NONE
        cooldownActive = true
    }

}