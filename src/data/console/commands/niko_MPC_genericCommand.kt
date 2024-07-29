package data.console.commands

import com.fs.starfarer.api.Global
import data.scripts.campaign.magnetar.niko_MPC_derelictOmegaFleetConstructor
import data.scripts.campaign.magnetar.niko_MPC_derelictOmegaFleetConstructor.createFleet
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils

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

        val fp = args.toFloat()

        val playerFleet = Global.getSector().playerFleet ?: return BaseCommand.CommandResult.ERROR
        val distFromStar = playerFleet.starSystem.star?.let { MathUtils.getDistance(playerFleet, it).toString() } ?: "N/A"
        val containingLocaiton = playerFleet.containingLocation ?: return BaseCommand.CommandResult.ERROR
        val fleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(createFleet(fp, null))
        containingLocaiton.addEntity(fleet)
        fleet.location.set(playerFleet.location.x, playerFleet.location.y)

        Console.showMessage("X:${playerFleet.location.x} Y:${playerFleet.location.y}")
        Console.showMessage("Dist from star: $distFromStar")

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