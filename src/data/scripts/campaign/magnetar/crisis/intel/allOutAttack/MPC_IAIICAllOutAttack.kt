package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.command.WarSimScript
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.intel.raid.OrganizeStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.ReturnStage
import com.fs.starfarer.api.impl.campaign.intel.raid.TravelStage
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionOrders
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*

class MPC_IAIICAllOutAttack(val from: MarketAPI, val target: MarketAPI, val spawnFP: Float): RaidIntel(target.starSystem, from.faction, null),
    RaidIntel.RaidDelegate {

    val random: Random = MathUtils.getRandom()
    var expectedCores: MutableList<String> = ArrayList()
    var targettingFractalCore: Boolean = false
    var orders: MPC_IAIICInspectionOrders = MPC_IAIICInspectionOrders.RESIST
    protected var action: MPC_IAIICAOAActionStage? = null
    var enteredSystem: Boolean = false
    var investedCredits: Int = 0


    companion object {
        val MADE_HOSTILE_UPDATE = Any()

        const val MPC_IAIICAOAFLEET = "\$MPC_IAIICAOAFLEET"
    }

    override fun getName(): String {
        val base = "IAIIC All-Out Attack"
        if (isFailed) return "$base - Failed" else if (isSucceeded) return "$base - Completed" else return base
    }

    override fun notifyRaidEnded(raid: RaidIntel?, status: RaidStageStatus?) {
        if (action?.gotCore == true) {
            MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
        } else {
            MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FAILED_ALL_OUT_ATTACK)
        }
    }

    init {
        setup()
    }
    fun setup() {
        val orgDur = if (Global.getSettings().isDevMode) 3f else 60f + MathUtils.getRandomNumberInRange(3f, 9f)
        addStage(OrganizeStage(this, from, orgDur))

        val gather = from.primaryEntity
        val raidJump: SectorEntityToken? = RouteLocationCalculator.findJumpPointToUse(factionForUIColors, target.primaryEntity)

        if (gather == null || raidJump == null) {
            endImmediately()
            return
        }

        val successMult = 0.2f

        val assemble = MPC_IAIICAllOutAttackAssemble(this, gather)
        assemble.addSource(from)
        assemble.spawnFP = spawnFP
        assemble.abortFP = spawnFP * successMult
        addStage(assemble)

        val travel = TravelStage(this, gather, raidJump, false)
        travel.abortFP = spawnFP * successMult
        addStage(travel)

        action = MPC_IAIICAOAActionStage(this, target)
        action!!.abortFP = spawnFP * successMult
        addStage(action)

        addStage(ReturnStage(this))

        isImportant = true
        //makeHostileAndSendUpdate()

        Global.getSector().intelManager.addIntel(this)
        MPC_IAIICFobIntel.get()?.currentAction = this
    }

    override fun failedAtStage(stage: RaidStage?) {
        super.failedAtStage(stage)

        MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FAILED_ALL_OUT_ATTACK)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f

        info!!.addImage(factionForUIColors.logo, width, 128f, opad)
        //String aOrAn = Misc.getAOrAnFor(noun);
        //info.addPara(Misc.ucFirst(aOrAn) + " %s " + noun + " against "

        //String aOrAn = Misc.getAOrAnFor(noun);
        //info.addPara(Misc.ucFirst(aOrAn) + " %s " + noun + " against "

        val colony = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return

        info.addPara(
            "Pushed to their limit, the %s is pouring all of their remaining fleetpower into one final mission: A %s against %s.",
            opad,
            Misc.getHighlightColor(),
            "IAIIC", "raid", "${colony.name}"
        ).setHighlightColors(
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            Misc.getNegativeHighlightColor(),
            colony.faction.baseUIColor
        )

        info.addPara(
            "Should this attack be defeated, the %s will have suffered a %s, and in all likelihood, will %s.",
            opad,
            Misc.getHighlightColor(),
            "IAIIC", "crushing defeat", "cease to exist"
        ).setHighlightColors(
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            Misc.getHighlightColor(),
            Misc.getPositiveHighlightColor()
        )

        val faction = getFaction()
        val has = faction.displayNameHasOrHave
        val `is` = faction.displayNameIsOrAre

        val `as` = assembleStage
        val source = firstSource

        //float raidStr = as.getSpawnFP();

        //float raidStr = as.getSpawnFP();
        var raidStr = `as`.origSpawnFP
        raidStr = Misc.getAdjustedStrength(raidStr, source)

        val strDesc = raidStrDesc
        val numFleets = origNumFleets.toInt()
        var fleets = "fleets"
        if (numFleets == 1) fleets = " large fleet, or several smaller ones"

        val label = info.addPara(
            "The raiding forces are " +
                    "projected to be " + strDesc +
                    " and likely comprised of " + numFleets + " " + fleets + ", each of which are expected to have" +
                    " multiple s-mods and exceptional officers/commanders.",
            opad
        )
        label.setHighlight(strDesc, "" + numFleets, "multiple s-mods", "exceptional officers/commanders")
        label.setHighlightColors(h, h, h, h)

        defenderStr = WarSimScript.getEnemyStrength(getFaction(), system)
        val defensiveStr = defenderStr + WarSimScript.getStationStrength(target.faction, system, target.primaryEntity)

        var safe = false

        if (defensiveStr > raidStr * 1.25f) {
            safe = true
        }

        if (!isEnding) {
            var showSafe = false
            if (raidStr < defenderStr * 0.75f) {
                info.addPara(
                    "The raiding forces should be %s by fleets defending the system. In the absence of " +
                            "other factors, the raid is unlikely to find success.", opad, Misc.getPositiveHighlightColor(), "outmatched"
                )
            } else if (raidStr < defenderStr * 1.25f) {
                info.addPara("The raiding forces are %s with fleets defending the system.", opad, Misc.getHighlightColor(), "evenly matched")
                showSafe = true
            } else {
                info.addPara("The raiding forces are %s to the fleets defending the system.", opad, Misc.getNegativeHighlightColor(), "superior")
                showSafe = true
            }
            if (showSafe) {
                if (safe) {
                    info.addPara(
                        "However, ${target.name} should be %s from the raid, " +
                                "owing to it's orbital defenses.", opad, Misc.getPositiveHighlightColor(), "safe"
                    )
                } else {
                    info.addPara(
                        "%s's ground defenses are %s, and may fail if the raid is allowed to reach groundfall.", opad, Misc.getNegativeHighlightColor(), "${target.name}", "inadequete"
                    ).setHighlightColors(
                        target.faction.baseUIColor,
                        Misc.getNegativeHighlightColor()
                    )

                    //					info.addPara("Unless the raid is stopped, these colonies " +
                    //							"may suffer " +
                    //							"reduced stability, infrastructure damage, and a possible loss of stockpiled resources.", opad);
                }
            }
        }

        info.addSectionHeading(
            "Status",
            faction.baseUIColor, faction.darkUIColor, Alignment.MID, opad
        )

        for (stage in stages) {
            stage.showStageInfo(info)
            if (getStageIndex(stage) == failStage) break
        }
    }

    fun makeHostileAndSendUpdate() {
        val hostile = getFaction().isHostileTo(Factions.PLAYER)
        if (!hostile) {
            val repResult = Global.getSector().adjustPlayerReputation(
                CoreReputationPlugin.RepActionEnvelope(
                    CoreReputationPlugin.RepActions.MAKE_HOSTILE_AT_BEST,
                    null, null, null, false, false
                ),
                niko_MPC_ids.IAIIC_FAC_ID
            )
        }
        sendUpdateIfPlayerHasIntel(MADE_HOSTILE_UPDATE, false)
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f

        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad

        val tc = getBulletColorForMode(mode)

        bullet(info)
        val isUpdate = getListInfoParam() != null

        val eta = eta

        info!!.addPara(
            "Faction: " + faction.displayName, initPad, tc,
            faction.baseUIColor, faction.displayName
        )
        initPad = 0f

        var max = 0
        var target: MarketAPI? = null
        for (other in Misc.getMarketsInLocation(system)) {
            if (!other.faction.isHostileTo(faction)) continue
            val size = other.size
            if (size > max || size == max && other.faction.isPlayerFaction) {
                max = size
                target = other
            }
        }


        if (target != null) {
            val other = target.faction
            info!!.addPara(
                "Target: " + other.displayName, initPad, tc,
                other.baseUIColor, other.displayName
            )
        }

        if (isUpdate) {
            if (getListInfoParam() === ENTERED_SYSTEM_UPDATE) {
                info.addPara("Arrived in-system", tc, initPad)
            } else if (listInfoParam == MADE_HOSTILE_UPDATE) {
                    info.addPara(
                        "All-out war: %s now %s",
                        0f,
                        Misc.getHighlightColor(), "IAIIC", "permanently hostile"
                    )?.setHighlightColors(
                        factionForUIColors.baseUIColor, Misc.getHighlightColor()
                    )
                } else if (failStage < 0) {
                    info!!.addPara(
                        "Colonies in the " + system.nameWithLowercaseType + " have been raided",
                        tc, initPad
                    )
                } else {
                    info!!.addPara(
                        "The raid on the " + system.nameWithLowercaseType + " has failed",
                        tc, initPad
                    )
                }
        } else {
            info!!.addPara(
                system.nameWithLowercaseType,
                tc, initPad
            )
        }
        initPad = 0f
        if (eta > 1 && failStage < 0 && getListInfoParam() != ENTERED_SYSTEM_UPDATE) {
            val days = getDaysString(eta)
            info!!.addPara(
                "Estimated %s $days until arrival",
                initPad, tc, h, "" + Math.round(eta)
            )
            initPad = 0f
        }

        unindent(info)
    }

    override fun createFleet(
        factionId: String?,
        route: RouteManager.RouteData?,
        market: MarketAPI?,
        locInHyper: Vector2f?,
        random: Random?
    ): CampaignFleetAPI? {
        var random = random
        if (random == null) random = Random()

        val extra = route!!.extra

        var combat = extra.fp
        val tanker = extra.fp * (0.1f + random.nextFloat() * 0.05f)
        val transport = extra.fp * (0.1f + random.nextFloat() * 0.05f)
        val freighter = 0f
        combat -= tanker
        combat -= transport

        val params = FleetParamsV3(
            market,
            locInHyper,
            factionId,
            route.qualityOverride,
            extra.fleetType,
            combat,  // combatPts
            freighter,  // freighterPts
            tanker,  // tankerPts
            transport,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod, won't get used since routes mostly have quality override set
        )

        params.timestamp = route.timestamp
        params.random = random

        params.averageSMods = 2
        params.officerLevelBonus = 4
        params.officerLevelLimit = 7
        params.officerNumberMult = 1.4f

        val fleet = FleetFactoryV3.createFleet(params)

        if (fleet == null || fleet.isEmpty) return null

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_WAR_FLEET] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_RAIDER] = true

        if (fleet.faction.getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PIRATE] = true
        }
        fleet.memoryWithoutUpdate[MPC_IAIICAOAFLEET] = true

        val postId = Ranks.POST_PATROL_COMMANDER
        val rankId = Ranks.SPACE_COMMANDER

        fleet.commander.postId = postId
        fleet.commander.rankId = rankId

        fleet.commander.stats.setSkillLevel(Skills.COORDINATED_MANEUVERS, 1f)
        fleet.commander.stats.setSkillLevel(Skills.CREW_TRAINING, 1f)
        fleet.commander.stats.setSkillLevel(Skills.TACTICAL_DRILLS, 1f)
        fleet.commander.stats.setSkillLevel(Skills.CARRIER_GROUP, 1f)

        return fleet
    }
}