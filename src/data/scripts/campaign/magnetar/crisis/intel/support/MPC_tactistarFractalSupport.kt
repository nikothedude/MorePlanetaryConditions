package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.isMilitary
import kotlin.math.roundToInt

class MPC_tactistarFractalSupport: MPC_fractalCrisisSupport() {
    override val tracker: IntervalUtil = IntervalUtil(2f, 3f)

    fun getMarket(): MarketAPI? {
        return Global.getSector().economy.marketsCopy.firstOrNull { it.factionId == Factions.INDEPENDENT && it.isMilitary() } ?:
        Global.getSector().economy.marketsCopy.firstOrNull { it.factionId == Factions.INDEPENDENT }
    }

    override fun createFleet(): CampaignFleetAPI? {
        val market = getMarket() ?: return null
        val colony = getColony() ?: return null

        val light: Int = getCount(PatrolType.FAST)
        val medium: Int = getCount(PatrolType.COMBAT)
        val heavy: Int = getCount(PatrolType.HEAVY)

        val maxMedium: Int = 4
        val maxHeavy: Int = 2

        val picker = WeightedRandomPicker<PatrolType>()
        picker.add(PatrolType.HEAVY, (maxHeavy - heavy).toFloat())
        picker.add(PatrolType.COMBAT, (maxMedium - medium).toFloat())
        //picker.add(PatrolType.FAST, (maxLight - light).toFloat())

        if (picker.isEmpty) return null

        val random = MathUtils.getRandom()
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        val type = picker.pick()
        when (type) {
            PatrolType.FAST -> {
                combat = (3f + random.nextFloat() * 2f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 3f
            }
            PatrolType.COMBAT -> {
                combat = (10f + random.nextFloat() * 3f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 7f
            }

            PatrolType.HEAVY -> {
                combat = (16f + random.nextFloat() * 5f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 10f
                freighter = random.nextFloat().roundToInt() * 10f
            }
        }

        val params = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            Factions.MERCENARY,
            1f,  // quality override
            type.fleetType,
            combat,  // combatPts
            freighter,  // freighterPts
            tanker,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod - since the Lion's Guard is in a different-faction market, counter that penalty
        )
        params.random = random
        params.officerLevelBonus = 3
        params.officerLevelLimit = 7
        params.officerNumberMult = 1.2f
        params.averageSMods = if (type == PatrolType.HEAVY) 2 else 1
        params.modeOverride = Misc.getShipPickMode(market)
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL
        val fleet = FleetFactoryV3.createFleet(params)

        if (fleet == null || fleet.isEmpty) return null

        fleet.setFaction(Factions.PLAYER)
        fleet.isNoFactionInName = true

        val postId = Ranks.POST_PATROL_COMMANDER
        var rankId: String? = when (type) {
            PatrolType.FAST -> Ranks.SPACE_LIEUTENANT
            PatrolType.COMBAT -> Ranks.SPACE_COMMANDER
            PatrolType.HEAVY -> Ranks.SPACE_CAPTAIN
        }

        fleet.commander.postId = postId
        fleet.commander.rankId = rankId

        when (type) {
            PatrolType.FAST -> fleet.name = "Tactistar Light Detachment"
            PatrolType.COMBAT -> fleet.name = "Tactistar Defense Detachment"
            PatrolType.HEAVY -> fleet.name = "Tactistar Heavy Detachment"
        }

        market.containingLocation.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.primaryEntity.location.x, market.primaryEntity.location.y)

        return fleet
    }

    override fun addDesc(info: TooltipMakerAPI) {
        val label = info.addPara(
            "You have purchased a %s, which provides a number of %s fleets.",
            5f,
            Misc.getHighlightColor(),
            "Tactistar defense contract", "elite mercenary"
        )
        label.setHighlightColors(
            Global.getSector().getFaction(Factions.INDEPENDENT).baseUIColor,
            Misc.getHighlightColor()
        )
        info.addPara(
            "They will patrol your space and assist your forces in combat against the %s.",
            0f,
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            "IAIIC"
        )
    }

    override fun getName() = "Tactistar Defense Contract"

    override fun getIcon(): String? {
        return "graphics/factions/crest_tactistar.png"
    }

    override fun getFactionForUIColors(): FactionAPI? {
        return Global.getSector().getFaction(Factions.INDEPENDENT)
    }
}