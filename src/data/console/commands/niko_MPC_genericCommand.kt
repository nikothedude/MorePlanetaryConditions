package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.intel.groundbattle.GBUtils
import exerelin.campaign.intel.groundbattle.GroundBattleIntel
import exerelin.campaign.intel.groundbattle.GroundUnit
import exerelin.campaign.intel.groundbattle.GroundUnitDef
import org.lazywizard.console.BaseCommand
import org.lwjgl.util.vector.Vector2f
import java.util.*

class niko_MPC_genericCommand: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val fleet: CampaignFleetAPI = Global.getSector().playerFleet ?: return BaseCommand.CommandResult.ERROR

        //JumpPointAPI jp = findGravityWell();

        //JumpPointAPI jp = findGravityWell()

        var strength: Float = 0.4f
        strength *= GenerateSlipsurgeAbility.SLIPSURGE_STRENGTH_MULT

        val angle = 50f
        val offset = 100f
//		Vector2f from = Misc.getUnitVectorAtDegreeAngle(angle);
//		from.scale(offset + fleet.getRadius());
//		Vector2f.add(from, startLoc, from);
        //		Vector2f from = Misc.getUnitVectorAtDegreeAngle(angle);
//		from.scale(offset + fleet.getRadius());
//		Vector2f.add(from, startLoc, from);
        val from: Vector2f = fleet.location

        val params = SlipstreamTerrainPlugin2.SlipstreamParams2()

        params.enteringSlipstreamTextOverride = "Entering slipsurge"
        params.enteringSlipstreamTextDurationOverride = 0.1f
        params.forceNoWindVisualEffectOnFleets = true

        val width = 600f
        var length = 1000f

        length += strength * 500f
        params.burnLevel = Math.round(400f + strength * strength * 500f)
        params.accelerationMult = 20f + strength * strength * 280f

        //params.accelerationMult = 500f;


        //params.accelerationMult = 500f;
        params.baseWidth = width
        params.widthForMaxSpeed = 400f
        params.widthForMaxSpeedMinMult = 0.34f
        //params.widthForMaxSpeed = 300f;
        //params.widthForMaxSpeed = 300f;
        params.slowDownInWiderSections = true
        //params.edgeWidth = 100f;
        //params.accelerationMult = 100f;


        //params.edgeWidth = 100f;
        //params.accelerationMult = 100f;
        params.minSpeed = Misc.getSpeedForBurnLevel((params.burnLevel - params.burnLevel / 8).toFloat())
        params.maxSpeed = Misc.getSpeedForBurnLevel((params.burnLevel + params.burnLevel / 8).toFloat())
        //params.lineLengthFractionOfSpeed = 0.25f * Math.max(0.25f, Math.min(1f, 30f / (float) params.burnLevel));
        //params.lineLengthFractionOfSpeed = 0.25f * Math.max(0.25f, Math.min(1f, 30f / (float) params.burnLevel));
        params.lineLengthFractionOfSpeed = 2000f / ((params.maxSpeed + params.minSpeed) * 0.5f)

        val lineFactor = 0.1f
        params.minSpeed *= lineFactor
        params.maxSpeed *= lineFactor
        //params.lineLengthFractionOfSpeed *= 0.25f;
        //params.lineLengthFractionOfSpeed *= 1f;
        //params.lineLengthFractionOfSpeed *= 0.25f;
        //params.lineLengthFractionOfSpeed *= 1f;
        params.maxBurnLevelForTextureScroll = (params.burnLevel * 0.1f).toInt()

        params.particleFadeInTime = 0.01f
        params.areaPerParticle = 1000f

        val to = Misc.getUnitVectorAtDegreeAngle(angle)
        to.scale(offset + fleet.radius + length)
        Vector2f.add(to, from, to)

        val slipstream = fleet.getContainingLocation().addTerrain(Terrain.SLIPSTREAM, params) as CampaignTerrainAPI
        slipstream.addTag(Tags.SLIPSTREAM_VISIBLE_IN_ABYSS)
        slipstream.setLocation(from.x, from.y)

        val plugin = slipstream.plugin as SlipstreamTerrainPlugin2

        val spacing = 100f
        val incr = spacing / length

        val diff = Vector2f.sub(to, from, Vector2f())
        var f = 0f
        while (f <= 1f) {
            val curr = Vector2f(diff)
            curr.scale(f)
            Vector2f.add(curr, from, curr)
            plugin.addSegment(curr, width - Math.min(300f, 300f * Math.sqrt(f.toDouble()).toFloat()))
            f += incr
        }

        plugin.recomputeIfNeeded()

        plugin.despawn(1.5f, 0.2f, Random())

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