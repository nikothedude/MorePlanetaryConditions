package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.BASE_X_COORD_FOR_SYSTEM
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.BASE_Y_COORD_FOR_SYSTEM
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.X_COORD_VARIATION_LOWER_BOUND
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.X_COORD_VARIATION_UPPER_BOUND
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.Y_COORD_VARIATION_LOWER_BOUND
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript.Companion.Y_COORD_VARIATION_UPPER_BOUND
import data.utilities.niko_MPC_ids
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import org.lazywizard.console.BaseCommand
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getStarSystemForAnchor
import java.awt.Color

class niko_MPC_genericCommand: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        /*val playerPerson = Global.getSector().playerPerson

        val disable = args.toBoolean()
        var numToUse = if (disable) 0f else 1f

        playerPerson.stats.setSkillLevel("captains_academician", numToUse)
        playerPerson.stats.setSkillLevel("captains_unbound", numToUse)
        playerPerson.stats.setSkillLevel("captains_usurper", numToUse)*/

        /*for (fleet in Global.getSector().playerFleet.containingLocation.fleets.toList()) {
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
        }*/

        /*val renderStartTwo = renderEndOne + 0f
        val renderEndTwo = renderEndOne + 2000f
        val effectMiddleDistTwo = 1000f
        val effectSizeBothWaysTwo = 1000f
        val paramsTwo = MagneticFieldParams(
            effectSizeBothWaysTwo,  // terrain effect band width
            effectMiddleDistTwo,  // terrain effect middle radius
            magnetar,  // entity that it's around
            renderStartTwo,  // visual band start
            renderEndTwo,  // visual band end
            Color(50, 20, 100, 50),  // base color
            1f,  // probability to spawn aurora sequence, checked once/day when no aurora in progress
            Color(50, 20, 110, 130),
            Color(150, 30, 120, 150),
            Color(200, 50, 130, 190),
            Color(250, 70, 150, 240),
            Color(200, 80, 130, 255),
            Color(75, 0, 160),
            Color(127, 0, 255)
        )
        val magfieldTwo = testSystem.addTerrain(Terrain.MAGNETIC_FIELD, paramsTwo)*/


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