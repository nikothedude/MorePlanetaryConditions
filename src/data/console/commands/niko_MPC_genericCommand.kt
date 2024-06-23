package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.ids.Tags.VARIANT_ALWAYS_RECOVERABLE
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_HasBackground
import exerelin.campaign.SectorManager.transferMarket
import exerelin.campaign.intel.colony.ColonyExpeditionIntel.createColonyStatic
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import indevo.industries.artillery.utils.ArtilleryStationPlacer.addArtilleryToPlanet
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.magiclib.kotlin.hasFarmland

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

        /*val fortExtradol = Global.getSector().getEntityById("exsedol_station") ?: return BaseCommand.CommandResult.ERROR
        val armaaFac = fortExtradol.faction
        fortExtradol.market.getIndustry(Industries.HIGHCOMMAND).isImproved = true
        fortExtradol.market.getIndustry(Industries.HIGHCOMMAND).aiCoreId = Commodities.ALPHA_CORE

        fortExtradol.market.getIndustry(Industries.HEAVYBATTERIES).isImproved = true
        fortExtradol.market.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE
        fortExtradol.market.getIndustry(Industries.HEAVYBATTERIES).specialItem = SpecialItemData(Items.DRONE_REPLICATOR, null)
        fortExtradol.market.getIndustry(Industries.HEAVYINDUSTRY).isImproved = true
        fortExtradol.market.removeIndustry(Industries.SPACEPORT, null, false)
        fortExtradol.market.addIndustry(Industries.MEGAPORT)

        fortExtradol.market.getIndustry(Industries.STARFORTRESS_MID).aiCoreId = Commodities.ALPHA_CORE

        val jenius = Global.getSector().getEntityById("nekki1") as? PlanetAPI ?: return BaseCommand.CommandResult.ERROR
        createColonyStatic(jenius.market, jenius, armaaFac, false, false)
        jenius.market.size = 4
        if (jenius.market.hasFarmland()) {
            jenius.market.addIndustry(Industries.FARMING)
            jenius.market.getIndustry(Industries.FARMING).isImproved = true
        } else {
            jenius.market.addIndustry(Industries.REFINING)
            jenius.market.getIndustry(Industries.REFINING).isImproved = true
        }
        jenius.market.removeCondition(Conditions.HIGH_GRAVITY)
        jenius.market.addIndustry(Industries.MINING)
        jenius.market.getIndustry(Industries.MINING).specialItem = SpecialItemData(Items.MANTLE_BORE, null)
        jenius.market.addIndustry(Industries.PATROLHQ)
        jenius.market.addIndustry(Industries.GROUNDDEFENSES)
        jenius.market.addIndustry(Industries.WAYSTATION)
        jenius.market.removeIndustry(Industries.SPACEPORT, null, false)
        jenius.market.addIndustry(Industries.MEGAPORT)
        jenius.market.addIndustry(Industries.ORBITALSTATION_HIGH)
        addArtilleryToPlanet(jenius, false)
        jenius.market.addIndustry("IndEvo_Artillery_railgun")
        jenius.market.getIndustry("IndEvo_Artillery_railgun").aiCoreId = Commodities.BETA_CORE

        val siphonPlat = Global.getSector().getEntityById("salus_siphon_plat") ?: return BaseCommand.CommandResult.ERROR
        DecivTracker.decivilize(siphonPlat.market, true)*/

        /*val markets = Global.getSector().getFaction("dassault_mikoyan")?.getMarketsCopy()
        if (markets != null) {
            for (market in markets) {
                DecivTracker.decivilize(market, true)
            }
        }*/

        /*Global.getSector().getEntityById("PSE_newCaledonia").market.factionId = Factions.DIKTAT
        transferMarket(Global.getSector().getEntityById("PSE_newCaledonia").market, Global.getSector().getFaction("pearson_exotronics"), Global.getSector().getFaction(Factions.DIKTAT), false, false, null, 0f, true)

        Console.showMessage("success")*/

        val variant = Global.getSettings().getVariant("legion_xiv_Elite")
        variant.removePermaMod(HullMods.SOLAR_SHIELDING)
        variant.removePermaMod(HullMods.HEAVYARMOR)
        variant.removePermaMod("niko_MPC_fighterSolarShielding")
        variant.tags -= VARIANT_ALWAYS_RECOVERABLE

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