package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.ids.Tags.VARIANT_ALWAYS_RECOVERABLE
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_HasBackground
import com.sun.org.apache.bcel.internal.generic.RET
import data.scripts.campaign.econ.conditions.derelictEscort.derelictEscortStates
import data.utilities.niko_MPC_ids
import exerelin.campaign.SectorManager.transferMarket
import exerelin.campaign.intel.colony.ColonyExpeditionIntel.createColonyStatic
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import exerelin.campaign.intel.specialforces.namer.PlanetNamer
import indevo.industries.artillery.utils.ArtilleryStationPlacer.addArtilleryToPlanet
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.magiclib.kotlin.hasFarmland

class niko_MPC_genericCommand: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        /*val playerPerson = Global.getSector().playerPerson

        val disable = args.toBoolean()
        var numToUse = if (disable) 0f else 1f

        playerPerson.stats.setSkillLevel("captains_academician", numToUse)
        playerPerson.stats.setSkillLevel("captains_unbound", numToUse)
        playerPerson.stats.setSkillLevel("captains_usurper", numToUse)*/

        for (fleet in Global.getSector().playerFleet.containingLocation.fleets.toList()) {
            var baseListener: MilitaryBase? = null
            for (listener in fleet.eventListeners) {
                if (listener is MilitaryBase) {
                    baseListener = listener
                    break
                }
            }
            if (baseListener == null) continue

            val route = RouteManager.getInstance().getRoute(baseListener.routeSourceId, fleet)
            if (route == null) {
                fleet.despawn(CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY, null)
            }
        }

        for (fleet in Global.getSector().playerFleet.containingLocation.fleets.toList()) {
            val existingState = fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_STATE_MEMFLAG] as? derelictEscortStates ?: continue
            if (existingState == derelictEscortStates.RETURNING_TO_BASE) {
                fleet.despawn(CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY, null)
            }
        }

        return BaseCommand.CommandResult.SUCCESS
    }

    protected fun createBattle(): GroundBattleIntel {
        val market = Global.getSector().getEntityById("typhoon").market
        val attacker = Global.getSector().getFaction("pirates")
        val battle = GroundBattleIntel(market, attacker, market.faction)
        battle.isEndIfPeace = false
        battle.init()
        val strength = GBUtils.estimateTotalDefenderStrength(battle, true)
        var marines = Math.round(strength * 0.65f)
        val heavies = Math.round(strength * 0.4f / GroundUnit.HEAVY_COUNT_DIVISOR)
        marines += heavies * GroundUnitDef.getUnitDef(GroundUnitDef.HEAVY).personnel.mult
        battle.autoGenerateUnits(marines, heavies, attacker, true, false)
        battle.playerJoinBattle(false, false)
        battle.start()
        battle.runAI(true, false) // deploy starting attacker units
        battle.isImportant = true
        return battle
    }
}