package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
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
import data.scripts.MPC_delayedExecution
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor.Companion.addSpecialItems
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.contribution.MPC_changeReason
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContribution
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContributionChangeData
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICAttritionFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedHint
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICShortageFactor
import data.scripts.campaign.skills.MPC_spaceOperations
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_settings
import lunalib.lunaExtensions.getKnownShipSpecs
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.isPatrol
import org.magiclib.kotlin.isWarFleet
import java.awt.Color
import kotlin.math.roundToInt

class MPC_IAIICFobIntel: BaseEventIntel(), CampaignEventListener {

    val affectedMarkets = HashSet<MarketAPI>()
    val checkInterval = IntervalUtil(1f, 1.1f)
    var escalationLevel: Float = 0f
    /** If true, [sanitizeFactionContributions] will be ran on the next advance tick. */
    var sanitizeContributions: Boolean = false
    var abandonedStage: Stage? = null
    val factionContributions = generateContributions()
        get() {
            sanitizeFactionContributions(field)
            return field
        }

    private fun generateContributions(): ArrayList<MPC_factionContribution> {
        val list = ArrayList<MPC_factionContribution>()

        list += MPC_factionContribution(
            Factions.HEGEMONY,
            1.7f,
            removeContribution = {
                    IAIIC -> IAIIC.getKnownShipSpecs().filter { it.hasTag("XIV_bp") || it.hasTag("heg_aux_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            },
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.LUDDIC_CHURCH,
            1.2f,
            removeContribution = {
                    IAIIC -> IAIIC.getKnownShipSpecs().filter { it.hasTag("luddic_church") || it.hasTag("LC_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            },
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.9f,
            removeContribution = null,
            removeNextAction = true,
            requireMilitary = false,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.DIKTAT,
            0.8f,
            removeContribution = {
                    IAIIC -> IAIIC.getKnownShipSpecs().filter { it.hasTag("sindrian_diktat") || it.hasTag("lions_guard") || it.hasTag("LG_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            },
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.TRITACHYON,
            0.7f,
            removeContribution = null,
            removeNextAction = false,
            requireMilitary = false,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.LUDDIC_PATH,
            0.4f,
            removeContribution = null,
            removeNextAction = false,
            requireMilitary = false,
            repOnRemove = -50f
        )

        sanitizeContributions = true
        //sanitizeFactionContributions(list) // no - we should check the next tick
        return list
    }

    private fun sanitizeFactionContributions(contributions: ArrayList<MPC_factionContribution> = factionContributions) {
        val iterator = contributions.iterator()
        while (iterator.hasNext()) {
            val contribution = iterator.next()
            if (!BaseHostileActivityFactor.checkFactionExists(contribution.factionId, contribution.requireMilitary)) {
                contribution.onRemoved(this, true)
                iterator.remove()
            }
        }
    }


    enum class Stage(
        val stageName: String,
        /** If true, losing a faction's pledge to the IAIIC can remove the stage before it fires.*/
        val isExpendable: Boolean = true
    ) {

        START("Start", false),
        FIRST_RAID("IAIIC Raid"),
        SECOND_RAID("IAIIC Raid"),
        FIRST_ESCALATION("Escalation", false),
        THIRD_RAID("IAIIC Raid"),
        BLOCKADE("IAIIC Blockade"),
        SECOND_ESCALATION("Escalation", false),
        FOURTH_RAID("IAIIC Raid"),
        ALL_OR_NOTHING("All-out attack", false);
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

        fun getFleetMultFromContributingFactions(contributions: ArrayList<MPC_factionContribution>): Float {
            var mult = 1f
            for (entry in contributions) {
                mult += entry.fleetMultIncrement
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

        addStage(Stage.FIRST_RAID, 275)
        addStage(Stage.SECOND_RAID, 350)
        addStage(Stage.FIRST_ESCALATION, 425)
        addStage(Stage.THIRD_RAID, 500)
        addStage(Stage.BLOCKADE, 600)
        addStage(Stage.SECOND_ESCALATION, 700)
        addStage(Stage.FOURTH_RAID, 800)

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

        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val opad = 10f

                tooltip.addTitle(stageId.stageName)

                addStageDesc(tooltip, stageId, opad, true)
            }
        }
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
                        "%s, %s, and %s, but %s, leading to faster political attrition - in other words, the IAIIC's benefactors will " +
                        "want the conflict to end even faster.",
                    initPad,
                    Misc.getHighlightColor(),
                    "repair the FOB", "respawn all patrols", "increase size of launched fleets", "also increase the IAIIC's conflict fatigue"
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

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null) return

        if (!isUpdate) return
        val data = getListInfoParam()
        if (data is MPC_factionContributionChangeData) {
            val contribution = data.contribution
            val faction = Global.getSector().getFaction(contribution.factionId)

            when (data.reason) {
                MPC_changeReason.PULLED_OUT -> {
                    val label = info.addPara(
                        "%s pulled out of %s",
                        0f,
                        faction.color,
                        faction.displayName, "IAIIC"
                    )
                    label.setHighlightColors(
                        faction.color,
                        Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).color
                    )
                }

                MPC_changeReason.FACTION_DIED -> {
                    val label = info.addPara(
                        "%s unable to provide support to %s",
                        0f,
                        Misc.getHighlightColor(),
                        faction.displayName, "IAIIC"
                    )
                    label.setHighlightColors(
                        faction.color,
                        Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).color
                    )
                }

                MPC_changeReason.JOINED -> { // TODO: this is not supported yet
                    info.addPara(
                        "%s joins the IAIIC project",
                        0f,
                        Misc.getHighlightColor(),
                        faction.displayName
                    )
                }
            }

            if (contribution.fleetMultIncrement > 0f) {
                val label = info.addPara(
                    "%s fleet-size reduced by %s",
                    0f,
                    Misc.getHighlightColor(),
                    "IAIIC", contribution.getStringifiedFleetsize()
                )
                label.setHighlightColors(
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).color,
                    Misc.getHighlightColor()
                )
            }

            if (abandonedStage != null) {
                val name = abandonedStage!!.stageName
                info.addPara(
                    "Upcoming %s aborted",
                    0f,
                    Misc.getHighlightColor(),
                    name
                )
                abandonedStage = null
            }
        }
    }

    override fun getCommMessageSound(): String? {
        if (isSendingUpdate) {
            return getSoundMajorPosting()
        }
        return getSoundColonyThreat()
    }

    /** Increases amount of fleets launched by the FOB and repairs all industries, but increases conflict fatigue. */
    private fun escalate(amount: Float) {
        val FOB = MPC_fractalCoreFactor.getFOB() ?: return
        FOB.industries.forEach {
            if (it.disruptedDays > 0.5f) {
                it.setDisrupted(0.5f)
            }
        }
        FOB.addSpecialItems()
        escalationLevel += amount
        FOB.reapplyConditions()
        MPC_delayedExecution(
            {
                MPC_spaceOperations.getPatrol(FOB)?.advance(Float.MAX_VALUE)
            },
            0.2f,
            runWhilePaused = false,
            useDays = true
        ).start() // respawn all the fleets
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        if (sanitizeContributions) {
            factionContributions // causes sanitization
            sanitizeContributions = false
        }
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

    /** Removes the next raid, blockade, whatever. Does NOT remove escalation, or the final push. */
    fun removeNextAction() {
        var foundStage: EventStageData? = null

        for (stage in this.stages) {
            val stageId = stage.id as Stage
            if (stage.wasEverReached) continue
            if (stageId.isExpendable) {
                foundStage = stage
                break
            }
        }
        if (foundStage != null) {
            abandonedStage = foundStage.id as Stage?
            stages.remove(foundStage)
        }

        return
    }


    // LISTENER CRAP

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
