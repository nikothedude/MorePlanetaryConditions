package data.scripts.campaign.econ.conditions.terrain.hyperspace

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.listeners.niko_MPC_saveListener
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.pow
import kotlin.math.roundToInt

class niko_MPC_realspaceHyperspace: HyperspaceTerrainPlugin(), niko_MPC_saveListener {
    var sourceJumpPoint: JumpPointAPI? = null
    var exitJumpPoint: JumpPointAPI? = null

    override fun containsEntity(other: SectorEntityToken?): Boolean {
        return isInClouds(other)
    }

    override fun containsPoint(test: Vector2f?, r: Float): Boolean {
        return isInClouds(test, r)
    }

    override fun advance(amount: Float) {
        //if (true) return;

        //if (true) return;
        super.advance(amount)

        getAbyssPlugin().advance(amount)

        if (!clearedCellsPostLoad && Global.getSector().playerFleet != null) {
            clearCellsNotNearPlayerNonStatic(this)
            clearedCellsPostLoad = true
        }

        playStormStrikeSoundsIfNeeded()

        val days = Global.getSector().clock.convertToDays(amount)

        auto.advance(days * 1f)

        val cells = auto.cells

        val playerFleet = Global.getSector().playerFleet
        var test = Vector2f()
        if (playerFleet != null) {
            test = playerFleet.location // changed
        }

        if (entity.containingLocation != null && playerFleet!!.containingLocation === entity.containingLocation &&
            isInAbyss(playerFleet)
        ) {
            val depth = getAbyssalDepth(playerFleet)
            entity.containingLocation.backgroundColorShifter.shift(
                "abyss_color", ABYSS_BACKGROUND_COLOR, 1f, 1f, depth
            )
            entity.containingLocation.backgroundParticleColorShifter.shift(
                "abyss_color", ABYSS_PARTICLE_COLOR, 1f, 1f, depth
            )
            val gain = spec.custom.optDouble("gain", 0.75).toFloat()
            val gainHF = spec.custom.optDouble("gainHF", 0.1).toFloat()
            if (gain < 1f || gainHF < 1f) {
                Global.getSoundPlayer().applyLowPassFilter(
                    Math.max(0f, 1f - (1f - gain) * depth),
                    Math.max(0f, 1f - (1f - gainHF) * depth)
                )
            }
        }

        var x = entity.location.x
        var y = entity.location.y
        val size = tileSize

        val w = tiles.size * size
        val h = tiles[0].size * size
        x -= w / 2f
        y -= h / 2f
        var xIndex = ((test.x - x) / size).toInt()
        var yIndex = ((test.y - y) / size).toInt()
        if (xIndex < 0) xIndex = 0
        if (yIndex < 0) yIndex = 0
        if (xIndex >= tiles.size) xIndex = tiles.size - 1
        if (yIndex >= tiles[0].size) yIndex = tiles[0].size - 1

        val subgridDist = 10000f + extraDistanceAroundPlayerToAdvanceStormCells
        val baseSubgridDist = 10000f

        val subgridSize = ((subgridDist / size + 1) * 2f).toInt()

        val minX = Math.max(0, xIndex - subgridSize / 2)
        val maxX = xIndex + subgridSize / 2
        val minY = Math.max(0, yIndex - subgridSize / 2)
        val maxY = yIndex + subgridSize / 2

        val baseSubgridSize = ((baseSubgridDist / size + 1) * 2f).toInt()

        val baseMinX = Math.max(0, xIndex - baseSubgridSize / 2)
        val baseMaxX = xIndex + baseSubgridSize / 2
        val baseMinY = Math.max(0, yIndex - baseSubgridSize / 2)
        val baseMaxY = yIndex + baseSubgridSize / 2

        // clean up area around the "active" area so that as the player moves around,
        // they don't leave frozen storm cells behind (which would then make it into the savefile)

        // clean up area around the "active" area so that as the player moves around,
        // they don't leave frozen storm cells behind (which would then make it into the savefile)
        val pad = 4

        var i = minX - pad
        while (i <= maxX + pad && i < tiles.size) {
            var j = minY - pad
            while (j <= maxY + pad && j < tiles[0].size) {
                if (i < minX || j < minY || i > maxX || j > maxY) {
                    if (i >= 0 && j >= 0) {
                        activeCells[i][j] = null
                    }
                }
                j++
            }
            i++
            }

        var ii = minX
        while (ii <= maxX && ii < tiles.size) {
            var j = minY
            while (j <= maxY && j < tiles[0].size) {

                if (tiles[ii][j] < 0) {
                    j++
                    continue
                }
                var curr = activeCells[ii][j]
                val `val` = cells[ii][j]
                val interval = auto.interval.intervalDuration
                if (`val` == 1 && curr == null) {
                    activeCells[ii][j] = CellStateTracker(
                        ii, j,
                        interval * 0f + interval * 1.5f * Math.random().toFloat(),
                        interval * 0.5f + interval * 0.5f * Math.random().toFloat()
                    )
                    curr = activeCells[ii][j]
                    //							interval * 0f + interval * 0.5f * (float) Math.random(),
//							interval * 0.25f + interval * 0.25f * (float) Math.random());
                }
                if (curr != null) {
                    if (`val` != 1 && curr.isStorming && !curr.isWaning) {
                        //curr.wane(interval * 0.25f + interval * 0.25f * (float) Math.random());
                        curr.wane(interval * 0.5f + interval * 0.5f * Math.random().toFloat())
                        //						curr.wane(interval * 0.5f * (float) Math.random() +
//								  interval * 0.25f + interval * 0.25f * (float) Math.random());
                    }
                    var timeMult = 1f
                    if (extraDistanceAroundPlayerToAdvanceStormCells > 0 && stormCellTimeMultOutsideBaseArea > 0) {
                        if (ii < baseMinX || j < baseMinY || i > baseMaxX || j > baseMaxY) {
                            timeMult = stormCellTimeMultOutsideBaseArea
                        }
                    }
                    curr.advance(days * timeMult)
                    if (curr.isOff) {
                        activeCells[ii][j] = null
                    }
                }
                j++
            }
            ii++
        }

        stormCellTimeMultOutsideBaseArea = 0f
        extraDistanceAroundPlayerToAdvanceStormCells = 0f
    }

    companion object {
        fun clearCellsNotNearPlayerNonStatic(plugin: HyperspaceTerrainPlugin) {
            val playerFleet = Global.getSector().playerFleet ?: return
            var test = Vector2f()
            if (playerFleet != null) {
                test = playerFleet.location
            }
            var x = plugin.entity.location.x
            var y = plugin.entity.location.y
            val size = plugin.tileSize
            val w = plugin.tiles.size * size
            val h = plugin.tiles[0].size * size
            x -= w / 2f
            y -= h / 2f
            var xIndex = ((test.x - x) / size).toInt()
            var yIndex = ((test.y - y) / size).toInt()
            if (xIndex < 0) xIndex = 0
            if (yIndex < 0) yIndex = 0
            if (xIndex >= plugin.tiles.size) xIndex = plugin.tiles.size - 1
            if (yIndex >= plugin.tiles[0].size) yIndex = plugin.tiles[0].size - 1
            val subgridSize = ((10000 / size + 1) * 2f).toInt()
            val minX = Math.max(0, xIndex - subgridSize / 2)
            val maxX = xIndex + subgridSize / 2
            val minY = Math.max(0, yIndex - subgridSize / 2)
            val maxY = yIndex + subgridSize / 2

            // clean up area around the "active" area so that as the player moves around,
            // they don't leave frozen storm cells behind (which would then make it into the savefile)
            val pad = Math.max(plugin.tiles.size, plugin.tiles[0].size) * 2
            var i = minX - pad
            while (i <= maxX + pad && i < plugin.tiles.size) {
                var j = minY - pad
                while (j <= maxY + pad && j < plugin.tiles[0].size) {
                    if (i < minX || (j < minY) || i > maxX || j > maxY) {
                        if (i >= 0 && j >= 0) {
                            plugin.activeCells[i][j] = null
                        }
                    }
                    j++
                }
                i++
            }
        }
    }

    override fun applyStormStrikes(cell: CellStateTracker?, fleet: CampaignFleetAPI?, days: Float) {
        if (cell == null) return
        if (fleet == null) return

        if (cell.flicker != null && cell.flicker.wait > 0) {
            cell.flicker.numBursts = 0
            cell.flicker.wait = 0f
            cell.flicker.newBurst()
        }

        if (cell.flicker == null || !cell.flicker.isPeakFrame) return


        fleet.addScript(niko_MPC_realspaceHyperspaceBoost(this, cell, fleet))

        val key = "\$MPC_hyperstormStrikeTimeout"
        val mem = fleet.memoryWithoutUpdate
        if (mem.contains(key)) return
        mem[key, true] = (STORM_MIN_TIMEOUT + (STORM_MAX_TIMEOUT - STORM_MIN_TIMEOUT) * Math.random()).toFloat()

        val members = fleet.fleetData.membersListCopy
        if (members.isEmpty()) return

        var totalValue = 0f
        for (member in members) {
            totalValue += member.stats.suppliesToRecover.modifiedValue
        }
        if (totalValue <= 0) return

        val strikeValue = totalValue * STORM_DAMAGE_FRACTION * (0.5f + Math.random().toFloat() * 0.5f)

        val ebCostThresholdMult = 4f

        val picker = WeightedRandomPicker<FleetMemberAPI>()
        val preferNotTo = WeightedRandomPicker<FleetMemberAPI>()
        for (member in members) {
            var w = 1f
            if (member.isMothballed) w *= 0.1f
            val ebCost = EmergencyBurnAbility.getCRCost(member, fleet)
            if (ebCost * ebCostThresholdMult > member.repairTracker.cr) {
                preferNotTo.add(member, w)
            } else {
                picker.add(member, w)
            }
        }
        if (picker.isEmpty) {
            picker.addAll(preferNotTo)
        }

        val member = picker.pick() ?: return

        val crPerDep = member.deployCost
        val suppliesPerDep = member.stats.suppliesToRecover.modifiedValue
        if (suppliesPerDep <= 0 || crPerDep <= 0) return

        var strikeDamage = crPerDep * strikeValue / suppliesPerDep
        if (strikeDamage < STORM_MIN_STRIKE_DAMAGE) strikeDamage = STORM_MIN_STRIKE_DAMAGE

        val resistance = member.stats.dynamic.getValue(Stats.CORONA_EFFECT_MULT)
        strikeDamage *= resistance

        if (strikeDamage > STORM_MAX_STRIKE_DAMAGE) strikeDamage = STORM_MAX_STRIKE_DAMAGE

        val currCR = member.repairTracker.baseCR
        var crDamage = currCR.coerceAtMost(strikeDamage)

        val ebCost = EmergencyBurnAbility.getCRCost(member, fleet)
        if (currCR >= ebCost * ebCostThresholdMult) {
            crDamage = (currCR - ebCost * 1.5f).coerceAtMost(crDamage)
        }

        if (crDamage > 0) {
            member.repairTracker.applyCREvent(-crDamage, "hyperstorm", "Hyperspace storm strike")
        }

        var hitStrength = member.stats.armorBonus.computeEffective(member.hullSpec.armorRating)
        hitStrength *= strikeDamage / crPerDep
        if (hitStrength > 0) {
            member.status.applyDamage(hitStrength)
            if (member.status.hullFraction < 0.01f) {
                member.status.hullFraction = 0.01f
            }
        }

        if (fleet.isPlayerFleet) {
            var verb = "suffers"
            var c = Misc.getNegativeHighlightColor()
            if (hitStrength <= 0) {
                verb = "avoids"
                //c = Misc.getPositiveHighlightColor();
                c = Misc.getTextColor()
            }
            Global.getSector().campaignUI.addMessage(
                member.shipName + " " + verb + " damage from the storm", c
            )
            Global.getSector().campaignUI.showHelpPopupIfPossible("chmHyperStorm")
        }
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        val pad = 10f
        val small = 5f
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val bad = Misc.getNegativeHighlightColor()

        val player = Global.getSector().playerFleet
        val inCloud = isInClouds(player)
        val depth = getAbyssalDepth(player)
        val inAbyss = depth > 0f
        val tile = getTilePreferStorm(player.location, player.radius)
        var cell: CellStateTracker? = null
        if (tile != null) {
            cell = activeCells[tile[0]][tile[1]]
        }

        tooltip.addTitle(terrainName)
        if (inAbyss) {
            //abyssal
            tooltip.addPara(
                Global.getSettings().getDescription(getTerrainId() + "_abyssal", Description.Type.TERRAIN).text1, pad
            )
        } else if (!inCloud) {
            // open
            tooltip.addPara(
                Global.getSettings().getDescription(getTerrainId() + "_normal", Description.Type.TERRAIN).text1, pad
            )
        } else if (cell == null || !cell.isStorming) {
            // deep
            tooltip.addPara(
                Global.getSettings().getDescription(getTerrainId() + "_deep", Description.Type.TERRAIN).text1, pad
            )
        } else if (cell.isStorming) {
            // storm
            tooltip.addPara(
                Global.getSettings().getDescription(getTerrainId() + "_storm", Description.Type.TERRAIN).text1, pad
            )
        }


        var nextPad = pad
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad)
            nextPad = small
        }

        if (inAbyss) {
            tooltip.addPara(
                "Reduces the sensor range and sensor profile of fleets inside it by %s. "
                        + "Also reduces the maximum burn level by %s. The reduction is gradual and based "
                        + "on the \"depth\" the fleet has reached.",
                pad,
                highlight,
                "" + Math.round((1f - ABYSS_VISIBLITY_MULT) * 100f) + "%",
                "" + Math.round((1f - ABYSS_BURN_MULT) * 100f) + "%"
            )
            if (ABYSS_NAVIGATION_EFFECT <= 0) {
                tooltip.addPara(
                    ("Skill in navigation is of little use, and does not provide its "
                            + "normal benefit in countering terrain-specific maximum burn penalties."), pad,
                    highlight,
                    "" + (ABYSS_NAVIGATION_EFFECT * 100f).roundToInt() + "%"
                )
            } else {
                tooltip.addPara(
                    ("Skill in navigation is of limited use, and only provides %s of its "
                            + "normal benefit in countering the maximum burn penalty."), pad,
                    highlight,
                    "" + (ABYSS_NAVIGATION_EFFECT * 100f).roundToInt() + "%"
                )
            }
        } else if (inCloud) {
            tooltip.addPara(
                "Reduces the range at which fleets inside can be detected by %s.",
                pad,
                highlight,
                "" + ((1f - VISIBLITY_MULT) * 100f).roundToInt() + "%"
            )
            tooltip.addPara(
                "Reduces the speed of fleets inside by up to %s. Larger fleets are slowed down more.",
                nextPad,
                highlight,
                "" + Math.round((Misc.BURN_PENALTY_MULT) * 100f) + "%"
            )
            val penalty = Misc.getBurnMultForTerrain(Global.getSector().playerFleet)
            tooltip.addPara(
                "Your fleet's speed is reduced by %s.", pad,
                highlight,
                "" + Math.round((1f - penalty) * 100f) + "%" //Strings.X + penaltyStr
            )
            tooltip.addSectionHeading("Hyperspace storms", Alignment.MID, pad)
            var stormDescColor = Misc.getTextColor()
            if (cell != null && cell.isStorming) {
                stormDescColor = bad
            }
            tooltip.addPara(
                ("Being caught in a storm causes storm strikes to damage ships " +
                        "and reduce their combat readiness. " +
                        "Larger fleets attract more damaging strikes."), stormDescColor, pad
            )
            tooltip.addPara(
                ("In addition, storm strikes toss the fleet's drive bubble about " +
                        "with great violence, often causing a loss of control. " +
                        "Some commanders are known to use these to gain additional " +
                        "speed, and to save fuel - a practice known as \"storm riding\"."), Misc.getTextColor(), pad
            )
            tooltip.addPara("\"Slow-moving\" fleets do not attract storm strikes.", Misc.getTextColor(), pad)
        }

        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad)
            //			if (inCloud) {
//				tooltip.addPara("Numerous patches of nebula-like hyperfragments present on the battlefield, slowing ships down to a percentage of their top speed.", small);
//			} else {
//				tooltip.addPara("No effect.", small);
//			}
            if (inAbyss) {
//				public static float ABYSS_SHIP_SPEED_PENALTY = 20f;
//				public static float ABYSS_MISSILE_SPEED_PENALTY = 20f;
                tooltip.addPara(
                    ("Reduces top speed of ships by up to %s, and the top speed and range "
                            + "of missiles by up to %s."), pad,
                    highlight,
                    "" + Math.round(BattleCreationPluginImpl.ABYSS_SHIP_SPEED_PENALTY) + "%",
                    "" + Math.round(BattleCreationPluginImpl.ABYSS_MISSILE_SPEED_PENALTY) + "%"
                )
            } else {
                tooltip.addPara("No combat effects.", nextPad)
            }
        }
    }

    override fun getAbyssalDepth(loc: Vector2f?): Float {
        return 0f
    }

    override fun getAbyssalDepth(other: SectorEntityToken?): Float {
        return 0f
    }

    override fun isInAbyss(other: SectorEntityToken?): Boolean {
        return false
    }

    override fun getAbyssalSystems(): List<StarSystemAPI?> {
        return ArrayList()
    }

    override fun beforeGameSave() {
        params.tiles = null
        savedTiles = encodeTiles(tiles)

        clearCellsNotNearPlayerNonStatic(this)

        savedActiveCells = ArrayList<CellStateTracker>()
        for (i in activeCells.indices) {
            for (j in activeCells[0].indices) {
                val curr: CellStateTracker? = activeCells[i][j]
                if (curr != null && isTileVisible(i, j)) {
                    savedActiveCells.add(curr)
                }
            }
        }
    }
}

class niko_MPC_realspaceHyperspaceBoost(
    val plugin: niko_MPC_realspaceHyperspace,
    val cell: HyperspaceTerrainPlugin.CellStateTracker,
    val fleet: CampaignFleetAPI
): EveryFrameScript {

    companion object {
        var MAX_BURN = Global.getSettings().getFloat("maxStormStrikeBurn")
        var STORM_SPEED_BURST = Global.getSettings().speedPerBurnLevel * 50f
        var DURATION_SECONDS = 1f
    }

    var elapsed: Float = 0f
    var angle: Float = 0f

    init {
        var x = plugin.entity.location.x
        var y = plugin.entity.location.y
        val size = plugin.tileSize

        val w = plugin.tiles.size * size
        val h = plugin.tiles[0].size * size

        x -= w / 2f
        y -= h / 2f

        val tx = x + cell.i * size + size / 2f
        val ty = y + cell.j * size + size / 2f

        angle = Misc.getAngleInDegrees(Vector2f(tx, ty), fleet.location)

        val v = fleet.velocity
        var angle2 = Misc.getAngleInDegrees(v)
        val speed = v.length()
        if (speed < 10) {
            angle2 = fleet.facing
        }

        val bestAngleAt = Global.getSettings().baseTravelSpeed + Global.getSettings().speedPerBurnLevel * 20f
        var mult = 0.5f + 0.4f * speed / bestAngleAt
        if (mult < 0.5f) {
            mult = 0.5f
        }
        if (mult > 0.9f) {
            mult = 0.9f
        }

        angle += Misc.getClosestTurnDirection(angle, angle2) * Misc.getAngleDiff(angle, angle2) * mult
    }

    override fun isDone(): Boolean = elapsed >= DURATION_SECONDS || fleet.isInHyperspace

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        elapsed += amount

        var boost = Misc.getUnitVectorAtDegreeAngle(angle)

        var mult = 1f - elapsed / DURATION_SECONDS
        mult *= 1f.coerceAtMost(elapsed / 0.25f).toDouble().pow(2.0).toFloat()
        if (mult < 0) {
            mult = 0f
        }
        if (mult > 1) {
            mult = 1f
        }
        boost.scale(niko_MPC_realspaceHyperspaceBoost.STORM_SPEED_BURST * amount * mult * 3) // *3 mult added because it feels weird otherwise

        val v = fleet.velocity

        if (fleet.currBurnLevel < niko_MPC_realspaceHyperspaceBoost.MAX_BURN) {
            fleet.setVelocity(v.x + boost.x, v.y + boost.y)
        }

        var angleHeading = Misc.getAngleInDegrees(v)
        if (v.length() < 10) {
            angleHeading = fleet.facing
        }

        boost = Misc.getUnitVectorAtDegreeAngle(angleHeading)
        if (boost.length() >= 1) {
            val durIn = 1f
            val durOut = 3f
            val intensity = 1f
            val sizeNormal = 5f + 20f * intensity
            val modId = "boost " + cell.i + cell.j * 100f
            val glowColor = Color(100, 100, 255, 75)
            for (view in fleet.views) {
                view.windEffectDirX.shift(modId, boost.x * sizeNormal, durIn, durOut, 1f)
                view.windEffectDirY.shift(modId, boost.y * sizeNormal, durIn, durOut, 1f)
                view.windEffectColor.shift(modId, glowColor, durIn, durOut, intensity)
            }
        }
    }
}