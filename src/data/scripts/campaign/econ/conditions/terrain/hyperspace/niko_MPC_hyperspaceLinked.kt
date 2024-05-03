package data.scripts.campaign.econ.conditions.terrain.hyperspace

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
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
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.JumpPoint
import com.fs.starfarer.campaign.StarSystem
import com.sun.org.apache.xpath.internal.operations.Bool
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript
import data.scripts.everyFrames.niko_MPC_delayedEntityRemovalScript
import data.scripts.everyFrames.niko_MPC_jumpPointStaplingScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import data.utilities.niko_MPC_miscUtils.setArc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.MathUtils.getDistance
import org.lwjgl.util.vector.Vector2f

class niko_MPC_hyperspaceLinked : niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_hyperspaceLinkedDeletionScript> {

    var applied = false

    var terrain: niko_MPC_realspaceHyperspace? = null

    var entryPoint: JumpPointAPI? = null
    var exitPoint: JumpPointAPI? = null

    override var deletionScript: niko_MPC_hyperspaceLinkedDeletionScript? = null

    var slipstreamDetectionBonus: Float = 8f // very very big
    var accessabilityBonus: Float = 0.5f
    var hazardBonus: Float = 0.5f
    var volatilesBonus = 4 // its seriously a lot

    var defenseIncrement = 200f
    var defenseMult = 2f

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        val contaningLocation = market.containingLocation ?: return
        applyConditionAttributes(id)

        val notDeserializing = (!market.isDeserializing() && !contaningLocation.isDeserializing() && market.id != "fake_Colonize")
        if (!applied && notDeserializing) {
            linkToHyperspace(id)
            applied = true
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
        val intel = HyperspaceTopographyEventIntel.get()
        if (intel != null && intel.isStageActive(HyperspaceTopographyEventIntel.Stage.SLIPSTREAM_DETECTION)) {
            market.stats.dynamic.getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).modifyFlat(
                id, getAdjustedSlipstreamDetectionBonus(), "${market.name} ${name}"
            )
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
        val market = getMarket() ?: return 0
        val primaryEntity = market.primaryEntity

        val anchor = 3
        val effectiveSize = ((market.size) - anchor) * 2
        val divisor = if (primaryEntity.orbit != null) 3 else 1

        return ((volatilesBonus + effectiveSize) / divisor)
    }

    private fun getAdjustedSlipstreamDetectionBonus(): Float {
        val market = getMarket() ?: return 0f

        val anchor = 3
        val effectiveSize = ((market.size) - anchor)

        return ((slipstreamDetectionBonus * effectiveSize))
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
        entryPoint?.setCircularOrbit(primaryEntity, 0f, primaryEntity.radius, primaryEntity.radius * 0.2f)

        exitPoint = Global.getFactory().createJumpPoint("MPC_hyperJumpPointExit $id", primaryEntity.name + " Bipartisan Jump Point")
        exitPoint!!.setStandardWormholeToStarOrPlanetVisual(primaryEntity)
        Global.getSector().hyperspace.addEntity(exitPoint)
        exitPoint?.location?.set(entryPoint!!.getApproximateHyperspaceLoc())
        exitPoint?.addScript(niko_MPC_jumpPointStaplingScript(toMove = exitPoint!!, target = entryPoint!!))

        val fromSystemToHyper = JumpDestination(exitPoint, "hyperspace")
        val fromHyperToSystem = JumpDestination(entryPoint, primaryEntity.name + " Bipartisan Jump Point")

        entryPoint?.addDestination(fromSystemToHyper)
        exitPoint?.addDestination(fromHyperToSystem)

        val nebula = Misc.addNebulaFromPNG("data/campaign/terrain/generic_system_nebula.png",
            0f, 0f, containingLocation, "terrain", "deep_hyperspace", 4, 4, "MPC_realspaceHyperspace", starSystem.age) as? CampaignTerrainAPI ?: return

        var arcTarget = primaryEntity
        val orbit = primaryEntity.orbit
        var orbitFocus = orbit.focus
        val radius = primaryEntity.radius
        val buffer = radius * 4f
        var sizeBonus = 0f

        val arcSource = Vector2f()

        var dist: Float

        if (orbitFocus == null) {
            arcSource.set(primaryEntity.location)
            dist = 0f
        } else {
            arcSource.set(orbitFocus.location)

            var recursiveFocus: SectorEntityToken? = orbitFocus.orbitFocus
            if (recursiveFocus != null && containingLocation.star != null) { // otherwise? fuck it. we ball. seriously, where do you find a orbit with 3 layers of recursion
                val extraDist = getDistance(primaryEntity, orbitFocus) + primaryEntity.radius + orbitFocus.radius
                sizeBonus += extraDist

                val star = containingLocation.star
                arcSource.set(star.location)
                arcTarget = orbitFocus
                orbitFocus = star
            }
            dist = getDistance(arcTarget, orbitFocus) + orbitFocus.radius + primaryEntity.radius

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

        val plugin = nebula.plugin as niko_MPC_realspaceHyperspace
        terrain = plugin

        val nebulaEditor = NebulaEditor(plugin)
        nebulaEditor.setArc(level, arcSource.x, arcSource.y,  innerRadius, outerRadius, 0f, 360f)
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
            "%s slipstream detection radius (based on population size)",
            10f,
            Misc.getHighlightColor(),
            "+${getAdjustedVolatilesBonus()}"
        )

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            "+${accessabilityBonus*100}%"
        )

        if (!containedByHyperclouds()) {
            tooltip.addPara(
                "Further effects could be acquired if ${market.name} was within hyperclouds.",
                5f
            )
            return
        }

        tooltip.addPara(
            "%s hazard rating",
            10f,
            Misc.getHighlightColor(),
            "+${hazardBonus*100}%"
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
            "+$defenseIncrement"
        )

        tooltip.addPara(
            "%s mining volatiles production (based on population size)",
            10f,
            Misc.getHighlightColor(),
            "+${getAdjustedVolatilesBonus()}"
        )
    }

    fun containedByHyperclouds(): Boolean {
        val plugin = terrain ?: return false
        val primaryEntity = getMarket()?.primaryEntity ?: return false
        return plugin.containsEntity(primaryEntity)
    }

    /*private fun getTerrain(): niko_MPC_realspaceHyperspace? {
        val market = getMarket() ?: return null

        val memory = market.memoryWithoutUpdate ?: return null
        return memory[niko_MPC_ids.hyperspaceLinkedTerrainMemoryId] as? niko_MPC_realspaceHyperspace
    }*/

    override fun delete() {
        super.delete()

        val market = getMarket() ?: return

        entryPoint?.let { niko_MPC_delayedEntityRemovalScript(it, 30f).start() }
        exitPoint?.let { niko_MPC_delayedEntityRemovalScript(it, 30f).start() }

        terrain?.entity?.containingLocation?.removeEntity(terrain?.entity)
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