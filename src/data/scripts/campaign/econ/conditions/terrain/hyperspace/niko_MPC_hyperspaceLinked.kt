package data.scripts.campaign.econ.conditions.terrain.hyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.OrbitAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.Mining
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript
import data.scripts.everyFrames.niko_MPC_delayedEntityRemovalScript
import data.scripts.everyFrames.niko_MPC_jumpPointStaplingScript
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import data.utilities.niko_MPC_miscUtils.setArc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.MathUtils.getDistance
import org.lwjgl.util.vector.Vector2f
import kotlin.math.roundToInt

class niko_MPC_hyperspaceLinked : niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_hyperspaceLinkedDeletionScript> {

    var terrain: niko_MPC_realspaceHyperspace? = null

    var entryPoint: JumpPointAPI? = null
    var exitPoint: JumpPointAPI? = null

    override var deletionScript: niko_MPC_hyperspaceLinkedDeletionScript? = null

    companion object {
        var slipstreamDetectionBonus: Float = 7f // very very big
        var accessabilityBonus: Float = 0.5f
        var hazardBonus: Float = 0.75f
        var volatilesBonus = 3 // its seriously a lot

        var defenseIncrement = 50f
        var defenseMult = 1.1f
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        val containingLocation = market.containingLocation ?: return
        applyConditionAttributes(id)

        val marketDeserializing = market.isDeserializing()
        if (!marketDeserializing) {
            if (terrain == null) {
                terrain =
                    market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] as? niko_MPC_realspaceHyperspace
            }
            if (entryPoint == null) {
                entryPoint =
                    market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointEntryMemoryId] as? JumpPointAPI
            }
            if (exitPoint == null) {
                exitPoint =
                    market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointExitMemoryId] as? JumpPointAPI
            }
        }
        val appliedTerrain = (terrain != null || entryPoint != null || exitPoint != null)

        val notDeserializing = (!marketDeserializing && !containingLocation.isDeserializing() && market.id != "fake_Colonize")
        if (Global.getCurrentState() != GameState.TITLE && notDeserializing) {
            if (!appliedTerrain) linkToHyperspace(id)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        startDeletionScript(ourMarket)
        if (id == null) return
        unapplyConditionAttributes(id)
    }

    private fun applyConditionAttributes(id: String) {
        val market = getMarket() ?: return
        if (market.isPlayerOwned) {
            val intel = HyperspaceTopographyEventIntel.get()
            if (intel != null && intel.isStageActive(HyperspaceTopographyEventIntel.Stage.SLIPSTREAM_DETECTION)) {
                market.stats.dynamic.getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).modifyFlat(
                    id, getAdjustedSlipstreamDetectionBonus(), name
                )
            }
        }
        market.accessibilityMod.modifyFlat(id, accessabilityBonus, name)

        if (!containedByHyperclouds()) return

        market.hazard.modifyFlat(id, hazardBonus, name)

        val mining = market.getIndustry(Industries.MINING)
        if (mining is Mining) {
            if (mining.isFunctional) mining.supply(id, Commodities.VOLATILES, getAdjustedVolatilesBonus(), name)
            else mining.getSupply(Commodities.VOLATILES).quantity.unmodify(id)
        }

        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, defenseMult, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, defenseIncrement, name)
    }


    private fun unapplyConditionAttributes(id: String) {
        market.stats.dynamic.getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).unmodifyFlat(id)

        market.accessibilityMod.unmodify(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        market.hazard.unmodify(id)

        val mining = market.getIndustry(Industries.MINING)
        if (mining is Mining) mining.getSupply(Commodities.VOLATILES).quantity.unmodify(id)
    }


    private fun getAdjustedVolatilesBonus(): Int {
        return volatilesBonus

       /* val market = getMarket() ?: return 0
        val primaryEntity = market.primaryEntity

        val anchor = 3
        val marketSize = if (market.isInhabited()) market.size else anchor
        val effectiveSize = ((marketSize + 1) - anchor) / 2
        val divisor = if (!containedByHyperclouds()) 3 else 1

        return ((volatilesBonus + effectiveSize) / divisor)*/
    }

    private fun getAdjustedSlipstreamDetectionBonus(): Float {
        /*val market = getMarket() ?: return 0f

        val anchor = 3
        val marketSize = if (market.isInhabited()) market.size else anchor
        val effectiveSize = ((marketSize + 1) - anchor)

        return ((slipstreamDetectionBonus * effectiveSize))*/
        return (slipstreamDetectionBonus)
    }

    private fun linkToHyperspace(id: String) {

        val market = getMarket() ?: return
        val primaryEntity = market.primaryEntity ?: return

        val containingLocation = primaryEntity.containingLocation ?: return
        val starSystem = containingLocation as? StarSystemAPI ?: return

        if (!isEligibleForSystem(starSystem)) return

        entryPoint = Global.getFactory().createJumpPoint("MPC_hyperJumpPoint $id", primaryEntity.name + " Bipartisan Jump Point")
        entryPoint?.relatedPlanet = primaryEntity
        entryPoint?.setStandardWormholeToHyperspaceVisual()
        containingLocation.addEntity(entryPoint)

        val orbitDays = (primaryEntity.radius * 0.2f) * MathUtils.getRandomNumberInRange(0.5f, 1.5f)

        entryPoint?.setCircularOrbit(primaryEntity, 0f, primaryEntity.radius, orbitDays)

        exitPoint = Global.getFactory().createJumpPoint("MPC_hyperJumpPointExit $id", primaryEntity.name + " Bipartisan Jump Point")
        exitPoint!!.setStandardWormholeToStarOrPlanetVisual(primaryEntity)
        Global.getSector().hyperspace.addEntity(exitPoint)
        exitPoint?.location?.set(entryPoint!!.getApproximateHyperspaceLoc())
        exitPoint?.addScript(niko_MPC_jumpPointStaplingScript(toMove = exitPoint!!, target = entryPoint!!))
        if (exitPoint?.isInHyperspace == false) {
            displayError("for some reason, a hyperspace linked exit jump point was spawned outside of hyperspace")
        }

        val fromSystemToHyper = JumpDestination(exitPoint, "hyperspace")
        val fromHyperToSystem = JumpDestination(entryPoint, primaryEntity.name + " Bipartisan Jump Point")

        entryPoint?.addDestination(fromSystemToHyper)
        exitPoint?.addDestination(fromHyperToSystem)
        entryPoint?.memoryWithoutUpdate?.set(niko_MPC_ids.hyperspaceLinkedJumpPointDesignationId, true)
        exitPoint?.memoryWithoutUpdate?.set(niko_MPC_ids.hyperspaceLinkedJumpPointDesignationId, true)

        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedExitJumppoints] !is MutableSet<*>) {
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedExitJumppoints] = HashSet<JumpPointAPI>()
        }
        (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedExitJumppoints] as MutableSet<JumpPointAPI>) += exitPoint!!

        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointEntryMemoryId] = entryPoint
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointExitMemoryId] = exitPoint

        val nebula = Misc.addNebulaFromPNG("data/campaign/terrain/generic_system_nebula.png",
            0f, 0f, containingLocation, "terrain", "deep_hyperspace", 4, 4, "MPC_realspaceHyperspace", starSystem.age) as? CampaignTerrainAPI ?: return
        val plugin = nebula.plugin as niko_MPC_realspaceHyperspace
        terrain = plugin
        terrain!!.sourceJumpPoint = entryPoint
        terrain!!.exitJumpPoint = exitPoint
        Global.getSector().listenerManager.addListener(terrain, false)
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] = terrain
        Global.getSector().memoryWithoutUpdate["\$niko_MPC_hyperspaceLinkedWithTerrain"]

        createArc()
    }

    fun createArc() {
        val market = getMarket() ?: return
        val primaryEntity = market.primaryEntity ?: return

        val containingLocation = primaryEntity.containingLocation ?: return

        var arcTarget = primaryEntity
        val orbit: OrbitAPI? = primaryEntity.orbit
        var orbitFocus: SectorEntityToken? = orbit?.focus
        val radius = primaryEntity.radius
        val buffer = radius * 2.5f
        var sizeBonus = 0f

        val arcSource = Vector2f()

        var dist: Float

        if (orbitFocus == null) {
            arcSource.set(primaryEntity.location)
            dist = 0f
        } else {
            arcSource.set(orbitFocus.location)

            val starSystem = containingLocation as? StarSystemAPI

            var recursiveFocus: SectorEntityToken? = orbitFocus.orbitFocus
            if (recursiveFocus != null && starSystem?.star != null) { // otherwise? fuck it. we ball. seriously, where do you find a orbit with 3 layers of recursion
                val extraDist = getDistance(primaryEntity, orbitFocus) + primaryEntity.radius + orbitFocus.radius
                sizeBonus += extraDist

                val star = starSystem.star
                arcSource.set(star.location)
                arcTarget = orbitFocus
                orbitFocus = star
            }
            dist = getDistance(arcTarget, orbitFocus) + primaryEntity.radius // i dont know how these 2 lines work
            if (orbitFocus != null) dist += orbitFocus.radius

            /*var lastFocus: SectorEntityToken = orbitFocus
            var recursiveFocus: SectorEntityToken? = orbitFocus.orbitFocus
            var maxDistanceFound = 0f

            val recursiveEntities: MutableSet<SectorEntityToken> = hashSetOf(primaryEntity)
            var recursionLevel = 0

            while (recursiveFocus != null) {
                recursiveEntities += recursiveFocus

                arcSource.set(recursiveFocus.location)
                val recursiveDist = MathUtils.getDistance(lastFocus, primaryEntity) + recursiveFocus.radius + primaryEntity.radius
                if (recursiveDist > maxDistanceFound) {
                    maxDistanceFound = recursiveDist
                    sizeBonus += recursiveDist
                }
                dist = MathUtils.getDistance(primaryEntity, lastFocus) + orbitFocus.radius + recursiveFocus.radius
                lastFocus = recursiveFocus
                recursiveFocus = recursiveFocus.orbitFocus
            }*/
        }

        val innerRadius = (dist - buffer - sizeBonus).coerceAtLeast(0f)
        val outerRadius = (dist + buffer + sizeBonus)
        val level = 100

        val nebulaEditor = NebulaEditor(terrain)
        nebulaEditor?.setArc(level, arcSource.x, arcSource.y,  innerRadius, outerRadius, 0f, 360f)
        val baseTimesToRemove = 50
        val widthOfArc = (outerRadius - innerRadius)
        var timesToRemove = (baseTimesToRemove * (widthOfArc)).roundToInt()
        while(timesToRemove-- > 0) {
            val randXDepth = MathUtils.getRandomNumberInRange(0f, widthOfArc)
            val preOffsetX = if (MathUtils.getRandom().nextFloat() >= 0.5f) randXDepth * -1 else randXDepth
            val randX = preOffsetX + arcSource.x

            val randYDepth = MathUtils.getRandomNumberInRange(0f, widthOfArc)
            val preOffsetY = if (MathUtils.getRandom().nextFloat() >= 0.5f) randYDepth * -1 else randYDepth
            val randY = preOffsetY + arcSource.y

            nebulaEditor.setTileAt(randX, randY, -1, 0f, false)
        }
    }

    fun isEligibleForSystem(starSystem: StarSystemAPI): Boolean {
        if (Global.getCurrentState() == GameState.TITLE && !starSystem.isProcgen) return false // during sector gen

        for (tag in starSystem.tags) {
            if (tag == Tags.SYSTEM_CUT_OFF_FROM_HYPER) return false
        }
        return true
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "%s slipstream detection radius",
            10f,
            Misc.getHighlightColor(),
            "+${getAdjustedSlipstreamDetectionBonus().toInt()} ly"
        )

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            "+${(accessabilityBonus*100).toInt()}%"
        )

        if (!containedByHyperclouds()) {
            var desc = ""
            val localHighlights: ArrayList<String> = ArrayList()
            if (marketProtected()) {
                desc += "Since ${market.name} is protected by a %s, it is isolated from external hazards. "
                localHighlights += "planetary shield"
            }
            desc += "Further effects could be acquired if ${market.name} was within hyperclouds."
            tooltip.addPara(
                desc,
                5f,
                Misc.getHighlightColor(),
                *(localHighlights.toTypedArray())
            )
            return
        }

        tooltip.addPara(
            "%s hazard rating",
            10f,
            Misc.getHighlightColor(),
            "+${(hazardBonus*100).toInt()}%"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            "${defenseMult}x"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            "+${defenseIncrement.toInt()}"
        )

        tooltip.addPara(
            "%s volatiles production (mining)",
            10f,
            Misc.getHighlightColor(),
            "+${getAdjustedVolatilesBonus()}"
        )
    }

    private fun marketProtected(): Boolean {
        val market = getMarket() ?: return false
        for (industry in market.industries) {
            if (industry.id == Industries.PLANETARYSHIELD && (industry.isFunctional)) return true
        }
        return false
    }

    fun containedByHyperclouds(): Boolean {
        if (marketProtected()) return false
        val plugin = terrain ?: return false
        val primaryEntity = getMarket()?.primaryEntity ?: return false
        return plugin.containsEntity(primaryEntity)
    }

    fun getCachedCells(): MutableList<CellStateTracker> {
        val market = getMarket() ?: return ArrayList()
        if (market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedSavedCellsMemoryId] !is MutableList<*>) {
            market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedSavedCellsMemoryId] = ArrayList<CellStateTracker>()
        }
        return market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedSavedCellsMemoryId] as? MutableList<CellStateTracker> ?: return ArrayList()
    }

    /*fun getTerrain(): niko_MPC_realspaceHyperspace? {
        val market = getMarket() ?: return null

        val memory = market.memoryWithoutUpdate ?: return null
        return memory[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] as? niko_MPC_realspaceHyperspace
    }*/

    override fun delete() {
        super.delete()

        val market = getMarket() ?: return

        if (entryPoint == null) { // this is so sloppy aaa
            entryPoint = market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointEntryMemoryId] as? JumpPointAPI
        }
        if (entryPoint == null) {
            entryPoint = terrain?.sourceJumpPoint
        }
        if (exitPoint == null) {
            exitPoint = market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointExitMemoryId] as? JumpPointAPI
        }
        if (exitPoint == null) {
            exitPoint = terrain?.exitJumpPoint
        }
        if (terrain == null) {
            terrain = market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] as? niko_MPC_realspaceHyperspace
        }

        entryPoint?.let { niko_MPC_delayedEntityRemovalScript(it, 30f).start() }
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointEntryMemoryId] = null
        exitPoint?.let { niko_MPC_delayedEntityRemovalScript(it, 30f).start() }
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointExitMemoryId] = null
        val jumpPointList: MutableSet<JumpPointAPI>? = (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedExitJumppoints] as? MutableSet<JumpPointAPI>)
        if (exitPoint != null && jumpPointList != null) jumpPointList -= exitPoint!!

        Global.getSector().listenerManager.removeListener(terrain)
        terrain?.entity?.containingLocation?.removeEntity(terrain?.entity)
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] = null
        market.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedSavedCellsMemoryId] = null
    }

    override fun createDeletionScriptInstance(vararg args: Any): niko_MPC_hyperspaceLinkedDeletionScript {
        val market = args[0] as MarketAPI
        return niko_MPC_hyperspaceLinkedDeletionScript(market.primaryEntity, getCondition().id, this, this)
    }
}

class niko_MPC_hyperspaceLinkedDeletionScript(entity: SectorEntityToken, conditionId: String,
                                              override val condition: niko_MPC_hyperspaceLinked? = null,
                                              hasDeletionScript: hasDeletionScript<out deletionScript?>):
    niko_MPC_conditionRemovalScript(entity, conditionId, condition, hasDeletionScript) {
}