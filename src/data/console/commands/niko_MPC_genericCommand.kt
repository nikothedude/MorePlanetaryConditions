package data.console.commands

import com.fs.starfarer.api.Global
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class niko_MPC_genericCommand: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        /*val playerLoc = Global.getSector().playerFleet.starSystem
        if (playerLoc == null) {
            Console.showMessage("failure - null loc")
            return BaseCommand.CommandResult.ERROR
        }

        for (planet in playerLoc.planets) {
            if (planet.isStar) continue

            val baseIndex = (niko_MPC_mesonField.baseColors.size * StarSystemGenerator.random.nextDouble()).toInt()
            val auroraIndex = (niko_MPC_mesonField.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()


            /*var visStartRadius = (planet.radius * 1.5f)
            var visEndRadius = planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET
            var bandWidth = 180f // approx the size of the band
            var midRadius = (planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET) / 1.5f*/

            var visStartRadius = (planet.radius * 1.5f)
            var visEndRadius = visStartRadius + WIDTH_PLANET
            var bandWidth = (visEndRadius - visStartRadius) * 0.6f
            var midRadius = (visStartRadius + visEndRadius) / 2
//		float visStartRadius = parent.getRadius() + 50f;
//		float visEndRadius = parent.getRadius() + 50f + WIDTH_PLANET + 50f;
            //		float visStartRadius = parent.getRadius() + 50f;
//		float visEndRadius = parent.getRadius() + 50f + WIDTH_PLANET + 50f;
            var auroraProbability = 1f

            val params = niko_MPC_mesonField.mesonFieldParams(
                bandWidth,
                midRadius,
                planet,
                visStartRadius,
                visEndRadius,
                niko_MPC_mesonField.baseColors[baseIndex],
                auroraProbability,
                niko_MPC_mesonField.auroraColors[auroraIndex].toList(),
            )
            val mesonField = playerLoc.addTerrain("MPC_mesonField", params)
            mesonField.setCircularOrbit(planet, 0f, 0f, 100f)
            break
        }*/
        //createBattle()

        Console.showMessage("success")
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