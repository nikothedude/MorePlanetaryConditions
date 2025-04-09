package data.scripts.campaign.econ.conditions.terrain.magfield

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.Mining
import com.fs.starfarer.api.impl.campaign.econ.impl.Refining
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.niko_MPC_modPlugin
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_stringUtils
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

class niko_MPC_hyperMagneticField:
    niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_magfieldConditionDeletionScript> {

    override var deletionScript: niko_MPC_magfieldConditionDeletionScript? = null

    companion object {
        var hazardRatingIncrement = 0.5f
        var accessabilityIncrement = -0.25f
        var defenseRatingMult = 1.3f

        const val REFINING_UPKEEP_MULT = 0.2f
    }
    var terrainPlugin: niko_MPC_hyperMagField? = null

    override fun apply(id: String) {
        super.apply(id)

        applyconditionAttributes(id)

        val market = getMarket() ?: return
        val containingLocation = market.containingLocation ?: return

        val marketDeserializing = market.isDeserializing()
        if (!marketDeserializing) {
            if (terrainPlugin == null) {
                terrainPlugin = market.memoryWithoutUpdate[niko_MPC_ids.hyperMagneticFieldMemoryId] as? niko_MPC_hyperMagField
            }
        }
        val notDeserializing = (!marketDeserializing && !containingLocation.isDeserializing() && market.id != "fake_Colonize")
        if (notDeserializing && terrainPlugin == null) {
            addTerrain()
        }
    }

    private fun addTerrain(): Boolean {
        val market = getMarket() ?: return false
        val primaryEntity = market.primaryEntity ?: return false

        if (Global.getCurrentState() == GameState.TITLE) return false

        val widthToUse = 3000f
        var visStartRadius = (primaryEntity.radius * 1.5f)
        var visEndRadius = visStartRadius + widthToUse
        var bandWidth = (visEndRadius) * 1f
        var midRadius = 0f
        var auroraProbability = 1f

        val auroraIndex = (MagFieldGenPlugin.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()

        val ringParams = MagneticFieldTerrainPlugin.MagneticFieldParams(
            bandWidth, midRadius,
            primaryEntity,
            visStartRadius, visEndRadius,
            niko_MPC_settings.hyperMagFieldColors.random(),
            auroraProbability,
            *MagFieldGenPlugin.auroraColors[auroraIndex],
        )
        val magField: CampaignTerrainAPI = primaryEntity.containingLocation.addTerrain("MPC_magnetic_field_hyper", ringParams) as? CampaignTerrainAPI
            ?: return false
        val plugin: niko_MPC_hyperMagField = magField.plugin as? niko_MPC_hyperMagField ?: return false
        terrainPlugin = plugin

        for (terrain in primaryEntity.containingLocation.terrainCopy) {
            if (terrain.plugin !is MagneticFieldTerrainPlugin) continue
            if (terrain.orbitFocus != primaryEntity) continue

            if (terrain.name != null && terrain.name != "Magnetic Field") {
                terrainPlugin!!.terrainName = terrain.name
            }
            primaryEntity.containingLocation.removeEntity(terrain)
        }

        magField.location.set(primaryEntity.location)
        magField.setCircularOrbit(primaryEntity, 0f, 0f, 100f)
        Global.getSector().listenerManager.addListener(terrainPlugin!!, false)
        market.memoryWithoutUpdate[niko_MPC_ids.hyperMagneticFieldMemoryId] = terrainPlugin
        return true
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        startDeletionScript(ourMarket)
        if (id == null) return
        unapplyConditionAttributes(id)
    }

    override fun delete() {
        val market = getMarket() ?: return
        val containingLocaiton = market.containingLocation ?: return

        containingLocaiton.removeEntity(terrainPlugin?.entity)
        market.memoryWithoutUpdate[niko_MPC_ids.hyperMagneticFieldMemoryId] = null
        Global.getSector().listenerManager.removeListener(terrainPlugin)

        super.delete()
    }

    private fun applyconditionAttributes(id: String) {
        val market = getMarket() ?: return
        market.hazard.modifyFlat(id, hazardRatingIncrement, name)
        market.accessibilityMod.modifyFlat(id, accessabilityIncrement, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, defenseRatingMult, name)

        var mining: Industry? = null
        for (industry in market.industries) {
            val spec = industry.spec ?: continue
            if (spec.tags.contains(Industries.MINING)) {
                mining = industry
                break
            }
        }
        if (mining != null) {
            mining.supply(id, Commodities.ORE, (mining.getSupply(Commodities.ORE).quantity.modifiedInt * -0.5).roundToInt(), name)
            mining.supply(id, Commodities.RARE_ORE, (mining.getSupply(Commodities.RARE_ORE).quantity.modifiedInt * -0.5).roundToInt(), name)
            mining.getSupply(Commodities.ORE).quantity.modifyMult(id, 0.5f, name)
            mining.getSupply(Commodities.RARE_ORE).quantity.modifyMult(id, 0.5f, name)
            if (mining.isFunctional) {
                mining.supply(id, Commodities.METALS, mining.getSupply(Commodities.ORE).quantity.modifiedInt, name)
                mining.supply(id, Commodities.RARE_METALS, mining.getSupply(Commodities.RARE_ORE).quantity.modifiedInt, name)
            }
            else {
                mining.getSupply(Commodities.RARE_METALS).quantity.unmodify(id)
                mining.getSupply(Commodities.METALS).quantity.unmodify(id)
            }
        }

        var refining: Industry? = null
        for (industry in market.industries) {
            val spec = industry.spec ?: continue
            if (spec.tags.contains(Industries.REFINING)) {
                refining = industry
                break
            }
        }
        refining?.upkeep?.modifyMult(id, REFINING_UPKEEP_MULT, name)
    }

    private fun unapplyConditionAttributes(id: String) {
        val market = getMarket() ?: return
        market.hazard.unmodify(id)
        market.accessibilityMod.unmodify(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        var mining: Industry? = null
        for (industry in market.industries) {
            val spec = industry.spec ?: continue
            if (spec.tags.contains(Industries.MINING)) {
                mining = industry
                break
            }
        }
        if (mining != null) {
            mining.getSupply(Commodities.RARE_ORE).quantity.unmodify(id)
            mining.getSupply(Commodities.ORE).quantity.unmodify(id)
            mining.getSupply(Commodities.RARE_METALS).quantity.unmodify(id)
            mining.getSupply(Commodities.METALS).quantity.unmodify(id)
        }
        var refining: Industry? = null
        for (industry in market.industries) {
            val spec = industry.spec ?: continue
            if (spec.tags.contains(Industries.REFINING)) {
                refining = industry
                break
            }
        }
        refining?.upkeep?.unmodify(id)
    }

    override fun createDeletionScriptInstance(vararg args: Any): niko_MPC_magfieldConditionDeletionScript {
        val market = args[0] as MarketAPI
        return niko_MPC_magfieldConditionDeletionScript(market.primaryEntity, condition.id, this, this)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "%s hazard rating",
            10f,
            Misc.getHighlightColor(),
            "+${(hazardRatingIncrement*100).toInt()}%"
        )

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            "${(accessabilityIncrement*100).toInt()}%"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            "${defenseRatingMult}x"
            )

        tooltip.addPara(
            "Ferromagnetic metals are naturally compressed by the magnetic field, resulting in %s",
            10f,
            Misc.getHighlightColor(),
            "half of the ore/rare ore output of mining being converted into metals/transplutonics"
        )

        tooltip.addPara(
            "The above processes can be used in refining, resulting in %s being reduced by %s",
            10f,
            Misc.getHighlightColor(),
            "refining upkeep", niko_MPC_stringUtils.toPercent(1 - REFINING_UPKEEP_MULT)
        )

    }
}
class niko_MPC_magfieldConditionDeletionScript(entity: SectorEntityToken, conditionId: String,
                                               override val condition: niko_MPC_hyperMagneticField? = null,
                                               hasDeletionScript: hasDeletionScript<out deletionScript?>
):
    niko_MPC_conditionRemovalScript(entity, conditionId, condition, hasDeletionScript) {
}