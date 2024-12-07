package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.NPCHassler
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.getRelativeFactionStrength
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecution
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor.Companion.addSpecialItems
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.respawnAllFleets
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.contribution.MPC_changeReason
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContribution
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContributionChangeData
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICAttritionFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedHint
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICShortageFactor
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_settings
import lunalib.lunaExtensions.getKnownShipSpecs
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.*
import java.awt.Color
import kotlin.math.roundToInt

class MPC_IAIICFobIntel: BaseEventIntel(), CampaignEventListener {

    var removeBlueprintFunctions: HashSet<() -> Unit> = HashSet()
    val affectedMarkets = HashSet<MarketAPI>()
    val checkInterval = IntervalUtil(1f, 1.1f)
    var escalationLevel: Float = 0f
    /** If true, [sanitizeFactionContributions] will be ran on the next advance tick. */
    var sanitizeContributions: Boolean = false
    var abandonedStage: Stage? = null
    val factionContributions = generateContributions()
    var embargoState = EmbargoState.INACTIVE
    fun getFactionContributionsExternal(): ArrayList<MPC_factionContribution> {
        sanitizeFactionContributions(factionContributions)
        return factionContributions
    }

    fun removeContribution(contribution: MPC_factionContribution, becauseFactionDead: Boolean, dialog: InteractionDialogAPI? = null) {
        factionContributions -= contribution
        contribution.onRemoved(this, becauseFactionDead, dialog)
        checkContributionValues()
    }

    private fun checkContributionValues() {
        val fleetSize = getFleetMultFromContributingFactions()

        if (fleetSize <= MIN_FLEET_SIZE_TIL_GIVE_UP) {
            end(MPC_IAIICFobEndReason.LOSS_OF_BENEFACTORS)
        }
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

        START("IAIIC Investigations", false),
        FIRST_RAID("IAIIC Raid"),
        SECOND_RAID("IAIIC Raid"),
        FIRST_ESCALATION("Escalation", false),
        THIRD_RAID("IAIIC Raid"),
        BLOCKADE("IAIIC Blockade"),
        SECOND_ESCALATION("Escalation", false),
        FOURTH_RAID("IAIIC Raid"),
        ALL_OR_NOTHING("All-out attack", false);
    }

    enum class EmbargoState(val isActive: Boolean = false) {
        INACTIVE,
        ACTIVE(true),
        DECAYING(true);
    }

    companion object {
        const val KEY = "\$MPC_IAIICIntel"
        const val PROGRESS_MAX = 1000
        const val FP_PER_POINT = 0.8f
        const val HEGEMONY_CONTRIBUTION = 1.7f
        const val CHURCH_CONTRIBUTION = 1.2f
        /** If overall contribution reaches or falls below this, the event ends. */
        const val MIN_FLEET_SIZE_TIL_GIVE_UP = (HEGEMONY_CONTRIBUTION + CHURCH_CONTRIBUTION) + 1f
        const val BASE_INSPECTION_FP = 200f
        const val DAYS_EMBARGO_LINGERS_FOR = 20f

        fun getFOB(): MarketAPI? {
            return MPC_fractalCoreFactor.getFOB()
        }

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
            val fractalColony = getFractalColony() ?: return 0f
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

    private fun generateContributions(): ArrayList<MPC_factionContribution> {
        val list = ArrayList<MPC_factionContribution>()

        list += MPC_factionContribution(
            Factions.HEGEMONY,
            HEGEMONY_CONTRIBUTION,
            removeContribution = {
                    IAIIC -> IAIIC.getKnownShipSpecs().filter { it.hasTag("XIV_bp") || it.hasTag("heg_aux_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            },
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.LUDDIC_CHURCH,
            CHURCH_CONTRIBUTION,
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

    fun getFleetMultFromContributingFactions(): Float {
        return Companion.getFleetMultFromContributingFactions(getFactionContributionsExternal())
    }

    fun getTotalEmbargoValue(market: MarketAPI?): Float {
        if (!embargoState.isActive) return 0f
        var base = 0f

        for (contribution in factionContributions) {
            val factionId = contribution.factionId
            val faction = Global.getSector().getFaction(factionId) ?: continue

            base += getEmbargoValue(faction, contribution, market)
        }
        return base
    }

    fun getEmbargoValue(faction: FactionAPI, contribution: MPC_factionContribution, market: MarketAPI?): Float {
        if (!embargoState.isActive) return 0f
        var base = 0f
        if (faction.isHostileTo(Global.getSector().playerFaction)) return 0f // already handled
        for (theirMarket in faction.getMarketsCopy().filter { market == null || it.econGroup == market.econGroup }) {
            base += (contribution.baseMarketEmbargoValue * (theirMarket.size - 2))
        }
        return base
    }

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this

        setup()

        val fractalColony = getFractalColony()!!
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

        addStage(Stage.START, 0)

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
            Stage.START -> {}
            Stage.FIRST_RAID -> {
                val FOB = getFOB() ?: return
                val colony = getFractalColony() ?: return end(MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST)
                MPC_IAIICInspectionIntel(FOB, colony, BASE_INSPECTION_FP)
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
                    "${getFractalColony()?.starSystem?.name}", "very strong"
                )
            }
            Stage.ALL_OR_NOTHING -> {
                info.addPara(
                    "Your IntSec's profile of the IAIIC details strong dedication, persistence, but also anxiety. It's likely that, " +
                    "once attrition reaches this critical point, the IAIIC's benefactors will want to %s. This is not to say they will simply give up - " +
                    "rather, out of desperation, a single all-out-strike against %s will take place, seeking to find evidence of your \"exotic intelligence\".",
                    initPad,
                    Misc.getHighlightColor(),
                    "pull out of the project", "${getFractalColony()?.name}"
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

        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
            return
        }

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
        } else if (data is MPC_IAIICFobEndReason) {
            info.addPara(
                "Event over",
                initPad
            )
            when (data) {
                MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST -> {
                    info.addPara(
                        "Target colony no longer exists",
                        initPad
                    )
                }
                MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED -> {
                    info.addPara(
                        "Exotic intelligence seized",
                        initPad
                    )
                }
                MPC_IAIICFobEndReason.FAILED_ALL_OUT_ATTACK -> {
                    info.addPara(
                        "%s suffers crushing defeat!",
                        initPad,
                        getFaction().color,
                        "IAIIC"
                    )
                }
                MPC_IAIICFobEndReason.LOSS_OF_BENEFACTORS -> {
                    info.addPara(
                        "%s crumbles as the sector abandons it!",
                        initPad,
                        getFaction().color,
                        "IAIIC"
                    )
                }
            }
        }
    }

    override fun getCommMessageSound(): String? {
        if (isSendingUpdate) {
            val data = getListInfoParam()
            if (data is MPC_IAIICFobEndReason) {
                if (data.consideredVictory) {
                    return Sounds.REP_GAIN
                } else {
                    return Sounds.REP_LOSS
                }
            }
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
                FOB.respawnAllFleets()
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

        val fractalColony = getFractalColony()
        if (fractalColony == null) {
            end(MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST)
        }
        else if (fractalColony.containingLocation != getFOB()?.containingLocation) {
            end(MPC_IAIICFobEndReason.FRACTAL_COLONY_MOVED)
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
        val fractalSystem = getFractalColony()?.starSystem ?: return
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
        val fractalSystem = getFractalColony()?.starSystem ?: return false
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

    /** Ends the event. Victory, or defeat. */
    fun end(reason: MPC_IAIICFobEndReason, dialog: InteractionDialogAPI? = null) {
        if (dialog == null) {
            sendUpdateIfPlayerHasIntel(reason, false)
        } else {
            sendUpdateIfPlayerHasIntel(reason, dialog.textPanel)
        }

        val fob = MPC_fractalCoreFactor.getFOB()
        dismissCombatFleets()
        if (fob != null) {
            evacuateFob(fob)
        }
        if (reason.consideredVictory) {
            beginCoreUpgrade()
            if (fob != null) {
                fob.removeCondition(niko_MPC_ids.MPC_BENEFACTOR_CONDID)
                DecivTracker.decivilize(fob, false,  false)
            }
        } else {
            val params = DebrisFieldParams(
                500f,
                30f,
                90f,
                0f
            )
            val containingLoc = fob?.containingLocation
            val fobEntity = fob?.primaryEntity
            DecivTracker.decivilize(fob, false,  false)
            if (containingLoc != null && fobEntity != null) {
                val field = containingLoc.addDebrisField(params, MathUtils.getRandom())
                val token = containingLoc.createToken(fobEntity.location)
                token.orbit = fobEntity.orbit.makeCopy()
                field.setCircularOrbit(token, 0f, 0f, 100f)
                fobEntity.fadeAndExpire(1f)
            }
        }
        killIAIIC()
    }

    private fun beginCoreUpgrade() {
        MPC_delayedExecution(
            { MPC_fractalUpgradeIntel() },
            30f,
            false,
            useDays = true
        ).start()
    }

    private fun dismissCombatFleets() {
        val targetColony = MPC_fractalCoreFactor.getFOB() ?: getFractalColony() ?: return
        for (fleet in targetColony.containingLocation.fleets.filter { it.faction.id == niko_MPC_ids.IAIIC_FAC_ID && !it.isTrader() }) {
            val evacLoc = getEvacLoc(targetColony) ?: return fleet.despawn()
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, evacLoc.primaryEntity, Float.MAX_VALUE, "returning to ${evacLoc.name}")
        }

        for (fleet in MPC_fractalCrisisHelpers.getAssistanceFleets()) {
            val despawnLoc = fleet.getSourceMarket()?.primaryEntity ?: Global.getSector().economy.marketsCopy.randomOrNull()?.primaryEntity ?: continue
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, despawnLoc, Float.MAX_VALUE, "returning to ${despawnLoc.name}")
        }
    }

    private fun evacuateFob(fob: MarketAPI) {
        val numOfFleets = fob.size
        var fleetsLeft = numOfFleets
        while (fleetsLeft-- > 0) {
            val evacLoc = getEvacLoc(fob) ?: continue
            val fleet = createEvacFleet(fob)

            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, evacLoc.primaryEntity, Float.MAX_VALUE, "evacuating to ${evacLoc.name}")
        }
    }

    private fun getEvacLoc(fob: MarketAPI): MarketAPI? {
        val tryOne = Global.getSector().economy.marketsCopy.filter { it.factionId == Factions.INDEPENDENT }
        if (tryOne.isNotEmpty()) return tryOne.randomOrNull()
        val tryTwo = Global.getSector().economy.marketsCopy.filter { !it.faction.isHostileTo(niko_MPC_ids.IAIIC_FAC_ID) }
        if (tryTwo.isNotEmpty()) return tryTwo.randomOrNull()

        return Global.getSector().economy.marketsCopy.randomOrNull()
    }

    private fun createEvacFleet(fob: MarketAPI): CampaignFleetAPI {
        val params = FleetParamsV3(
            fob,
            FleetTypes.TRADE_LINER,
            40f,
            100f,
            30f,
            50f,
            150f,
            0f,
            0f
        )
        val fleet = FleetFactoryV3.createFleet(params)
        fleet.name = "Evacuation Fleet"

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.memoryWithoutUpdate["\$MPC_evacFleet"] = true

        return fleet
    }

    private fun killIAIIC() {
        for (market in Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).getMarketsCopy()) {
            market.factionId = Factions.INDEPENDENT
        }
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
        val fractalSystem = getFractalColony()?.starSystem ?: return
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
            val factor = MPC_IAIICMilitaryDestroyedFactor(1 * points)
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
        if (fleet == null) return
        val source = fleet.getSourceMarket() ?: return
        if (source.faction.id != niko_MPC_ids.IAIIC_FAC_ID) return
        if (!fleet.isPatrol()) return

        fleet.addScript(NPCHassler(fleet, source.starSystem))

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
