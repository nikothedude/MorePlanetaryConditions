package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_derelictSatelliteHandler
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.everyFrames.niko_MPC_satelliteCustomEntityRemovalScript
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.hasCustomControls
import data.utilities.niko_MPC_settings


class niko_MPC_derelictAntiAsteroidSatellites: niko_MPC_antiAsteroidSatellitesBase() {

    override val suppressorId: String = niko_MPC_industryIds.luddicPathSuppressorStructureId
    override var deletionScript: niko_MPC_satelliteCustomEntityRemovalScript? = null

    init {
        suppressedConditions += Conditions.METEOR_IMPACTS //these things just fuck those things up
        industryIds += suppressorId
    }

    companion object {
        var baseAccessibilityIncrementObj = -0.10f //also placeholder
        var baseGroundDefenseIncrementObj = 400f
        var baseStabilityIncrementObj = 2f

        var baseGroundDefenseMultObj = 1.5f
    }

    // TODO: COMPATABILITY: these fields are only here so games load. remove post 3.1.0
    var baseAccessibilityIncrement = -0.30f
    var baseGroundDefenseIncrement = 400f
    var baseStabilityIncrement = 2f
    var baseGroundDefenseMult = 1.5f

    override fun handleConditionAttributes(id: String, ourMarket: MarketAPI) {
        for (suppressedCondition in suppressedConditions) {
            market.suppressCondition(suppressedCondition)
        }

        market.accessibilityMod.modifyFlat(id, baseAccessibilityIncrementObj, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, baseGroundDefenseMultObj, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, baseGroundDefenseIncrementObj, name)
        market.stability.modifyFlat(id, baseStabilityIncrementObj, name)
    }

    override fun unapplyConditionAttributes(id: String, ourMarket: MarketAPI) {
        ourMarket.accessibilityMod.unmodify(id)
        ourMarket.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        ourMarket.stability.unmodify(id)

        for (suppressedCondition in suppressedConditions) {
            market.unsuppressCondition(suppressedCondition)
        }
    }

    override fun createNewHandlerInstance(entity: SectorEntityToken): niko_MPC_satelliteHandlerCore {
        return niko_MPC_derelictSatelliteHandler.createNewHandlerInstance(entity)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        val ourMarket = getMarket() ?: return

        val luddicSupressionValue = getLuddicSupression()
        tooltip.addPara(
            "%s stability",
            10f,
            Misc.getHighlightColor(),
            "+${baseStabilityIncrementObj.toInt()}"
        )

        val convertedAccessibilityBonus = (baseAccessibilityIncrementObj * 100).toInt() //times 100 to convert out of decimal

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            "$convertedAccessibilityBonus%"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            "+${baseGroundDefenseIncrementObj.toInt()}"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            ("+${baseGroundDefenseMultObj}x")
        )

        tooltip.addPara(
            "Danger from asteroid impacts %s.",
            10f,
            Misc.getHighlightColor(),
            "nullified"
        )

        if (ourMarket.hasCustomControls()) {
            tooltip.addPara(
                "Due to the satellites defaulting to hostile behavior when considering a fleet with no transponder " +
                        "(excluding programmed exceptions and manual overrides), customs control is significantly easier, " +
                        "leading to %s being reduced by %s.",
                10f,
                Misc.getHighlightColor(),
                "effective luddic path interest", luddicSupressionValue.toString()
            )
        } else {
            tooltip.addPara(
                "While the satellites would normally make it more difficult for pathers to approach the planet, and thus reduce" +
                " effective pather interest, the market's status as a %s requires a firmware hack to allow anyone and anything unidentified into orbit.",
                10f,
                Misc.getHighlightColor(),
                "free port"
            )
        }

        tooltip.addPara(
            ("Due to the fact that fleets are %s, " +
                    "and the fact that %s, " +
                    "orbital defenses are %s."),
            10f,
            Misc.getHighlightColor(),
            "forced to break into the satellite orbit to interact with the planet",
            "any battle that takes place very close to the planet will have satellites interfere",
            "enhanced"
        )

        if (niko_MPC_settings.nexLoaded) {
            tooltip.addPara(
                "The satellites can be used to perform a %s that pins down an entire industry's worth of enemy forces.",
                10f,
                Misc.getHighlightColor(),
                "powerful ground battle ability"
            )
        }
    }
}