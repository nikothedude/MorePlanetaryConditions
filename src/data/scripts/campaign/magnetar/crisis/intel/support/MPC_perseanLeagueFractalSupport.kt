package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.isMilitary
import java.awt.Color
import kotlin.math.roundToInt

class MPC_perseanLeagueFractalSupport: MPC_fractalCrisisSupport() {
    override val tracker: IntervalUtil = IntervalUtil(2f, 3f)
    val listener = MPC_fractalSupportRepLinker(Factions.PERSEAN, this)
    init {
        Global.getSector().addListener(listener)
    }

    fun getMarket(): MarketAPI? {
        val kazeron = Global.getSector().economy.getMarket("kazeron")
        if (kazeron == null || kazeron.factionId != Factions.PERSEAN) {
            return Global.getSector().economy.marketsCopy.firstOrNull { it.factionId == Factions.PERSEAN && it.isMilitary() } ?:
            Global.getSector().economy.marketsCopy.firstOrNull { it.factionId == Factions.PERSEAN }
        }
        return kazeron
    }

    override fun createFleet(): CampaignFleetAPI? {
        val market = getMarket() ?: return null
        val colony = getColony() ?: return null

        val light: Int = getCount(PatrolType.FAST)
        val medium: Int = getCount(PatrolType.COMBAT)
        val heavy: Int = getCount(PatrolType.HEAVY)

        val maxLight: Int = 5
        val maxMedium: Int = 3
        val maxHeavy: Int = 2

        val picker = WeightedRandomPicker<PatrolType>()
        picker.add(PatrolType.HEAVY, (maxHeavy - heavy).toFloat())
        picker.add(PatrolType.COMBAT, (maxMedium - medium).toFloat())
        picker.add(PatrolType.FAST, (maxLight - light).toFloat())

        if (picker.isEmpty) return null

        val random = MathUtils.getRandom()
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        val type = picker.pick()
        when (type) {
            PatrolType.FAST -> {
                combat = (5f + random.nextFloat() * 2f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 3f
            }
            PatrolType.COMBAT -> {
                combat = (12f + random.nextFloat() * 3f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 7f
            }

            PatrolType.HEAVY -> {
                combat = (20f + random.nextFloat() * 5f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 10f
                freighter = random.nextFloat().roundToInt() * 10f
            }
        }

        val params = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            Factions.PERSEAN,
            null,  // quality override
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
        params.modeOverride = Misc.getShipPickMode(market)
        //params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL
        val fleet = FleetFactoryV3.createFleet(params)

        if (fleet == null || fleet.isEmpty) return null

        //fleet.setFaction(Factions.DIKTAT, true)
        //fleet.isNoFactionInName = true

        val postId = Ranks.POST_PATROL_COMMANDER
        var rankId: String? = when (type) {
            PatrolType.FAST -> Ranks.SPACE_LIEUTENANT
            PatrolType.COMBAT -> Ranks.SPACE_COMMANDER
            PatrolType.HEAVY -> Ranks.SPACE_CAPTAIN
        }

        fleet.commander.postId = postId
        fleet.commander.rankId = rankId

        market.containingLocation.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.primaryEntity.location.x, market.primaryEntity.location.y)

        return fleet
    }

    override fun getName(): String = "League Defense Fleets"
    override fun getTitleColor(mode: ListInfoMode?): Color {
        val isUpdate = getListInfoParam() != null
        return if (isEnding && !isUpdate && mode != ListInfoMode.IN_DESC) {
            Misc.getGrayColor()
        } else Global.getSector().getFaction(Factions.PERSEAN).baseUIColor
    }

    override fun addDesc(info: TooltipMakerAPI) {
        val label = info.addPara(
            "Pursuant to your status as a %s, the %s is sending fleets to help in your fight against the %s.",
            5f,
            Misc.getHighlightColor(),
            "League Member", "Persean League", "IAIIC"
        )
        label.setHighlightColors(
            Misc.getHighlightColor(),
            Global.getSector().getFaction(Factions.PERSEAN).baseUIColor,
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
        )
        info.addPara(
            "They will patrol your space and assist your forces in combat against the %s.",
            0f,
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            "IAIIC"
        )
    }

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.PERSEAN).crest
    }

    override fun getFactionForUIColors(): FactionAPI? {
        return Global.getSector().getFaction(Factions.PERSEAN)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        if (tracker.intervalElapsed()) {
            if (!PerseanLeagueMembership.isLeagueMember()) {
                endAfterDelay()
                return
            }
        }
    }

    override fun notifyEnding() {
        super.notifyEnding()

        Global.getSector().listenerManager.removeListener(listener)
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        super.reportPlayerReputationChange(faction, delta)
        if (state == State.ACTIVE) {
            if (faction == Factions.PERSEAN && Global.getSector().getFaction(Factions.PERSEAN).isHostileTo(Global.getSector().playerFaction)) {
                // we are hostile now
                setStateExternal(State.SUSPENDED_DUE_TO_HOSTILITIES)
                sendUpdateIfPlayerHasIntel(State.SUSPENDED_DUE_TO_HOSTILITIES, null)
            }
        } else if (state == State.SUSPENDED_DUE_TO_HOSTILITIES) {
            if (faction == Factions.PERSEAN && !Global.getSector().getFaction(Factions.PERSEAN).isHostileTo(Global.getSector().playerFaction)) {
                // not hostile anymore
                setStateExternal(State.ACTIVE)
                sendUpdateIfPlayerHasIntel(State.ACTIVE, null)
            }
        }
    }
}