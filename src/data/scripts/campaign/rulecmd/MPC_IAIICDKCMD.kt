package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_IAIICDKFuelHubFleetSpawner
import data.scripts.campaign.magnetar.crisis.intel.MPC_DKContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.hasUnexploredRuins
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import kotlin.math.abs

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
            "beginSearch" -> {
                val intel = MPC_DKContributionIntel.get(true) ?: return false
                intel.sendUpdateIfPlayerHasIntel(MPC_DKContributionIntel.State.FIND_CORE, dialog.textPanel)
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
        }

        return false
    }
}