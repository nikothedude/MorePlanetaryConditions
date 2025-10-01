package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.ui.intnew
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_patherContributionIntel
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import org.magiclib.kotlin.getStationIndustry
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class MPC_IAIICPatherCMD: BaseCommandPlugin() {

    companion object {
        fun generateHideout(): SectorEntityToken? {
            if (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] != null) return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as SectorEntityToken

            var backupTarget: PlanetAPI? = null
            var target: PlanetAPI? = null

            for (system in Global.getSector().starSystems) {
                if (system.hasTag(Tags.THEME_SPECIAL) || system.hasTag(Tags.THEME_UNSAFE) || system.hasTag(Tags.SYSTEM_ABYSSAL) || system.hasTag(Tags.THEME_HIDDEN) || system.hasTag(Tags.THEME_CORE)) continue
                if (!system.isProcgen) continue
                if (abs(system.location.x) > HYPERSPACE_MAX_DIST || abs(system.location.y) > HYPERSPACE_MAX_DIST_Y) continue
                if (system.hasBlackHole() || system.hasPulsar()) continue

                for (planet in system.planets) {
                    val market = planet.market ?: continue
                    if (market.isInhabited()) continue
                    backupTarget = planet
                    if (market.hasCondition("pre_collapse_facility")) continue
                    if (market.surveyLevel == MarketAPI.SurveyLevel.FULL) continue
                    if (market.hasCondition(Conditions.HABITABLE)) continue
                    if (market.hazardValue > MIN_HAZARD) continue
                    target = planet
                    break
                }
            }
            if (target == null) target = backupTarget
            if (target == null) return null

            target.makeImportant("\$MPC_IAIICPatherHideout")

            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] = target
            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideoutName"] = target.name
            return target
        }

        fun marketSuitableForTransfer(market: MarketAPI): Boolean {
            //if (market.hasCondition(Conditions.HABITABLE)) return false // the nanoforge would defile this
            if (market.industries.any { it.aiCoreId != null }) return false
            if (market.admin.isAICore) return false
            if (market.stabilityValue < MIN_STABILITY) return false
            if (market.industries.any { it.isDisrupted }) return false
            if (!market.hasIndustry(Industries.ORBITALWORKS)) return false
            if (!market.hasIndustry(Industries.HEAVYBATTERIES)) return false
            if (!market.hasIndustry(Industries.MEGAPORT)) return false
            if (!market.hasIndustry(Industries.MILITARYBASE) && !market.hasIndustry(Industries.HIGHCOMMAND)) return false
            if (market.getStationIndustry()?.spec?.hasTag(Industries.TAG_BATTLESTATION) != true && market.getStationIndustry()?.spec?.hasTag(Industries.TAG_STARFORTRESS) != true) return false
            if (market.getIndustry(Industries.ORBITALWORKS).specialItem == null &&
                market.getIndustry(Industries.HEAVYBATTERIES).specialItem == null &&
                market.getIndustry(Industries.MEGAPORT).specialItem == null &&
                (market.getIndustry(Industries.MILITARYBASE) != null && market.getIndustry(Industries.MILITARYBASE).specialItem == null) &&
                (market.getIndustry(Industries.HIGHCOMMAND) != null && market.getIndustry(Industries.HIGHCOMMAND).specialItem == null)
            ) return false

            if (market.containingLocation == null) return false
            if (market.containingLocation?.getCustomEntitiesWithTag(Tags.COMM_RELAY)?.isNotEmpty() != true) return false
            if (market.containingLocation.hasTag(Tags.THEME_UNSAFE)) return false
            if (market.starSystem.hasBlackHole() || market.starSystem.hasPulsar()) return false

            return true
        }

        const val MIN_HAZARD = 1.5f

        const val HYPERSPACE_MAX_DIST = 25000f
        const val HYPERSPACE_MAX_DIST_Y = 15000f
        const val MIN_STABILITY = 7f

        //public static final float SIZE_FRACTION_FOR_VICTORY = 0.501f;
        //public static final float HI_FRACTION_FOR_VICTORY = 0.67f;
        val POSTS_TO_CHANGE_ON_CAPTURE = Arrays.asList(
            *arrayOf(
                Ranks.POST_BASE_COMMANDER,
                Ranks.POST_OUTPOST_COMMANDER,
                Ranks.POST_STATION_COMMANDER,
                Ranks.POST_PORTMASTER,
                Ranks.POST_SUPPLY_OFFICER,
                Ranks.POST_ADMINISTRATOR
            )
        )
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val interactionTarget = dialog.interactionTarget
        val market = interactionTarget.market ?: return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "canCreateFirstBarEvent" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.LUDDIC_PATH }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) return false
                if (!market.isFractalMarket()) return false
                return true
            }
            "canCreateSecondBarEvent" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.LUDDIC_PATH }) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) return false
                if (market.faction.id != Factions.LUDDIC_PATH) return false
                return true
            }
            "createIntel" -> {
                val intel = MPC_patherContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel("Rumors of involvement", dialog.textPanel)
            }

            "startGoToIthaca" -> {
                val intel = MPC_patherContributionIntel.get(true)
                generateHideout()
                intel?.state = MPC_patherContributionIntel.State.GO_TO_HIDEOUT
                intel?.sendUpdateIfPlayerHasIntel(MPC_patherContributionIntel.State.GO_TO_HIDEOUT, dialog.textPanel)
            }

            "initialHideoutLanding" -> {
                val planet = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI ?: return false
                if (interactionTarget != planet) return false
                val intel = MPC_patherContributionIntel.get() ?: return false
                if (intel.state != MPC_patherContributionIntel.State.GO_TO_HIDEOUT) return false
                return true
            }
            "ambush" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
                Global.getSoundPlayer().pauseMusic()
                Global.getSoundPlayer().playCustomMusic(1, 1, "music_luddite_encounter_neutral", true)
            }
            "endAmbush" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
                Global.getSoundPlayer().playCustomMusic(1, 1, null)
            }

            "beginBuildObj" -> {
                val intel = MPC_patherContributionIntel.get() ?: return false
                intel.state = MPC_patherContributionIntel.State.HAND_OVER_MARKET
                intel.sendUpdateIfPlayerHasIntel(MPC_patherContributionIntel.State.HAND_OVER_MARKET, dialog.textPanel)
                interactionTarget.market?.addCondition("MPC_arrowPatherCondition")

                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarketName"] = interactionTarget.market.name
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarket"] = interactionTarget.market.id
            }
            "setTarget" -> {
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarketName"] = interactionTarget.market.name
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarket"] = interactionTarget.market.id
            }
            "targetReady" -> {
                val target = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarket"] as? String ?: return false
                val market = Global.getSector().economy.getMarket(target)
                return marketSuitableForTransfer(market)
            }

            "canVisitHideoutAgain" -> {
                val planet = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI ?: return false
                if (interactionTarget != planet) return false
                val intel = MPC_patherContributionIntel.get() ?: return false
                return intel.state == MPC_patherContributionIntel.State.HAND_OVER_MARKET
            }

            "raidFinished" -> {
                val intel = MPC_patherContributionIntel.get() ?: return false
                intel.state = MPC_patherContributionIntel.State.FAILED
                intel.sendUpdateIfPlayerHasIntel(MPC_patherContributionIntel.State.FAILED, dialog.textPanel)
                intel.endAfterDelay()
                interactionTarget.market?.removeCondition("MPC_arrowPatherCondition")
            }
            "isDangerous" -> {
                return interactionTarget.starSystem?.hasTag(Tags.THEME_UNSAFE) == true || (interactionTarget.starSystem.hasPulsar() || interactionTarget.starSystem.hasBlackHole())
            }
            "canAddDedicateOption" -> {
                val intel = MPC_patherContributionIntel.get() ?: return false
                if (intel.state != MPC_patherContributionIntel.State.HAND_OVER_MARKET) return false
                return interactionTarget.market?.factionId == Factions.PLAYER
            }

            "donateColony" -> {
                (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI)?.market?.removeCondition("MPC_arrowPatherCondition")
                (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI)?.makeUnimportant("\$MPC_IAIICPatherHideout")

                val target = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPTargetMarket"] as? String ?: return false
                val targetMarket = Global.getSector().economy.getMarket(target) ?: return false
                targetMarket.isPlayerOwned = false
                targetMarket.admin = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson()
                targetMarket.admin.stats.setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1f)
                targetMarket.commDirectory.addPerson(targetMarket.admin)
                targetMarket.admin.postId = Ranks.POST_ADMINISTRATOR

                // Use comm board people instead of market people,
                // because some appear on the former but not the latter
                // (specifically when a new market admin is assigned, old one disappears from the market)
                // Also, this way it won't mess with player-assigned admins
                for (dir in targetMarket.commDirectory.entriesCopy) {
                    if (dir.type != CommDirectoryEntryAPI.EntryType.PERSON) continue
                    val person = dir.entryData as PersonAPI
                    // TODO should probably switch them out completely instead of making them defect
                    if (POSTS_TO_CHANGE_ON_CAPTURE.contains(person.postId)) {
                        person.setFaction(Factions.LUDDIC_PATH)
                        person.voice = Voices.PATHER
                    }
                }

                // transfer defense station

                // transfer defense station
                if (Misc.getStationFleet(targetMarket) != null) {
                    Misc.getStationFleet(targetMarket).setFaction(Factions.LUDDIC_PATH, true)
                }
                if (Misc.getStationBaseFleet(targetMarket) != null) {
                    Misc.getStationBaseFleet(targetMarket).setFaction(Factions.LUDDIC_PATH, true)
                }

                // hack for "our interaction target is a station and we just sold it" case
                if (Entities.STATION_BUILT_FROM_INDUSTRY == interactionTarget.customEntityType) {
                    interactionTarget.setFaction(Factions.LUDDIC_PATH)
                }
                (targetMarket.getSubmarket(Submarkets.SUBMARKET_STORAGE)?.plugin as? StoragePlugin)?.setPlayerPaidToUnlock(true)
                targetMarket.connectedEntities.forEach { it.setFaction(Factions.LUDDIC_PATH) }

                targetMarket.size = max(targetMarket.size, 4)
                targetMarket.removeCondition(Conditions.POPULATION_3)
                targetMarket.addCondition(Conditions.POPULATION_4)

                /*if (niko_MPC_settings.AOTD_vaultsEnabled) {
                    targetMarket.constructionQueue.addToEnd("militarygarrison", 0)
                }*/
                targetMarket.constructionQueue.addToEnd(Industries.WAYSTATION, 0)
                targetMarket.getIndustry(Industries.MILITARYBASE)?.startUpgrading()
                targetMarket.getIndustry(Industries.MILITARYBASE)?.isImproved = true
                targetMarket.getIndustry(Industries.HIGHCOMMAND)?.isImproved = true
                targetMarket.addSubmarket(Submarkets.SUBMARKET_OPEN)
                targetMarket.addSubmarket(Submarkets.SUBMARKET_BLACK)
                //targetMarket.addCondition(Conditions.LUDDIC_MAJORITY)
                targetMarket.addCondition("MPC_arrowPatherConditionTwo")

                RecentUnrest.get(targetMarket, true)?.add(4, "Transfer to the Luddic Path")

                targetMarket.reapplyConditions()
                targetMarket.reapplyIndustries()


                (targetMarket.primaryEntity as? PlanetAPI?)?.descriptionIdOverride = "MPC_arrowPatherPlanet"

                val intel = MPC_patherContributionIntel.get(true) ?: return false
                intel.state = MPC_patherContributionIntel.State.DONE
                intel.sendUpdateIfPlayerHasIntel(MPC_patherContributionIntel.State.DONE, dialog.textPanel)
                intel.endAfterDelay()

                Global.getSoundPlayer().restartCurrentMusic()
            }
            "pullOut" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                val toRemove = intel.getContributionById(Factions.LUDDIC_PATH) ?: return false
                intel.removeContribution(toRemove, false, dialog)
            }
        }

        return false
    }
}