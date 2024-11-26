package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.getRelativeFactionStrength
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.*
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_factionContribution
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICAttritionFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedHint
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICShortageFactor
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_settings
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.isPatrol
import org.magiclib.kotlin.isWarFleet
import java.awt.Color
import kotlin.math.roundToInt

class MPC_IAIICFobIntel: BaseEventIntel(), CampaignEventListener {

    val affectedMarkets = HashSet<MarketAPI>()
    val checkInterval = IntervalUtil(1f, 1.1f)
    var escalationLevel: Float = 0f
    val factionContributions = HashSet<MPC_factionContribution>()
        get() {
            sanitizeFactionContributions(field)
            return field
        }

    private fun sanitizeFactionContributions(contributions: HashSet<MPC_factionContribution> = factionContributions) {
        val iterator = contributions.iterator()
        while (iterator.hasNext()) {
            val contribution = iterator.next()
            if (!BaseHostileActivityFactor.checkFactionExists(contribution.factionId, contribution.requireMilitary)) {
                contribution.onRemoved(true)
                iterator.remove()
            }
        }
    }


    enum class Stage {
        START,
        FIRST_RAID,
        SECOND_RAID,
        FIRST_ESCALATION,
        THIRD_RAID,
        BLOCKADE,
        SECOND_ESCALATION,
        FOURTH_RAID,
        ALL_OR_NOTHING;
    }

    companion object {
        const val KEY = "\$MPC_IAIICIntel"
        const val PROGRESS_MAX = 1000
        const val FP_PER_POINT = 2

        fun get(): MPC_IAIICFobIntel? {
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICFobIntel
        }

        fun addFactorCreateIfNecessary(factor: EventFactor?, dialog: InteractionDialogAPI?) {
            if (get() == null) {
                MPC_IAIICFobIntel()
            }
            if (get() != null) {
                get()!!.addFactor(factor, dialog)
            }
        }

        fun getIAIICStrengthInSystem(): Float {
            val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return 0f
            return getRelativeFactionStrength(niko_MPC_ids.IAIIC_FAC_ID, fractalColony.starSystem)
        }

        fun computeShipsDestroyedPoints(fleetPointsDestroyed: Float): Int {
            if (fleetPointsDestroyed <= 0) return 0
            var points = (fleetPointsDestroyed / FP_PER_POINT).roundToInt()
            if (points < 1) points = 1
            return points
        }

        fun getFleetMultFromContributingFactions(contributions: MutableSet<MPC_factionContribution>): Float {
            var mult = 1f
            for (entry in contributions) {
                mult += entry.fleetMult
            }
            return mult
        }

    }
    fun getFleetMultFromContributingFactions(): Float {
        return Companion.getFleetMultFromContributingFactions(this.factionContributions)
    }

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this

        setup()

        val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()!!
        AICoreAdmin.get(fractalColony)!!.daysActive = 500f // so it cant be removed anymore
        Global.getSector().intelManager.addIntel(this, false, null)
        Global.getSector().addListener(this)
        isImportant = true
    }

    private fun setup() {
        factors.clear()
        stages.clear()

        setMaxProgress(PROGRESS_MAX)

        addFactor(MPC_IAIICMilitaryDestroyedHint())
        //addFactor(MPC_IAIICTradeDestroyedFactorHint()) // the shortage factor already does this
        addFactor(MPC_IAIICShortageFactor())
        addFactor(MPC_IAIICAttritionFactor())

        addStage(Stage.FIRST_RAID, 200)
        addStage(Stage.SECOND_RAID, 275)
        addStage(Stage.FIRST_ESCALATION, 325)
        addStage(Stage.THIRD_RAID, 400)
        addStage(Stage.BLOCKADE, 500)
        addStage(Stage.SECOND_ESCALATION, 600)
        addStage(Stage.FOURTH_RAID, 700)

        addStage(Stage.ALL_OR_NOTHING, 1000)
    }

    override fun getName(): String {
        return "IAIIC Investigations"
    }

    override fun getBarColor(): Color? {
        var color = getFaction().baseUIColor
        color = Misc.interpolateColor(color, Color.black, 0.25f)
        return color
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_COLONIES)
        tags.add(niko_MPC_ids.IAIIC_FAC_ID)
        return tags
    }

    override fun getStageIconImpl(stageId: Any?): String? {
        val esd = getDataFor(stageId) ?: return null
        return Global.getSettings().getSpriteName("events", "MPC_IAIIC_" + (esd.id as Stage).name)
    }

    override fun getIcon(): String? {
        return Global.getSettings().getSpriteName("events", "MPC_IAIIC_START")
    }

    override fun notifyStageReached(stage: EventStageData?) {
        if (stage == null) return

        when (stage.id) {
            Stage.FIRST_RAID -> {

            }
            Stage.FIRST_ESCALATION, Stage.SECOND_ESCALATION -> escalate(2f)
            Stage.BLOCKADE -> {

            }
            Stage.ALL_OR_NOTHING -> {
                // HERE WE GO
            }
        }
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return

        val small = 0f

        val stage = stageId as? Stage ?: return
        if (isStageActive(stageId)) {
            addStageDesc(info, stage, small, false)
        }
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId !is Stage) return null

        when (stageId) {
            Stage.FIRST_RAID, Stage.SECOND_RAID, Stage.THIRD_RAID, Stage.FOURTH_RAID -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                        val opad = 10f

                        tooltip.addTitle("IAIIC Raid")

                        addStageDesc(tooltip, stageId, opad, true)
                    }
                }
            }
            Stage.FIRST_ESCALATION, Stage.SECOND_ESCALATION -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                        val opad = 10f

                        tooltip.addTitle("Escalation")

                        addStageDesc(tooltip, stageId, opad, true)
                    }
                }
            }

            Stage.BLOCKADE -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                        val opad = 10f

                        tooltip.addTitle("IAIIC Blockade")

                        addStageDesc(tooltip, stageId, opad, true)
                    }
                }
            }

            Stage.ALL_OR_NOTHING -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                        val opad = 10f

                        tooltip.addTitle("All-out attack")

                        addStageDesc(tooltip, stageId, opad, true)
                    }
                }
            }
        }
        return null
    }

    private fun addStageDesc(info: TooltipMakerAPI, stage: Stage, initPad: Float, forTooltip: Boolean) {
        when (stage) {
            Stage.FIRST_RAID, Stage.SECOND_RAID, Stage.THIRD_RAID, Stage.FOURTH_RAID -> {
                info.addPara(
                    "The IAIIC will launch a raid on your colonies with the intent of obtaining (and destroying) your AI cores. This will " +
                    "automatically succeed if your faction is not hostile to the IAIIC.", initPad
                )
            }
            Stage.FIRST_ESCALATION, Stage.SECOND_ESCALATION -> {
                val label = info.addPara(
                    "Feeling the pressure, the IAIIC's benefactors will pour more resources into the project. This will " +
                        "%s and %s, but %s, leading to faster political attrition - in other words, the IAIIC's benefactors will " +
                        "want the conflict to end even faster.",
                    initPad,
                    Misc.getHighlightColor(),
                    "repair the FOB", "increase the number of patrols launched", "also increase the IAIIC's conflict fatigue"
                )
                label.setHighlightColors(Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
            }
            Stage.BLOCKADE -> {
                info.addPara(
                    "The IAIIC will attempt to blockade %s, sending significant fleetpower to secure the system's jumppoints and hopefully " +
                    "force you to \"cooperate\". It is likely the blockading force will be %s, and may require precision strikes to defeat.",
                    initPad,
                    Misc.getHighlightColor(),
                    "${MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem?.name}", "very strong"
                )
            }
            Stage.ALL_OR_NOTHING -> {
                info.addPara(
                    "Your IntSec's profile of the IAIIC details strong dedication, persistence, but also anxiety. It's likely that, " +
                    "once attrition reaches this critical point, the IAIIC's benefactors will want to %s. This is not to say they will simply give up - " +
                    "rather, out of desperation, a single all-out-strike against %s will take place, seeking to find evidence of your \"exotic intelligence\".",
                    initPad,
                    Misc.getHighlightColor(),
                    "pull out of the project", "${MPC_hegemonyFractalCoreCause.getFractalColony()?.name}"
                )
                info.addPara(
                    "The strength of this theoretical attack is a subject of intense debate amongst your brass, but it's estimated" +
                    "to be %s, and an %s to defeat.", 5f,
                    Misc.getHighlightColor(),
                    "very strong", "extreme challenge"
                )
                info.addPara(
                    "If such an attack would be defeated, the IAIIC would have suffered such a crushing defeat that it's likely for them" +
                    "to be %s, leaving behind their material for your use. It would also %s, potentially securing a foothold in the sector for your faction.",
                    5f,
                    Misc.getHighlightColor(),
                    "dissolved on the spot", "send waves throughout the geo-political realm"
                )
            }
        }
    }

    /** Increases amount of fleets launched by the FOB and repairs all industries, but increases conflict fatigue. */
    private fun escalate(amount: Float) {
        val FOB = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return
        FOB.industries.forEach {
            if (it.disruptedDays > 0.5f) {
                it.setDisrupted(0.5f)
            }
        }
        if (FOB.memoryWithoutUpdate[niko_MPC_ids.MPC_IAIIC_ESCALATION_ID] == null) {
            FOB.memoryWithoutUpdate[niko_MPC_ids.MPC_IAIIC_ESCALATION_ID] = 0f
        }
        val existingAmount = FOB.memoryWithoutUpdate.getFloat(niko_MPC_ids.MPC_IAIIC_ESCALATION_ID)
        FOB.memoryWithoutUpdate[niko_MPC_ids.MPC_IAIIC_ESCALATION_ID] = (existingAmount + amount)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        val days = Misc.getDays(amount)
        checkInterval.advance(days)
        val elapsed = checkInterval.intervalElapsed()
        // idk why i have to do this, but this is like a quantum slit bug
        // if you dont define the var, you dont observe it being true in the debugger, so it doesnt work
        // its weird????
        if (elapsed) {
            checkMarketDeficits()
            checkPlayerRep()
        }

        val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()
        if (fractalColony == null) {
            TODO("add a contingency for if the fractal colony is destroyed")
        }
    }

    private fun checkPlayerRep() {
        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return
        if (IAIIC.relToPlayer.rel >= niko_MPC_settings.MAX_IAIIC_REP) {
            IAIIC.setRelationship(Factions.PLAYER, niko_MPC_settings.MAX_IAIIC_REP)
        }
    }

    private fun checkMarketDeficits() {
        for (market in affectedMarkets) {
            assignOrUnassignDeficit(market)
        }
        val fractalSystem = MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem ?: return
        for (market in fractalSystem.getMarketsInLocation()) { // in case markets change hands, we should check for all markets
            assignOrUnassignDeficit(market)
        }
    }

    private fun assignOrUnassignDeficit(market: MarketAPI) {
        if (market.shouldHaveDeficit()) {
            market.addConditionIfNotPresent(niko_MPC_ids.IAIIC_CONDITION_ID)
            affectedMarkets += market
        } else {
            market.removeCondition(niko_MPC_ids.IAIIC_CONDITION_ID)
            affectedMarkets -= market
        }
    }

    private fun MarketAPI.shouldHaveDeficit(): Boolean {
        val fractalSystem = MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem ?: return false
        if (containingLocation != fractalSystem) return false
        if (!isPlayerOwned) return false

        return true
    }

    fun getFaction(): FactionAPI {
        return Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
    }

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        return
    }

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        return
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        return
    }

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (primaryWinner == null) return
        if (battle == null) return
        if (isEnded || isEnding) return
        if (!battle.isPlayerInvolved) return

        val playerFleet = Global.getSector().playerFleet
        val playerLoc = playerFleet.starSystem ?: return
        val fractalSystem = MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem ?: return
        if (!battle.playerSide.contains(primaryWinner)) return
        val isNear = (playerLoc == fractalSystem || Misc.isNear(playerFleet, fractalSystem.location))
        if (!isNear) return

        var fpDestroyed = 0f
        var first: CampaignFleetAPI? = null
        for (otherFleet in battle.nonPlayerSideSnapshot.filter { it.faction.id == niko_MPC_ids.IAIIC_FAC_ID && (it.isWarFleet() || it.isPatrol()) }) {
            //if (!Global.getSector().getPlayerFaction().isHostileTo(otherFleet.getFaction())) continue;
            for (loss in Misc.getSnapshotMembersLost(otherFleet)) {
                fpDestroyed += loss.fleetPointCost.toFloat()
                if (first == null) {
                    first = otherFleet
                }
            }
        }

        val points = computeShipsDestroyedPoints(fpDestroyed)
        if (points > 0) {
            //points = 700;
            val factor = MPC_IAIICMilitaryDestroyedFactor(-1 * points)
            //sendUpdateIfPlayerHasIntel(factor, false); // addFactor now sends update
            addFactor(factor)
        }
    }

    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        return
    }

    override fun reportFleetDespawned(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        return
    }

    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        return
    }

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        return
    }

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI?) {
        return
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        return
    }

    override fun reportPlayerReputationChange(person: PersonAPI?, delta: Float) {
        return
    }

    override fun reportPlayerActivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDeactivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDumpedCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportPlayerDidNotTakeCargo(cargo: CargoAPI?) {
        return
    }

}
