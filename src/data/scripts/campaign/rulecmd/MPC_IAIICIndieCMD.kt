package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.StatBonus
import com.fs.starfarer.api.impl.campaign.DebugFlags
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.TransmitterTrapSpecial.makeFleetInterceptPlayer
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeScript
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.MPC_indieUnrestScript
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContribution
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_indieContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.support.MPC_tactistarFractalSupport
import data.scripts.utils.SotfMisc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.IAIIC_QUEST
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import org.lwjgl.input.Keyboard
import org.magiclib.kotlin.*
import java.awt.Color
import kotlin.math.roundToInt

class MPC_IAIICIndieCMD: BaseCommandPlugin() {

    companion object {
        fun getNewVoidsunRelaySystem(): StarSystemAPI? {
            val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return null
            val fractalSys = fractalColony.starSystem

            val allSys = Global.getSector().starSystems - fractalSys
            val nearbySys = allSys.filter { sys -> !sys.hasTag(Tags.THEME_CORE) && sys.customEntities.any { it.hasTag(Tags.STABLE_LOCATION) } && Misc.getDistanceLY(fractalSys.location, sys.location) <= 10f }
            if (nearbySys.isNotEmpty()) {
                val nearbyUnexploredSys = nearbySys.filter { !it.isEnteredByPlayer }
                if (nearbyUnexploredSys.isNotEmpty()) {
                    return nearbyUnexploredSys.maxBy { Misc.getDistanceLY(fractalSys.location, it.location) }
                }
                return nearbySys.maxBy { Misc.getDistanceLY(fractalSys.location, it.location) }
            }
            return allSys.filter { sys -> !sys.hasTag(Tags.THEME_CORE) && sys.customEntities.any { it.hasTag(Tags.STABLE_LOCATION) } }.randomOrNull()
        }

        fun getBlackknifeTargetPlanet(): SectorEntityToken {
            val validFirstPass: SectorEntityToken = (Global.getSector().starSystems.filter {
                        !it.hasTag(Tags.THEME_CORE) &&
                        !it.hasTag(Tags.THEME_UNSAFE) &&
                        !it.hasTag(Tags.THEME_SPECIAL) &&
                        !it.hasTag(Tags.THEME_HIDDEN) &&
                        !it.hasTag(Tags.SYSTEM_ABYSSAL) &&
                        it.planets.any { it.market?.hasUnexploredRuins() == true }
            }.randomOrNull()?.planets?.firstOrNull { it.market?.hasUnexploredRuins() == true } ?: Global.getSector().economy.getMarket("qaras").primaryEntity)
            return validFirstPass
        }

        fun generateBlackknifeTargetFleet(): CampaignFleetAPI {
            val params = FleetParamsV3(
                null,
                Factions.PERSEAN,
                1f,
                FleetTypes.MERC_ARMADA,
                300f,
                40f,
                30f,
                5f,
                0f,
                5f,
                1.2f
            )
            params.averageSMods = 2
            params.officerLevelLimit = 7
            params.officerLevelBonus = 2

            val fleet = FleetFactoryV3.createFleet(params)
            fleet.commander = Global.getSector().importantPeople.getPerson(MPC_People.BLACKKNIFE_TARGET)
            fleet.flagship.captain = Global.getSector().importantPeople.getPerson(MPC_People.BLACKKNIFE_TARGET)
            fleet.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            fleet.name = "Expeditionary Fleet"

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
        val market = interactionTarget.market

        val command = params[0].getString(memoryMap)

        val contribIntel = MPC_indieContributionIntel.get() ?: return false
        val fobIntel = MPC_IAIICFobIntel.get() ?: return false

        when (command) {
            "hammerEnd" -> {
                val nuked = params[1].getBoolean(memoryMap)
                val contrib = fobIntel.getContributionById("thehammer")
                contrib?.let { fobIntel.removeContribution(it, nuked, dialog) }
                if (niko_MPC_settings.SOTF_enabled && nuked) {
                    SotfMisc.addGuilt(0.5f, 10f)
                    dialog.textPanel.setFontSmallInsignia()
                    dialog.textPanel.addPara(
                        "Received a guilty conscious",
                        Misc.getNegativeHighlightColor()
                    )
                    val baetis = Global.getSector().economy.getMarket("baetis")
                    if (baetis != null) {
                        RecentUnrest.get(baetis).add(
                            3,
                            "Horrific Tragedy Occurred"
                        )
                    }
                    Misc.increaseMarketHostileTimeout(baetis, 30f)
                    val script = MilitaryResponseScript(
                        MilitaryResponseScript.MilitaryResponseParams(
                            CampaignFleetAIAPI.ActionType.HOSTILE,
                            "MPC_IAIICBaetisBombarded",
                            baetis.faction,
                            baetis.primaryEntity,
                            0.2f,
                            30f
                        )
                    )
                    baetis.containingLocation.addScript(script)
                    baetis.primaryEntity.makeUnimportant(IAIIC_QUEST)
                    dialog.textPanel.setFontInsignia()
                }
            }
            "hammerGoAway" -> {
                val contrib = fobIntel.getContributionById("thehammer")
                contrib?.custom = "TOLD_TO_GO_AWAY"
                contribIntel.sendUpdate("Wipe out the mercenaries", dialog.textPanel)
            }
            "hammerIsFaithful" -> {
                return (Global.getSector().playerMemoryWithoutUpdate.getFloat("\$luddicAttitudePather") >= 3f ||
                        Global.getSector().playerMemoryWithoutUpdate.getFloat("\$luddicAttitudeFaithful") >= 3f
                )
            }
            "hammerIsRuthless" -> {
                return SotfMisc.getPlayerGuilt() >= 4f || (Global.getSector().playerMemoryWithoutUpdate.getFloat("\$ethosRuthless") >= 3f)
            }
            "hammerBombardMenu" -> {
                val playerFleet = Global.getSector().playerFleet
                val width = 350f
                val opad = 10f
                val small = 5f

                val h = Misc.getHighlightColor()
                Misc.getNegativeHighlightColor()

                dialog.visualPanel.showImagePortion("illustrations", "bombard_prepare", 640f, 400f, 0f, 0f, 480f, 300f)

                val defender = market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD)

                val bomardBonus = Misc.getFleetwideTotalMod(playerFleet, Stats.FLEET_BOMBARD_COST_REDUCTION, 0f)
                val increasedBombardKey = "core_addedBombard"
                val bombardBonusStat = StatBonus()
                if (bomardBonus > 0) {
                    bombardBonusStat.modifyFlat(
                        increasedBombardKey,
                        -bomardBonus,
                        "Specialized fleet bombardment capability"
                    )
                }

                var defenderStr = defender.computeEffective(0f).roundToInt().toFloat()
                defenderStr -= bomardBonus
                if (defenderStr < 0) defenderStr = 0f

                //temp.defenderStr = defenderStr

                val text = dialog.textPanel
                val faction = interactionTarget.market.faction
                val info: TooltipMakerAPI = text.beginTooltip()

                info.setParaSmallInsignia()

                val `is`: String? = faction.displayNameIsOrAre
                val hostile: Boolean = faction.isHostileTo(Factions.PLAYER)
                var initPad = 0f
                if (!hostile) {
                    info.addPara(
                        Misc.ucFirst(faction.displayNameWithArticle) + " " + `is` +
                                " not currently hostile. While bombardments typically cannot be hidden, this specific circumstance " +
                                "can be concealed using creative propaganda or misinformation tactics - though relations will still be significant strained.",
                        initPad, faction.baseUIColor, faction.displayNameWithArticleWithoutArticle
                    )
                    initPad = opad
                }

                info.addPara(
                    "As you've previously observed, the arcology below is fragile and lacks significant vaccum protection " +
                        "in the event of a breach. Should a stray asteroid or fuel tank smash into it, casualties would likely near 99%... " +
                        "including it's citizenship. It would be trivial to hide your own involvement - but you're not sure if this is the ethical choice, " +
                        "or even the practical one.", initPad
                )

                dialog.textPanel.setFontSmallInsignia()
                dialog.textPanel.addPara(
                    "Will cause major reputation loss with the independents, a smaller loss with the luddic church, but not enough to provoke war",
                    Misc.getNegativeHighlightColor()
                )
                dialog.textPanel.setFontInsignia()


                if (bomardBonus > 0) {
                    info.addPara("Effective ground defense strength: %s", opad, h, "" + defenderStr.toInt())
                } else {
                    info.addPara("Ground defense strength: %s", opad, h, "" + defenderStr.toInt())
                }
                info.addStatModGrid(width, 50f, opad, small, defender, true, MarketCMD.statPrinter(true))
                if (!bombardBonusStat.isUnmodified) {
                    info.addStatModGrid(width, 50f, opad, 3f, bombardBonusStat, true, MarketCMD.statPrinter(false))
                }

                text.addTooltip()


                //		text.addPara("A tactical bombardment will only hit military targets and costs less fuel. A saturation " +
//				"bombardment will devastate the whole colony, and only costs marginally more fuel, as the non-military " +
//				"targets don't have nearly the same degree of hardening.");
                val bombardCost = MarketCMD.getBombardmentCost(market, playerFleet)

                val fuel = playerFleet.getCargo().getFuel().toInt()
                var canBombard = fuel >= bombardCost

                val b = Misc.getNegativeHighlightColor()

                val label = text.addPara(
                    "A bombardment requires %s fuel. " +
                            "You have %s fuel.",
                    h, "" + bombardCost, "" + fuel
                )
                label.setHighlight("" + bombardCost, "" + fuel)
                label.setHighlightColors(if (canBombard) h else b, h)

                val options = dialog.optionPanel
                options.clearOptions()

                options.addOption("Launch the targeted bombardment", "MPC_IAIICHammerMercBombardExecute")

                if (DebugFlags.MARKET_HOSTILITIES_DEBUG) {
                    canBombard = true
                }
                if (!canBombard) {
                    options.setEnabled("MPC_IAIICHammerMercBombardExecute", false)
                    options.setTooltip("MPC_IAIICHammerMercBombardExecute", "Not enough fuel.")
                } else {
                    options.addOptionConfirmation(
                        "MPC_IAIICHammerMercBombardExecute",
                        "A crime of such magnitude cannot be fully hidden and will provoke major reputation loss - as well " +
                                "as a persistent sense of guilt.",
                        "Do it",
                        "Abort"
                    )
                }

                options.addOption("Go back", MarketCMD.RAID_GO_BACK)
                options.setShortcut(MarketCMD.RAID_GO_BACK, Keyboard.KEY_ESCAPE, false, false, false, true)
            }

            "blackknifeGiveTask" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                blackknifeContrib?.custom = "GAVE_DEAL"
                contribIntel.sendUpdate("blackknifeGivenDeal", dialog.textPanel)
            }
            "blackknifeEnd" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                blackknifeContrib?.let { fobIntel.removeContribution(it, false) }
            }

            "tactistarGenPrice" -> {
                MPC_indieContributionIntel.getTactistarExitPrice()
                MPC_indieContributionIntel.getTactistarJoinPrice()
            }
            "tactistarOfferGiven" -> {
                val tactistarContrib = fobIntel.getContributionById("tactistar")
                tactistarContrib?.custom = "GIVEN_DEAL"
                contribIntel.sendUpdate("Buy out the Tactistar contract", dialog.textPanel)
            }
            "tactistarOver" -> {
                val boughtOut = params[1].getBoolean(memoryMap)
                val tactistarContrib = fobIntel.getContributionById("tactistar")
                tactistarContrib?.let { fobIntel.removeContribution(it, false, dialog) }
                val culann = Global.getSector().economy.getMarket("culann")
                culann?.makeNonStoryCritical(niko_MPC_ids.IAIIC_QUEST)
                culann?.commDirectory?.removePerson(Global.getSector().importantPeople.getPerson(MPC_People.TACTISTAR_REP))

                if (boughtOut) {
                    Global.getSector().intelManager.addIntel(MPC_tactistarFractalSupport(), false, dialog.textPanel)
                }
            }

            "ailmarOfferGiven" -> {
                val ailmarContrib = fobIntel.getContributionById("ailmar")
                ailmarContrib?.custom = "GIVEN_DEAL"
                contribIntel.sendUpdate("Gain Ailmar's loyalty", dialog.textPanel)
            }
            "ailmarOver" -> {
                val type = params[1].getString(memoryMap)

                val ailmarContrib = fobIntel.getContributionById("ailmar")
                ailmarContrib?.let { fobIntel.removeContribution(it, false, dialog) }
                val ailmar = Global.getSector().economy.getMarket("ailmar")
                ailmar?.makeNonStoryCritical(niko_MPC_ids.IAIIC_QUEST)
                ailmar?.admin?.makeUnimportant(IAIIC_QUEST)

                if (type == "FLEETSIZE") {
                    MPC_ailmarFleetsizeScript().start()
                }
            }
            "alimarCanUseRep" -> {
                val playerFac = Global.getSector().playerFaction
                return playerFac.getRelationshipLevel(Factions.PERSEAN) == RepLevel.COOPERATIVE || playerFac.getRelationshipLevel(Factions.PERSEAN) == RepLevel.FRIENDLY
            }

            "blackknifeFoundCommsKey" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                val intel = MPC_indieContributionIntel.get(false) ?: return false
                val qaras = Global.getSector().economy.getMarket("qaras") ?: return false

                blackknifeContrib?.custom = "GOT_COMMS_KEY"
                intel.sendUpdate("Contact Jango Retrina", dialog.textPanel)

                qaras.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.BLACKKNIFE_REP))
            }
            "blackknifeGenTargetFleet" -> {
                val target = getBlackknifeTargetPlanet()
                val sys = target.starSystem ?: return false

                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetPlanet"] = target
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetPlanetName"] = target.name
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetSys"] = sys
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetSysName"] = sys.name

                val fleet = generateBlackknifeTargetFleet()
                sys.addEntity(fleet)
                fleet.setLocation(target.location.x, target.location.y)
                fleet.clearAssignments()
                fleet.addAssignment(
                    FleetAssignment.ORBIT_PASSIVE,
                    target,
                    Float.MAX_VALUE,
                    "treasure hunting on ${target.name}"
                )
                fleet.addEventListener(BlackknifeTargetDeathListener())
            }
            "blackknifeGoKillGuy" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                val intel = MPC_indieContributionIntel.get(false) ?: return false
                val qaras = Global.getSector().economy.getMarket("qaras") ?: return false

                blackknifeContrib?.custom = "GO_KILL_GUY"
                intel.sendUpdate("Eliminate the target in ${Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetSysName"]}", dialog.textPanel)

                qaras.commDirectory.removePerson(Global.getSector().importantPeople.getPerson(MPC_People.BLACKKNIFE_REP))
            }

            "MMMCtoldOff" -> {
                val MMMCContrib = fobIntel.getContributionById("mmmc")
                val intel = MPC_indieContributionIntel.get(false) ?: return false

                MMMCContrib?.custom = "TOLD_OFF"
                intel.sendUpdate("Disrupt orbital works for sixty days", dialog.textPanel)
                Global.getSector().listenerManager.addListener(MMMCDisruptListener())
            }
            "MMMCend" -> {
                val MMMCContrib = fobIntel.getContributionById("mmmc") ?: return false
                val intel = MPC_indieContributionIntel.get(false) ?: return false

                fobIntel.removeContribution(MMMCContrib, false, dialog)
                Global.getSector().listenerManager.removeListenerOfClass(MMMCDisruptListener::class.java)
                Global.getSector().economy.getMarket("new_maxios")?.commDirectory?.removePerson(Global.getSector().importantPeople.getPerson(MPC_People.MMMC_REP))
            }
            "voidsunCanGenBarEvent" -> {
                if (!market.isPlayerOwned) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_voidsunCanSpawnNow")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didVoidsunBar")) return false

                return true
            }
            "voidsunGetPlanet" -> {
                val playerFacMarkets = Global.getSector().playerFaction.getFactionMarkets()

                val target = playerFacMarkets.find { it.isMilitary() } ?: playerFacMarkets.random()
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICvoidsunPlanetId"] = target.id
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICvoidsunPlanetName"] = target.name

                target.commDirectory.addPerson(MPC_People.getImportantPeople()[MPC_People.VOIDSUN_REP])
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false

                class VoidsunExistsScript: MPC_factionContribution.ContributorExistsScript(
                    Factions.INDEPENDENT,
                    false
                ) {
                    override fun run(): Boolean {
                        return Global.getSector().economy.getMarket(Global.getSector().memoryWithoutUpdate.getString("\$MPC_IAIICvoidsunPlanetId"))?.isInhabited() ?: return false
                    }
                }

                voidsunContrib.contributorExists = VoidsunExistsScript()
            }
            "voidsunBegin" -> {
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                val intel = MPC_indieContributionIntel.get(false) ?: return false

                intel.sendUpdateIfPlayerHasIntel("voidsunBegin", dialog.textPanel)
                voidsunContrib.addBenefactorInfo = true
            }
            "voidsunHasProof" -> {
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                return voidsunContrib.custom == "GOT_EVIDENCE"
            }
            "voidsunCanAddEvictOption" -> {
                if (market.id != Global.getSector().memoryWithoutUpdate["\$MPC_IAIICvoidsunPlanetId"]) return false
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                return voidsunContrib.addBenefactorInfo
            }
            "voidsunUnlawfullyEvict" -> {
                val market = Global.getSector().economy.getMarket(Global.getSector().memoryWithoutUpdate.getString("\$MPC_IAIICvoidsunPlanetId")) ?: return false
                market.commDirectory.removePerson(MPC_People.getImportantPeople()[MPC_People.VOIDSUN_REP])
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                fobIntel.removeContribution(voidsunContrib, true, dialog, sendMessage = false)

                MPC_indieUnrestScript().start()
            }
            "voidsunNormalArrest" -> {
                val market = Global.getSector().economy.getMarket(Global.getSector().memoryWithoutUpdate.getString("\$MPC_IAIICvoidsunPlanetId")) ?: return false
                market.commDirectory.removePerson(MPC_People.getImportantPeople()[MPC_People.VOIDSUN_REP])

                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                fobIntel.removeContribution(voidsunContrib, true, dialog)
            }

            "voidsunGoToComms" -> {
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                voidsunContrib.custom = "GO_TO_COMMS_RELAY"
                contribIntel.sendUpdateIfPlayerHasIntel(
                    "Investigate comms relay",
                    dialog.textPanel
                )
            }

            "voidsunIsRelayGoodTarget" -> {
                if (!interactionTarget.hasTag(Tags.COMM_RELAY)) return false
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                if (voidsunContrib.custom != "GO_TO_COMMS_RELAY") return false

                return true
            }

            "voidsunGoToRelay" -> {
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                voidsunContrib.custom = "GO_TO_EXTERNAL_RELAY"
                contribIntel.sendUpdateIfPlayerHasIntel(
                    "VS_GO_TO_RELAY_PLACE",
                    dialog.textPanel
                )
            }

            "voidsunGenRelay" -> {
                val system = getNewVoidsunRelaySystem() ?: return false
                val stableLoc = system.getCustomEntitiesWithTag(Tags.STABLE_LOCATION).random() // let it crash here if it fails tbqh
                val orbit = stableLoc.orbit.makeCopy()
                val newEntity = system.addCustomEntity(
                    "MPC_voidsunRelay",
                    null,
                    "MPC_voidsunRelay",
                    Factions.INDEPENDENT
                )
                newEntity.orbit = orbit
                //system.removeEntity(stableLoc)

                Global.getSector().memoryWithoutUpdate["\$MPC_voidsunRelaySysName"] = system.name
                Global.getSector().memoryWithoutUpdate["\$MPC_voidsunRelaySysId"] = system.id

            }

            "voidsunEvidenceGet" -> {
                val voidsunContrib = fobIntel.getContributionById("voidsun") ?: return false
                voidsunContrib.custom = "GOT_EVIDENCE"
                contribIntel.sendUpdateIfPlayerHasIntel(
                    "VS_GOT_EVIDENCE",
                    dialog.textPanel
                )
            }

            "voidsunEXPLODE" -> {
                val targetSysId = Global.getSector().memoryWithoutUpdate.getString("\$MPC_voidsunRelaySysId")
                val targetSys = Global.getSector().getStarSystem(targetSysId) ?: return false

                val relay = targetSys.getCustomEntitiesWithTag("MPC_voidsunRelay").first()

                val color = Color(255, 90, 60)

                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    color,
                    targetSys,
                    relay.location,
                    200f,
                    1f
                )
                explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.MEDIUM
                val explosion = targetSys.addCustomEntity(
                    Misc.genUID(), "Explosion",
                    Entities.EXPLOSION, Factions.NEUTRAL, explosionParams
                )
                explosion.setLocation(relay.location.x, relay.location.y)
                relay.fadeAndExpire()
            }

            "voidsunBeginAmbush" -> {
                val targetSysId = Global.getSector().memoryWithoutUpdate.getString("\$MPC_voidsunRelaySysId")
                val targetSys = Global.getSector().getStarSystem(targetSysId) ?: return false
                val relay = targetSys.getCustomEntitiesWithTag("MPC_voidsunRelay").first()

                val playerFleet = Global.getSector().playerFleet
                val fp = (playerFleet.fleetPoints * 0.9f).coerceAtLeast(200f)
                val params = FleetParamsV3(
                    MPC_fractalCoreFactor.getFOB(),
                    FleetTypes.MERC_ARMADA,
                    fp,
                    40f,
                    30f,
                    5f,
                    0f,
                    5f,
                    1.2f
                )
                params.averageSMods = 2
                params.officerLevelLimit = 7
                params.officerLevelBonus = 2

                val fleet = FleetFactoryV3.createFleet(params)
                fleet.stats.fleetwideMaxBurnMod.modifyFlat("MPC_SPEEEED", 2f)

                //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_REP_IMPACT] = true
                //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE] = true
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PURSUE_PLAYER] = true
                fleet.memoryWithoutUpdate.set("\$genericHail", true)
                fleet.memoryWithoutUpdate.set("\$genericHail_openComms", "MPC_IAIICVoidsunAmbushFleetHail")
                targetSys.addEntity(fleet)
                val spawnDist = 3000f
                val loc = Misc.getPointAtRadius(relay.location, spawnDist)
                fleet.setLocation(loc.x, loc.y)

                Misc.giveStandardReturnToSourceAssignments(fleet)
                makeFleetInterceptPlayer(fleet, true, false, 30f)
            }

            "ibrahimCanAddInitialOpt" -> {
                val ibrahimContrib = fobIntel.getContributionById("agreus") ?: return false
                return ibrahimContrib.custom == null
            }
            "ibrahimKillPatrol" -> {
                val ibrahimContrib = fobIntel.getContributionById("agreus") ?: return false
                ibrahimContrib.custom = "GET_PROOF"
                contribIntel.sendUpdateIfPlayerHasIntel(
                    "Destroy an IAIIC spearfleet",
                    dialog.textPanel
                )

                Global.getSector().addListener(IbrahimFleetDestroyedListener())
            }

            "ibrahimCanAddEndOpt" -> {
                val ibrahimContrib = fobIntel.getContributionById("agreus") ?: return false
                return ibrahimContrib.custom == "GOT_PROOF"
            }

            "ibrahimEnd" -> {
                val ibrahimContrib = fobIntel.getContributionById("agreus") ?: return false
                fobIntel.removeContribution(ibrahimContrib, false, dialog)
                Global.getSector().importantPeople.getPerson(People.IBRAHIM).makeUnimportant(IAIIC_QUEST)

            }

        }

        return false
    }

    class IbrahimFleetDestroyedListener(): BaseCampaignEventListener(false) {
        override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
            super.reportBattleOccurred(primaryWinner, battle)

            if (primaryWinner == null || battle == null) return
            if (!battle.isPlayerInvolved) return

            val playerFleet = Global.getSector().playerFleet
            if (!battle.wasFleetVictorious(playerFleet, primaryWinner)) return
            val playerSide = battle.playerSide
            for (fleet in battle.nonPlayerSideSnapshot) {
                if (fleet.faction.id != niko_MPC_ids.IAIIC_FAC_ID) continue
                if (!fleet.name.contains("Spearfleet")) continue
                val lost = Misc.getSnapshotFPLost(fleet)
                if (lost >= fleet.fleetPoints * 0.8f) {

                    val contribIntel = MPC_indieContributionIntel.get(false) ?: return
                    val fobIntel = MPC_IAIICFobIntel.get() ?: return
                    val contrib = fobIntel.getContributionById("agreus") ?: return
                    contrib.custom = "GOT_PROOF"
                    contribIntel.sendUpdateIfPlayerHasIntel("Return to Agreus with proof", false)
                    Global.getSector().removeListener(this)
                }
            }

        }
    }

    class MMMCDisruptListener: ColonyPlayerHostileActListener {
        override fun reportRaidForValuablesFinishedBeforeCargoShown(
            dialog: InteractionDialogAPI?,
            market: MarketAPI?,
            actionData: MarketCMD.TempData?,
            cargo: CargoAPI?
        ) {
            return
        }

        override fun reportRaidToDisruptFinished(
            dialog: InteractionDialogAPI?,
            market: MarketAPI?,
            actionData: MarketCMD.TempData?,
            industry: Industry?
        ) {
            val intel = MPC_indieContributionIntel.get(false) ?: return Global.getSector().listenerManager.removeListener(this)
            if (market?.id != "new_maxios") return
            if (industry?.spec?.hasTag(Industries.TAG_HEAVYINDUSTRY) != true) return

            if (industry.disruptedDays < 60f) return

            val fobIntel = MPC_IAIICFobIntel.get() ?: return Global.getSector().listenerManager.removeListener(this)
            val MMMCContrib = fobIntel.getContributionById("mmmc")
            MMMCContrib?.custom = "DISRUPTED"
            intel.sendUpdate("Revisit the MMMC", dialog?.textPanel)
            Global.getSector().listenerManager.removeListener(this)
        }

        override fun reportTacticalBombardmentFinished(
            dialog: InteractionDialogAPI?,
            market: MarketAPI?,
            actionData: MarketCMD.TempData?
        ) {
            return
        }

        override fun reportSaturationBombardmentFinished(
            dialog: InteractionDialogAPI?,
            market: MarketAPI?,
            actionData: MarketCMD.TempData?
        ) {
            return
        }

    }

    class BlackknifeTargetDeathListener: FleetEventListener {

        companion object {
            fun blackknifeTargetDeath() {
                val fobIntel = MPC_IAIICFobIntel.get() ?: return
                val blackknifeContrib = fobIntel.getContributionById("blackknife") ?: return
                val intel = MPC_indieContributionIntel.get(false) ?: return
                val qaras = Global.getSector().economy.getMarket("qaras") ?: return

                fobIntel.removeContribution(blackknifeContrib, false, null)

                qaras.removePerson(Global.getSector().importantPeople.getPerson(MPC_People.BLACKKNIFE_REP))
                qaras.makeNonStoryCritical(niko_MPC_ids.IAIIC_QUEST)
                qaras.primaryEntity.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
        }

        override fun reportFleetDespawnedToListener(
            fleet: CampaignFleetAPI,
            reason: CampaignEventListener.FleetDespawnReason?,
            param: Any?
        ) {
            if (reason != CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY) {
                blackknifeTargetDeath()
                fleet.removeEventListener(this)
            }
        }

        override fun reportBattleOccurred(
            fleet: CampaignFleetAPI?,
            primaryWinner: CampaignFleetAPI?,
            battle: BattleAPI?
        ) {
            return
        }
    }
}