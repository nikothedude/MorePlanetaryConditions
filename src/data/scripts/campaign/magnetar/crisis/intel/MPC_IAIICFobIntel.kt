package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.TableRowClickData
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.NPCHassler
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.getRelativeFactionStrength
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.intel.group.FGBlockadeAction.FGBlockadeParams
import com.fs.starfarer.api.impl.campaign.intel.group.FGRaidAction.FGRaidType
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI.GenericRaidParams
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission.FleetStyle
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.ai.CampaignFleetAI
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.MPC_fractalCoreReactionScript
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor.Companion.addSpecialItems
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause.Companion.getFractalColony
import data.scripts.campaign.magnetar.crisis.contribution.MPC_changeReason
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContribution
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContribution.benefactorData
import data.scripts.campaign.magnetar.crisis.contribution.MPC_factionContributionChangeData
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICAttritionFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedFactor
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICMilitaryDestroyedHint
import data.scripts.campaign.magnetar.crisis.factors.MPC_IAIICShortageFactor
import data.scripts.campaign.magnetar.crisis.intel.allOutAttack.MPC_IAIICAllOutAttack
import data.scripts.campaign.magnetar.crisis.intel.blockade.MPC_IAIICBlockadeFGI
import data.scripts.campaign.magnetar.crisis.intel.bombard.MPC_IAIICBombardFGI
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICSabotageType
import data.scripts.campaign.magnetar.crisis.intel.support.MPC_fractalCrisisSupport
import data.scripts.campaign.magnetar.crisis.intel.support.MPC_fractalSupportFleetAssignmentAI
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.IAIIC_QUEST
import data.utilities.niko_MPC_ids.MPC_FOB_ID
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_mathUtils.prob
import data.utilities.niko_MPC_settings
import indevo.exploration.minefields.MineBeltTerrainPlugin
import indevo.ids.Ids
import lunalib.lunaExtensions.getKnownShipSpecs
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.*
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MPC_IAIICFobIntel(dialog: InteractionDialogAPI? = null): BaseEventIntel(), CampaignEventListener, FleetGroupIntel.FGIEventListener,
    ColonyPlayerHostileActListener {

    var daysLeftTilNextRetaliate: Float = 0f
    var removeBlueprintFunctions: HashSet<Script> = HashSet()
    val affectedMarkets = HashSet<MarketAPI>()
    var disarmedMarkets = HashSet<MarketAPI>()
    val checkInterval = IntervalUtil(1f, 1.1f)
    var escalationLevel: Float = 0f
    /** If true, [sanitizeFactionContributions] will be ran on the next advance tick. */
    var sanitizeContributions: Boolean = false
    var abandonedStage: Stage? = null
    val factionContributions = generateContributions()
    val benefactorData = generateBenefactorData()
    var embargoState = EmbargoState.INACTIVE
    var sabotageRandom = Random()
    var disarmTimeLeft = 0f

    val support: MutableSet<MPC_fractalCrisisSupport> = HashSet()
    var currentAction: BaseIntelPlugin? = null
        get() {
            if (field != null) {
                if (field is GenericRaidFGI) {
                    val FGI = field as GenericRaidFGI
                    if (FGI.isAborted || FGI.isFailed || FGI.isSucceeded || FGI.isEnded || FGI.isEnding) {
                        field = null
                    }
                }
                if (field is RaidIntel) {
                    val raidIntel = field as RaidIntel
                    if (raidIntel.isFailed || raidIntel.isSucceeded || raidIntel.isEnding || raidIntel.isEnded) {
                        field = null
                    }
                }
            }
            return field
        }
    /** If true, reputation can go above hostile for this frame. */
    var acceptingPeaceOneFrame: Boolean = false
    var pulledOut: Int = 0
    /** How long the IAIIC is suffering disrupted command for. */
    var disruptedCommandDaysLeft = 0f

    fun getFactionContributionsExternal(): ArrayList<MPC_factionContribution> {
        sanitizeFactionContributions(factionContributions)
        return factionContributions
    }

    fun removeContribution(contribution: MPC_factionContribution, becauseFactionDead: Boolean, dialog: InteractionDialogAPI? = null, sendMessage: Boolean = true) {
        if (!becauseFactionDead) {
            pulledOut++
        }
        factionContributions -= contribution
        contribution.onRemoved(this, becauseFactionDead, dialog, sendMessage)
        updateDoctrine()
        checkContributionValues()

        val benefactorInfo = benefactorData.firstOrNull { it.id == contribution.contributionId } ?: return
        benefactorData -= benefactorInfo
    }

    private fun checkContributionValues() {
        val fleetSize = getFleetMultFromContributingFactions()

        if (fleetSize <= MIN_FLEET_SIZE_TIL_GIVE_UP) {
            //forceAllOutAttack()
            end(MPC_IAIICFobEndReason.LOSS_OF_BENEFACTORS)
        }
    }

    fun forceAllOutAttack() {
        for (stage in stages.toList()) {
            if (stage.id != Stage.ALL_OR_NOTHING) {
                stages.remove(stage)
            }
        }
        setProgress(999999999)
    }

    fun updateDoctrine() {
        val IAIIC = getFaction()

        for (ship in IAIIC.knownShips.toList()) {
            var contributorHasShip = false
            for (contribution in factionContributions) {
                val faction = Global.getSector().getFaction(contribution.factionId) ?: continue
                if (faction.knowsShip(ship)) {
                    contributorHasShip = true
                    break
                }
            }
            if (contributorHasShip) continue
            IAIIC.removeKnownShip(ship)
        }
    }

    private fun sanitizeFactionContributions(contributions: ArrayList<MPC_factionContribution> = factionContributions) {
        val iterator = contributions.iterator()
        while (iterator.hasNext()) {
            val contribution = iterator.next()
            if (contribution.contributorExists?.run() == false) {
                contribution.onRemoved(this, true)
                iterator.remove()
            }
        }
    }


    enum class Stage(
        val stageName: String,
        val spriteId: String,
        /** If true, losing a faction's pledge to the IAIIC can remove the stage before it fires.*/
        val isExpendable: Boolean = true,
        val canBeSubstituted: Boolean = false
    ) {
        START("IAIIC Investigations", "MPC_IAIIC_START", false),

        // targets military infrastructure in-system
        FIRST_BOMBARDMENT("Targeted Bombardments", "MPC_IAIIC_BOMBARDMENT", canBeSubstituted = true),
        // randomly rolled sabotage action(?)
        FIRST_SABOTAGE("Sabotage", "MPC_IAIIC_SABOTAGE"),
        FIRST_ESCALATION("Escalation", "MPC_IAIIC_ESCALATION", false),
        SECOND_BOMBARDMENT("Targeted Bombardments", "MPC_IAIIC_BOMBARDMENT", canBeSubstituted = true),
        SECOND_SABOTAGE("Sabotage", "MPC_IAIIC_SABOTAGE"),
        SECOND_ESCALATION("Escalation", "MPC_IAIIC_ESCALATION", false),
        BLOCKADE("Blockade", "MPC_IAIIC_BLOCKADE"),
        THIRD_BOMBARDMENT("Targeted Bombardments", "MPC_IAIIC_BOMBARDMENT", canBeSubstituted =  true),
        ALL_OR_NOTHING("All-out attack", "MPC_IAIIC_ALL_OR_NOTHING", false);

        fun getActualName(isHostile: Boolean): String {
            if (canBeSubstituted && !isHostile) return "Sabotage" else return stageName
        }

        fun getSprite(isHostile: Boolean): String {
            if (canBeSubstituted && !isHostile) {
                return Global.getSettings().getSpriteName("events", "MPC_IAIIC_SABOTAGE")
            }
            return Global.getSettings().getSpriteName("events", spriteId)
        }
    }

    enum class EmbargoState(val isActive: Boolean = false) {
        INACTIVE,
        ACTIVE(true),
        DECAYING(true); // after youve sued for peace, it lingers for a while
    }

    enum class PeacePossibility(val canSueForPeace: Boolean) {
        YES(true),
        ALREADY_AT_PEACE(false),
        HOSTILITIES_ACTIVE(false)
    }

    companion object {
        const val GLOBAL_SABOTAGE_MULT = 1f
        const val GLOBAL_FLEETSIZE_MULT = 1f
        const val DEFAULT_DISARM_TIME = 60f
        const val DEFAULT_COMMAND_DISRUPTION_DAYS = 90f
        const val DISARMAMENT_FLEET_SIZE_MULT = 0.25f
        const val DISARMAMENT_PREMATURE_DAYS = 10f
        const val KEY = "\$MPC_IAIICIntel"
        const val RETALIATE_COOLDOWN_DAYS = 0f
        const val PROGRESS_MAX = 1000
        const val FP_PER_POINT = 0.1f
        const val HEGEMONY_CONTRIBUTION = 1.5f
        const val CHURCH_CONTRIBUTION = 1.2f
        /** If overall contribution reaches or falls below this, the event ends. */
        const val MIN_FLEET_SIZE_TIL_GIVE_UP = (HEGEMONY_CONTRIBUTION + CHURCH_CONTRIBUTION) + 1f
        const val DAYS_EMBARGO_LINGERS_FOR = 20f

        const val BASE_SABOTAGE_RESISTANCE = 7f
        const val SABOTAGE_SUBVERSION_CHANCE_PER_OPERATIVE_LEVEL = 11f
        const val MAX_OPERATIVE_RESISTANCE = 85f

        fun getFOB(): MarketAPI? {
            return MPC_fractalCoreFactor.getFOB()
        }

        fun get(): MPC_IAIICFobIntel? {
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICFobIntel
        }

        fun addFactorCreateIfNecessary(factor: EventFactor?, dialog: InteractionDialogAPI?) {
            if (get() == null) {
                MPC_IAIICFobIntel(dialog)
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
            return points.coerceAtMost(50)
        }

        fun getFleetMultFromContributingFactions(contributions: ArrayList<MPC_factionContribution>): Float {
            var mult = 1f
            for (entry in contributions) {
                mult += entry.fleetMultIncrement * GLOBAL_FLEETSIZE_MULT
            }
            return mult
        }

        fun getSabotageMultFromContributingFactions(contributions: ArrayList<MPC_factionContribution>): Float {
            var mult = 1f
            for (entry in contributions) {
                mult += entry.sabotageMultIncrement * GLOBAL_SABOTAGE_MULT
            }
            if (niko_MPC_settings.astralAscensionEnabled) {
                mult *= 2f
            }

            return mult
        }

        fun getMilitaryTargets(addFractal: Boolean = true, includeHeavyIndustry: Boolean = true, includePatrolHQ: Boolean = false): MutableSet<MarketAPI> {
            val targets = HashSet<MarketAPI>()

            if (addFractal) {
                val fractalColony = getFractalColony() ?: return targets
                targets += fractalColony
            }
            val FOB = getFOB() ?: return targets
            for (market in FOB.starSystem.getMarketsInLocation()) {
                if (!isValidMilitaryTarget(market, includeHeavyIndustry, includePatrolHQ)) continue
                targets += market
            }

            return targets
        }

        fun getGenericTargets(): MutableSet<MarketAPI> {
            val targets = HashSet<MarketAPI>()

            val fractalColony = getFractalColony() ?: return targets
            targets += fractalColony

            for (market in fractalColony.starSystem.getMarketsInLocation()) {
                if (!marketIsEnemy(market)) continue
                targets += market
            }

            return targets
        }

        fun isValidMilitaryTarget(market: MarketAPI, includeHeavyIndustry: Boolean = true, includePatrolHQ: Boolean = false): Boolean {
            if (!marketIsEnemy(market)) return false
            if (!market.isMilitary()) return false
            if (includePatrolHQ && !market.industries.any { it is MilitaryBase }) return false
            if (includeHeavyIndustry && !market.hasHeavyIndustry()) return false

            return true
        }

        fun marketIsEnemy(market: MarketAPI): Boolean {
            return (market.isPlayerOwned || (market.faction.isHostileTo(getFaction())))
        }

        fun getFaction(): FactionAPI {
            return Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
        }

        fun getPeacePossibility(): PeacePossibility {
            val intel = get()
            if (intel != null) {
                if (intel.currentAction != null) return PeacePossibility.HOSTILITIES_ACTIVE
                if (!intel.isHostile()) return PeacePossibility.ALREADY_AT_PEACE
            }
            return PeacePossibility.YES
        }
    }

    private fun generateBenefactorData(): ArrayList<benefactorData> {
        val list = ArrayList<benefactorData>()

        for (contribution in factionContributions.filter { it.addBenefactorInfo }) {
            list += benefactorData(
                contribution.contributionId,
                contribution.factionName,
                contribution.bulletColor,
                contribution.benefactorSuffix
            )
        }

        list += benefactorData(
            Factions.PERSEAN,
            "Persean League",
            Global.getSector().getFaction(Factions.PERSEAN).baseUIColor,
            "Possible"
        )

        return list
    }

    private fun generateContributions(): ArrayList<MPC_factionContribution> {
        val list = ArrayList<MPC_factionContribution>()

        class HegeRemovalScript: RemoveContributionScript() {
            override fun run() {
                val IAIIC = getIAIIC()
                IAIIC.getKnownShipSpecs().filter { it.hasTag("XIV_bp") || it.hasTag("heg_aux_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            }
        }
        list += MPC_factionContribution(
            Factions.HEGEMONY,
            HEGEMONY_CONTRIBUTION,
            0.3f,
            removeContribution = HegeRemovalScript(),
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )
        Global.getSector().importantPeople.getPerson(People.DAUD).makeImportant(IAIIC_QUEST)

        class ChurchRemovalScript: RemoveContributionScript() {
            override fun run() {
                val IAIIC = getIAIIC()
                IAIIC.getKnownShipSpecs().filter { it.hasTag("luddic_church") || it.hasTag("LC_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            }
        }
        list += MPC_factionContribution(
            Factions.LUDDIC_CHURCH,
            CHURCH_CONTRIBUTION,
            0f,
            removeContribution = ChurchRemovalScript(),
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -40f
        )

        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.7f,
            0.05f,
            null,
            contributionId = "tactistar",
            benefactorSuffix = "Branch on Culann",
            factionName = "Tactistar"
        )
        val culann = Global.getSector().economy.getMarket("culann")
        culann.makeStoryCritical(IAIIC_QUEST)
        culann.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.TACTISTAR_REP))

        class BaetisExistsScript(factionId: String, requireMilitary: Boolean): MPC_factionContribution.ContributorExistsScript(factionId, requireMilitary) {
            override fun run(): Boolean {
                return Global.getSector().economy.getMarket("baetis")?.isInhabited() ?: return false
            }
        }
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.4f,
            0.05f,
            null,
            contributorExists = BaetisExistsScript(Factions.INDEPENDENT, false),
            contributionId = "thehammer",
            benefactorSuffix = "Baetis",
            factionName = "The Hammer"
        )
        //Global.getSector().importantPeople.getPerson(MPC_People.HAMMER_REP).makeImportant("\$MPC_IAIICquest")
        //Global.getSector().economy.getMarket("baetis")?.commDirectory?.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HAMMER_REP))
        //Global.getSector().economy.getMarket("baetis").makeStoryCritical(IAIIC_QUEST)
        Global.getSector().economy.getMarket("baetis").primaryEntity.makeImportant(IAIIC_QUEST)

        class BlackKnifeExistsScript(factionId: String, requireMilitary: Boolean): MPC_factionContribution.ContributorExistsScript(factionId, requireMilitary) {
            override fun run(): Boolean {
                return Global.getSector().economy.getMarket("qaras")?.isInhabited() ?: return false
            }
        }
        Global.getSector().economy.getMarket("qaras").primaryEntity.makeImportant(IAIIC_QUEST)
        list += MPC_factionContribution(
            Factions.PIRATES,
            0.1f,
            0.4f,
            null,
            contributorExists = BlackKnifeExistsScript(Factions.PIRATES, false),
            contributionId = "blackknife",
            benefactorSuffix = "Qaras",
            factionName = "Blackknife"
        )
        class MMMCExistsScript(factionId: String, requireMilitary: Boolean): MPC_factionContribution.ContributorExistsScript(factionId, requireMilitary) {
            override fun run(): Boolean {
                return Global.getSector().economy.getMarket("new_maxios")?.isInhabited() ?: return false
            }
        }
        Global.getSector().economy.getMarket("new_maxios").commDirectory.addPerson(MPC_People.getImportantPeople()[MPC_People.MMMC_REP])
        MPC_People.getImportantPeople()[MPC_People.MMMC_REP]?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.5f,
            0f,
            null,
            contributorExists = MMMCExistsScript(Factions.INDEPENDENT, false),
            benefactorSuffix = "Nova Maxios",
            contributionId = "mmmc",
            factionName = "MMMC"
        )
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.3f,
            0.6f,
            null,
            bulletColor = Global.getSector().playerFaction.baseUIColor,
            contributorExists = null, // exists on one of your planets
            addBenefactorInfo = false,
            contributionId = "voidsun",
            factionName = "Voidsun"
        )
        class AgreusExistsScript(factionId: String, requireMilitary: Boolean): MPC_factionContribution.ContributorExistsScript(factionId, requireMilitary) {
            override fun run(): Boolean {
                return Global.getSector().economy.getMarket("agreus")?.isInhabited() ?: return false
            }
        }
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.4f,
            0f,
            null,
            benefactorSuffix = "Agreus",
            contributorExists = AgreusExistsScript(Factions.INDEPENDENT, false),
            contributionId = "agreus",
            factionName = "IIT&S"
        )
        Global.getSector().importantPeople.getPerson(People.IBRAHIM).makeImportant(IAIIC_QUEST)
        class AlimarExistsScript(factionId: String, requireMilitary: Boolean): MPC_factionContribution.ContributorExistsScript(factionId, requireMilitary) {
            override fun run(): Boolean {
                return Global.getSector().economy.getMarket("ailmar")?.isInhabited() ?: return false
            }
        }
        val ailmar = Global.getSector().economy.getMarket("ailmar")
        ailmar?.admin?.makeImportant(IAIIC_QUEST)
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.3f,
            0.1f,
            null,
            contributorExists = AlimarExistsScript(Factions.INDEPENDENT, false),
            contributionId = "ailmar",
            factionName = "Ailmar"
        )
        list += MPC_factionContribution(
            Factions.INDEPENDENT,
            0.2f,
            0.1f,
            null,
            addBenefactorInfo = false,
            contributionId = "miscIndependent",
            factionName = "Misc. Independent"
        )

        class DiktatRemovalScript: RemoveContributionScript() {
            override fun run() {
                val IAIIC = getIAIIC()
                IAIIC.getKnownShipSpecs().filter { it.hasTag("sindrian_diktat") || it.hasTag("lions_guard") || it.hasTag("LG_bp") }.forEach { spec -> IAIIC.removeKnownShip(spec.hullId) }
            }
        }
        list += MPC_factionContribution(
            Factions.DIKTAT,
            0.7f,
            0.3f,
            removeContribution = DiktatRemovalScript(),
            removeNextAction = true,
            requireMilitary = true,
            repOnRemove = -50f
        )

        list += MPC_factionContribution(
            Factions.TRITACHYON,
            0.8f,
            1f,
            removeContribution = null,
            removeNextAction = false,
            requireMilitary = false,
            repOnRemove = -40f
        )
        list += MPC_factionContribution(
            Factions.LUDDIC_PATH,
            0.3f,
            0.8f,
            removeContribution = null,
            removeNextAction = false,
            requireMilitary = false,
            repOnRemove = -50f
        )

        sanitizeContributions = true
        //sanitizeFactionContributions(list) // no - we should check the next tick
        return list
    }

    fun getContributionById(id: String): MPC_factionContribution? {
        return factionContributions.firstOrNull { it.contributionId == id }
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
        Global.getSector().intelManager.addIntel(this, false, dialog?.textPanel)
        Global.getSector().addListener(this)
        MPC_IAIICInspectionPrepIntel(this)
        isImportant = true

        class IndieContribAdderScript(): EveryFrameScript {
            val interval = IntervalUtil(0.1f, 0.1f)

            override fun isDone(): Boolean = false

            override fun runWhilePaused(): Boolean = false

            override fun advance(amount: Float) {
                interval.advance(amount)
                if (interval.intervalElapsed()) {
                    MPC_indieContributionIntel.get(true)
                }
            }
        }
        Global.getSector().addScript(IndieContribAdderScript())
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

        addStage(Stage.FIRST_BOMBARDMENT, 150)
        addStage(Stage.FIRST_SABOTAGE, 250)
        addStage(Stage.FIRST_ESCALATION, 375)
        addStage(Stage.SECOND_BOMBARDMENT, 450)

        addStage(Stage.SECOND_ESCALATION, 670)
        addStage(Stage.BLOCKADE, 700)
        addStage(Stage.THIRD_BOMBARDMENT, 800)
        //addStage(Stage.FOURTH_SABOTAGE, 700)

        addStage(Stage.ALL_OR_NOTHING, 1000)

        //stages.forEach { it.isOneOffEvent = true }
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
        return (esd.id as Stage).getSprite(isHostile())
    }

    override fun getIcon(): String? {
        return Global.getSettings().getSpriteName("events", "MPC_IAIIC_ICON")
    }

    override fun notifyStageReached(stage: EventStageData?) {
        if (stage == null) return

        if (stage.id is Stage) {
            val id = stage.id as Stage
            if (id.canBeSubstituted && !isHostile()) {
                return sabotage()
            }
        }

        when (stage.id) {
            Stage.START -> {}
            Stage.FIRST_BOMBARDMENT, Stage.SECOND_BOMBARDMENT, Stage.THIRD_BOMBARDMENT -> {
                startBombardment()
            }
            Stage.FIRST_ESCALATION, Stage.SECOND_ESCALATION -> escalate(1f)
            Stage.BLOCKADE -> {
                startBlockade()
            }
            Stage.FIRST_SABOTAGE, Stage.SECOND_SABOTAGE -> {
                Global.getSector().memoryWithoutUpdate["\$MPC_voidsunCanSpawnNow"] = true
                sabotage()
            }
            Stage.ALL_OR_NOTHING -> {
                startAllOutAttack()
                // HERE WE GO
            }
        }
    }

    fun isHostile(): Boolean {
        return getFaction().isHostileTo(Global.getSector().playerFaction)
    }

    private fun sabotage() {
        val sabotage = MPC_IAIICSabotageType.addApplicableSabotage(sabotageRandom)
        if (sabotage.isNotEmpty()) {
            sendUpdateIfPlayerHasIntel(sabotage, false)
        }
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return

        val small = 0f

        val stage = stageId as? Stage ?: return
        if (isStageActive(stageId)) {
            if (stageId == Stage.START) {
                val colony = getFractalColony() ?: return
                val sys = colony.starSystem
                val systemName = sys.nameWithNoType
                val FOBString = getFOB()?.name

                info.addPara("The IAIIC has set up " +
                    "a base in %s, using it as a launchpad to harass and raid your citizenship.",
                    0f,
                    Misc.getHighlightColor(),
                    systemName
                )
                info.addPara(
                    "With the support of certain major factors in the sector, the IAIIC is a %s, and has the potential to become even more threatening. " +
                    "However, if this support was to be stripped away, the IAIIC may become little more than a nuisance.",
                    0f,
                    Misc.getNegativeHighlightColor(),
                    "major threat"
                )
                addBenefactorSection(info)
                info.addPara(
                    "Avenues of diplomacy exist outside violence; less \"motivated\" polities may be amenable to a favor or two.",
                    0f
                )

                val targetW = 150f
                val fobW = 80f
                info.beginTable(
                    factionForUIColors, 20f,
                    "Target", targetW,
                    "IAIIC Base", fobW
                )
                info.addTableHeaderTooltip(0, (object: BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                        tooltip?.addPara(
                            "The target of the bulk of the IAIIC's suspicion." +
                            "" +
                            "\n\nMore likely to be targeted by raids and inspections - likely due to your \"creative\" choice of %s.",
                            0f,
                            Misc.getHighlightColor(),
                            "colony administrator"
                        )
                    }
                }))
                info.addTableHeaderTooltip(1, (object: BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                        val label = tooltip?.addPara(
                            "The launchpad of the IAIIC into your space." +
                                    "" +
                                "\n\nDestroying or taking over this market will unceremoniously end the IAIIC's efforts - though that's easier said than done." +
                                "\n\nBe warned; MilSec suggests sleeper cells of IAIIC saboteurs in our space ready to %s in retaliation of any %s we take against %s. \n" +
                                "Additionally, these sleeper cells may become active during raids and %s, causing %s.",
                            0f,
                            Misc.getNegativeHighlightColor(),
                            "sabotage our colonies", "directly hostile actions", "${getFOB()?.name}", "sabotage repair yards", "repairs to take time"
                        )
                        label?.setHighlightColors(Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), getFaction().baseUIColor, Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
                    }
                }))
                info.makeTableItemsClickable()

                info.addRowWithGlow(
                    Alignment.MID, Misc.getBasePlayerColor(), colony.name,
                    Alignment.MID, getFaction().color, FOBString
                )
                info.addTooltipToAddedRow(object:BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                        val w = tooltip.widthSoFar
                        val h = (w / 1.6f).roundToInt().toFloat()
                        tooltip.addSectorMap(w, h, sys, 0f)
                        tooltip.addPara("Click to open map", Misc.getGrayColor(), 5f)
                    }
                }, TooltipLocation.LEFT, false)
                info.setIdForAddedRow(getFOB())
                info.addTable("None", -1, 5f)
                info.addSpacer(3f)
                if (disruptedCommandDaysLeft > 0f) {
                    info.addPara("The destruction of the recent blockade's command fleet has severely damaged the IAIIC command structure.", 5f)
                    info.addPara(
                        "The IAIIC military is currently %s, and %s are %s. This will last for %s more days.", 5f,
                        Misc.getHighlightColor(),
                        "crippled", "inspections", "postponed", "${disruptedCommandDaysLeft.roundToInt()}"
                    )
                    info.addSpacer(3f)
                }
            }
            addStageDesc(info, stage, small, false)
        }
    }

    override fun afterStageDescriptions(main: TooltipMakerAPI?) {
        super.afterStageDescriptions(main)

        val width = barWidth
        val color = Misc.getStoryOptionColor()
        val dark = Misc.getStoryDarkColor()
        val bw = 100f
        val button = addGenericButton(main, bw, color, dark, "Escalate", HostileActivityEventIntel.BUTTON_ESCALATE)
        val inset = width - bw

        //inset = 0f;
        button.getPosition().setXAlignOffset(inset)
        main!!.addSpacer(0f).getPosition().setXAlignOffset(-inset)
        if (currentAction != null) {
            button.isEnabled = false

            main!!.addTooltipTo(object : TooltipCreator {
                override fun isTooltipExpandable(tooltipParam: Any?): Boolean {
                    return false
                }

                override fun getTooltipWidth(tooltipParam: Any?): Float {
                    return 450f
                }

                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                    tooltip.addPara(
                        "Only available when a hostile action is not ongoing.", 0f
                    )
                }
            }, button, TooltipLocation.BELOW)
        }
    }

    override fun storyActionConfirmed(buttonId: Any?, ui: IntelUIAPI) {
        if (buttonId === HostileActivityEventIntel.BUTTON_ESCALATE) {
            ui.recreateIntelUI()
        }
    }

    override fun getButtonStoryPointActionDelegate(buttonId: Any?): StoryPointActionDelegate? {
        if (buttonId === HostileActivityEventIntel.BUTTON_ESCALATE) {
            val params = StoryOptionParams(
                null, 1, "escalateCrisis",
                Sounds.STORY_POINT_SPEND_INDUSTRY,
                "Escalated IAIIC crisis"
            )
            return object : BaseOptionStoryPointActionDelegate(null, params) {
                override fun confirm() {
                    setProgress(999999999)
                }

                override fun getTitle(): String? {
                    return null
                }

                override fun createDescription(info: TooltipMakerAPI) {
                    info.setParaInsigniaLarge()
                    info.addPara(
                        "Take certain actions to provoke the IAIIC further. Forces attrition to escalate to the %s.",
                        -10f,
                        Misc.getHighlightColor(),
                        "next stage"
                    )
                    info.addSpacer(20f)
                    super.createDescription(info)
                }
            }
        }
        return null
    }

    private fun addBenefactorSection(info: TooltipMakerAPI) {
        info.addPara("Probable benefactors:", 10f)
        info.setBulletedListMode(BaseIntelPlugin.BULLET)
        for (entry in benefactorData) {
            entry.addBullet(info)
        }
        /*for (entry in MPC_benefactorDataStore.get().probableBenefactors) {
            entry.addBullet(info)
        }*/
        info.setBulletedListMode(null)
    }

    override fun tableRowClicked(ui: IntelUIAPI, data: TableRowClickData) {
        if (data.rowId is MarketAPI) {
            ui.showOnMap((data.rowId as MarketAPI).primaryEntity)
        }
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId !is Stage) return null

        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val opad = 10f

                tooltip.addTitle(stageId.getActualName(isHostile()))

                addStageDesc(tooltip, stageId, opad, true)
            }
        }
    }

    private fun addStageDesc(info: TooltipMakerAPI, stage: Stage, initPad: Float, forTooltip: Boolean) {
        if (stage.canBeSubstituted && !isHostile()) {
            return addSabotageDesc(info, stage, initPad, true)
        }

        when (stage) {
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
            Stage.FIRST_BOMBARDMENT, Stage.SECOND_BOMBARDMENT, Stage.THIRD_BOMBARDMENT -> {
                info.addPara(
                    "The IAIIC will launch a targeted bombardment against in-system military infrastructure, or civilian if " +
                    "no military presence exists. %s.",
                    5f,
                    Misc.getNegativeHighlightColor(),
                    "The colony harboring the fractal core will always be targeted"
                )
            }
            Stage.FIRST_SABOTAGE, Stage.SECOND_SABOTAGE -> {
                return addSabotageDesc(info, stage, initPad, false)
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

            else -> {}
        }
    }

    private fun addSabotageDesc(info: TooltipMakerAPI, stage: Stage, initPad: Float, isSubstituted: Boolean) {
        info.addPara(
            "The IAIIC will launch covert actions against your faction, causing varying levels of disorder amongst your colonies.",
            5f
        )
        val stringAndColor = getSabotagePowerString()
        info.addPara(
            "The IAIIC delegates it's covert actions to the more cunning members of it's benefactory. As it stands, IntSec estimates " +
            "the threat of subterfuge to be %s.",
            5f,
            stringAndColor.second,
            stringAndColor.first
        )
        if (niko_MPC_settings.nexLoaded) {
            info.addPara(
                "%s will passively act to prevent sabotage against the market they are stationed on, but cannot be relied upon - even if highly skilled.",
                5f,
                Misc.getHighlightColor(),
                "Operatives"
            )
        }

        if (isSubstituted) {
            info.addPara(
                "If your faction was to turn hostile against the IAIIC, this stage would transform into a %s.",
                5f,
                Misc.getHighlightColor(),
                stage.stageName
            )
        }
    }

    fun getSabotagePowerString(): Pair<String, Color> {
        val strength = getSabotageMultFromContributingFactions(factionContributions)

        if (strength >= 2.5f) {
            return Pair("extreme", Misc.getNegativeHighlightColor())
        }
        if (strength >= 2f) {
            return Pair("severe", Misc.getNegativeHighlightColor())
        }
        if (strength >= 1.5f) {
            return Pair("moderate", Misc.getHighlightColor())
        }
        if (strength <= 1.5f) {
            return Pair("minor", Misc.getPositiveHighlightColor())
        }
        return Pair("massive", Misc.getNegativeHighlightColor())
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null) return

        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
            return
        }

        if (!isUpdate) return
        val data = getListInfoParam()
        if (data is EventStageData) {
            info.addPara(
                "Stage reached: %s",
                0f,
                Misc.getHighlightColor(),
                (data.id as Stage).getActualName(isHostile())
            )
        }
        else if (data is Collection<*> && data.any { it is MPC_IAIICSabotageType.MPC_IAIICSabotageResultData }) {
            val sabotageList = data as Collection<MPC_IAIICSabotageType.MPC_IAIICSabotageResultData>
            val rand = data.random()
            val randInstance = rand.sabotage
            info.addPara(
                "Markets sabotaged: %s",
                0f,
                Misc.getHighlightColor(),
                randInstance.baseName
            )
            val oldMode = info.bulletedListPrefix
            info.setBulletedListMode("${BaseIntelPlugin.INDENT}${oldMode}")
            for (entry in sabotageList) {
                var baseString = "${entry.target.name}"
                var highlights: String? = null
                if (!entry.result.isSuccess) {
                    highlights = "(${entry.result.message})"
                    baseString += " $highlights"
                }

                val label = info.addPara(
                    baseString,
                    5f
                )
                if (highlights != null) {
                    label.setHighlight(highlights)
                    label.setHighlightColors(Misc.getHighlightColor())
                }
            }
            info.setBulletedListMode(oldMode)
        }
        else if (data is RetaliateReason) {
            when (data) {
                RetaliateReason.ATTACKED_FOB -> {
                    info.addPara(
                        "Retaliation imminent due to hostile actions!",
                        0f
                    )
                }

                RetaliateReason.BETRAYED_LINDUNBERG -> {
                    info.addPara(
                        "Retaliation imminent due to an enraged aristocrat!",
                        0f
                    )
                }

                else -> {
                    info.addPara(
                        "Covert action imminent!",
                        0f
                    )
                }
            }
        }
        else if (data is MPC_factionContributionChangeData) {
            val contribution = data.contribution
            val faction = Global.getSector().getFaction(contribution.factionId)
            val name = contribution.factionName

            when (data.reason) {
                MPC_changeReason.PULLED_OUT -> {
                    val label = info.addPara(
                        "%s pulls out of %s",
                        0f,
                        faction.color,
                        name, "IAIIC"
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
                        contribution.factionName, "IAIIC"
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
                        contribution.factionName
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
            if (contribution.sabotageMultIncrement > 0f) {
                val label = info.addPara(
                    "%s sabotage potential reduced by %s",
                    0f,
                    Misc.getHighlightColor(),
                    "IAIIC", contribution.getStringifiedSabotagePotential()
                )
                label.setHighlightColors(
                    getFaction().baseUIColor,
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
                        "%s crumbles as the sector abandons it! (NOTE-this will be replaced later by forcing the all out attack)",
                        initPad,
                        getFaction().color,
                        "IAIIC"
                    )
                }
                MPC_IAIICFobEndReason.FOB_LOST -> {
                    info.addPara(
                        "%s loses their grasp on the sector!",
                        initPad,
                        getFaction().color,
                        "IAIIC"
                    )
                }

                else -> {}
            }
        } else if (data is String) {
            info.addPara(data, initPad)
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
            else if (data is EventStageData) {
                return getSoundColonyThreat()
            }
            return getSoundMajorPosting()
        }
        return "MPC_IAIIC_INTEL_START"
    }

    /** Increases amount of fleets launched by the FOB and repairs all industries, but increases conflict fatigue. */
    private fun escalate(amount: Float) {
        val FOB = MPC_fractalCoreFactor.getFOB() ?: return
        FOB.industries.forEach {
            if (it.disruptedDays > 0f) {
                it.setDisrupted(0f)
            }
        }
        escalationLevel += amount
        FOB.addSpecialItems()
        FOB.reapplyConditions()
        class MPC_respawnFleets(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
            override fun executeImpl() {
                val intel = MPC_TTContributionIntel.get()
                intel?.state = MPC_TTContributionIntel.State.RESOLVE
                intel?.sendUpdateIfPlayerHasIntel(MPC_TTContributionIntel.State.RESOLVE, false, false)
            }
        }
        MPC_respawnFleets(IntervalUtil(0.2f, 0.2f)).start()
        val recentUnrest: RecentUnrest = RecentUnrest.get(FOB) ?: return
        if (recentUnrest.penalty > 1) {
            val toStabilize = (recentUnrest.penalty - 1)
            recentUnrest.counter(toStabilize, "Colony stabilized") // yeaaaah bitch they have the stabilize button too!!! fuck you!!!!
        }
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        var oldTime = disarmTimeLeft
        disarmTimeLeft = max(0f, (disarmTimeLeft - Misc.getDays(amount)))
        var oldCommandTime = disruptedCommandDaysLeft
        disruptedCommandDaysLeft = max(0f, disruptedCommandDaysLeft - Misc.getDays(amount))
        checkCommandDisruption(oldCommandTime)
        checkIfStillDisarmed(oldTime)
        acceptingPeaceOneFrame = false

        if (sanitizeContributions) {
            sanitizeFactionContributions(factionContributions) // causes sanitization
            sanitizeContributions = false
        }
        val days = Misc.getDays(amount)
        daysLeftTilNextRetaliate = (daysLeftTilNextRetaliate - days).coerceAtLeast(0f)
        checkInterval.advance(days)

        /*if (true) {
            Global.getSector().removeScriptsOfClass(TransponderCheckBlockScript::class.java)
            niko_MPC_debugUtils.log.error("NIKO FORGOT TO REMOVE THE FUCKING DEBUG REMOVAL")

            getFractalColony()?.starSystem?.fleets?.filter { it.isPatrol() && it.faction.id == niko_MPC_ids.IAIIC_FAC_ID }?.forEach { it.memoryWithoutUpdate.setFlagWithReason(MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF, "player_system_owner", false, 0.1f) }
        }*/
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
        val FOB = getFOB()
        if (FOB == null || FOB.factionId != niko_MPC_ids.IAIIC_FAC_ID) {
            end(MPC_IAIICFobEndReason.FOB_LOST)
        }
    }

    private fun checkIfStillDisarmed(oldTime: Float) {
        if (oldTime <= 0f) return
        if (disarmTimeLeft <= 0f) {
            stopDisarm()
        }
    }

    private fun checkPlayerRep() {
        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return
        if (IAIIC.relToPlayer.rel > niko_MPC_settings.MAX_IAIIC_REP) {
            IAIIC.setRelationship(Factions.PLAYER, niko_MPC_settings.MAX_IAIIC_REP)
        }
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        if (faction != niko_MPC_ids.IAIIC_FAC_ID) return
        val IAIIC = getFaction()
        if (IAIIC.relToPlayer.rel > niko_MPC_settings.MAX_IAIIC_REP) {
            IAIIC.setRelationship(Factions.PLAYER, niko_MPC_settings.MAX_IAIIC_REP)
            return
        }
        if (acceptingPeaceOneFrame) return
        val oldRep = IAIIC.relToPlayer.rel - delta
        val oldRepLevel = RepLevel.getLevelFor(oldRep)
        if (oldRepLevel.ordinal <= RepLevel.HOSTILE.ordinal) {
            IAIIC.setRelationship(Factions.PLAYER, -0.5f)
        }
        if (IAIIC.relToPlayer.isHostile) {
            disarmTimeLeft = min(DISARMAMENT_PREMATURE_DAYS, disarmTimeLeft)
        }
    }

    private fun checkMarketDeficits() {
        for (market in affectedMarkets.toList()) {
            assignOrUnassignDeficit(market)
        }
        val fractalSystem = getFractalColony()?.starSystem ?: return
        for (market in fractalSystem.getMarketsInLocation()) { // in case markets change hands, we should check for all markets
            assignOrUnassignDeficit(market)
        }

        for (market in disarmedMarkets + Global.getSector().getFaction(Factions.PLAYER).getMarketsCopy()) {
            disarmOrRearm(market)
        }
    }

    private fun disarmOrRearm(market: MarketAPI) {
        if (market.shouldBeDisarmed()) {
            market.addConditionIfNotPresent(niko_MPC_ids.DISARMED_CONDITION_ID)
            disarmedMarkets += market
        } else {
            market.removeCondition(niko_MPC_ids.DISARMED_CONDITION_ID)
            disarmedMarkets -= market
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

    private fun MarketAPI.shouldBeDisarmed(): Boolean {
        if (disarmTimeLeft <= 0f) return false
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
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IAIIC_EVENT_CONCLUDED] = true

        val fob = MPC_fractalCoreFactor.getFOB()
        dismissCombatFleets()
        if (fob != null) {
            evacuateFob(fob)
        }
        if (reason.consideredVictory) {
            Global.getSector().memoryWithoutUpdate["\$MPC_IAIICDefeated"] = true
            beginCoreUpgrade()
            //beginHumanitarianAction()
            if (fob != null) {
                fob.removeCondition(niko_MPC_ids.MPC_BENEFACTOR_CONDID)
                fob.primaryEntity.makeUnimportant(MPC_FOB_ID)
                fob.primaryEntity.customDescriptionId = "MPC_IAIICFOBDecivved"
                DecivTracker.decivilize(fob, false,  false)
            }
        } else {
            val params = DebrisFieldParams(
                500f,
                3.2f,
                90f,
                0f
            )
            val containingLoc = fob?.containingLocation
            val fobEntity = fob?.primaryEntity
            DecivTracker.decivilize(fob, false,  false)
            //fob?.removeRadioChatter()

            //remove junk
            if (containingLoc != null && fobEntity != null) {
                for (entity in containingLoc.customEntities.toList()) {
                    if (entity.orbitFocus != fobEntity) continue
                    containingLoc.removeEntity(entity)
                }
                val field = containingLoc.addDebrisField(params, MathUtils.getRandom())
                val token = containingLoc.createToken(fobEntity.location)
                token.orbit = fobEntity.orbit.makeCopy()
                field.setCircularOrbit(token, 0f, 0f, 100f)
                fobEntity.fadeAndExpire(1f)
                if (niko_MPC_settings.indEvoEnabled) {
                    fob.removeCondition(Ids.COND_MINERING)
                    var minefield: MineBeltTerrainPlugin? = null
                    for (terrain in containingLoc.terrainCopy) {
                        if (terrain.plugin is MineBeltTerrainPlugin) {
                            val localField = terrain.plugin as MineBeltTerrainPlugin
                            if (!localField.primary.id.contains("MPC_arkFOB")) continue
                            minefield = localField
                            break
                        }
                    }
                    if (minefield != null) {
                        containingLoc.removeEntity(minefield.entity)
                        for (mine in containingLoc.customEntities.filter { it.customEntityType == "IndEvo_mine" }) {
                            if (mine.orbitFocus == minefield.entity) {
                                containingLoc.removeEntity(mine)
                            }
                        }
                    }
                }
            }
        }
        killIAIIC()
        support.forEach {
            it.recallFleets(MPC_fractalSupportFleetAssignmentAI.ReturnReason.EVENT_OVER)
            Global.getSector().intelManager.removeIntel(it)
        }
        support.clear()
        Global.getSector().intelManager.removeIntel(this)
        Global.getSector().memoryWithoutUpdate[KEY] = null
        MPC_IAIICInspectionPrepIntel.get()?.end()
        currentAction?.endAfterDelay()
        endAfterDelay()
        MPC_IAIICBlockadeFGI.get()?.finish(false)

        affectedMarkets.forEach { it.removeCondition(niko_MPC_ids.IAIIC_CONDITION_ID) }
        affectedMarkets.clear()

        disarmedMarkets.forEach { it.removeCondition(niko_MPC_ids.DISARMED_CONDITION_ID) }
        disarmedMarkets.clear()
        disarmTimeLeft = 0f

        MPC_fractalCoreFactor.unSetImportantMarkets()
    }

    private fun beginHumanitarianAction() {
        return
    }

    private fun beginCoreUpgrade() {
        class MPC_upgrade(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
            override fun executeImpl() {
                val colony = MPC_fractalCoreReactionScript.Companion.getFractalColony() ?: return
                Global.getSector().memoryWithoutUpdate["\$MPC_fractalColonyName"] = colony.name
                Global.getSector().memoryWithoutUpdate["\$MPC_fractalSystemName"] = colony.containingLocation.name

                Global.getSector().campaignUI.showInteractionDialog(
                    RuleBasedInteractionDialogPluginImpl(
                        "MPC_coreUpgradedInit"
                    ), Global.getSector().playerFleet
                )
            }
        }
        MPC_upgrade(IntervalUtil(30f, 30f)).start()
    }

    private fun dismissCombatFleets() {
        val targetColony = MPC_fractalCoreFactor.getFOB() ?: getFractalColony() ?: return
        for (fleet in targetColony.containingLocation.fleets.filter { it.faction.id == niko_MPC_ids.IAIIC_FAC_ID && !it.isTrader() }) {
            val evacLoc = getEvacLoc(targetColony) ?: return fleet.despawn()
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, evacLoc.primaryEntity, Float.MAX_VALUE, "returning to ${evacLoc.name}")
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
        fob.containingLocation.addEntity(fleet)
        fleet.containingLocation = fob.containingLocation
        fleet.setLocation(fob.primaryEntity.location.x, fob.primaryEntity.location.y)
        fleet.facing = MathUtils.getRandomNumberInRange(0f, 360f)

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.memoryWithoutUpdate["\$MPC_evacFleet"] = true

        return fleet
    }

    private fun killIAIIC() {
        for (market in Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).getMarketsCopy()) {
            market.factionId = Factions.INDEPENDENT
        }
        getFaction().isShowInIntelTab = false
    }

    // you cannot overflow in this intel
    override fun setProgress(progress: Int) {
        var lowestReachableProgress = Int.MAX_VALUE
        val diff = (progress - this.progress)
        if (diff > 0 && currentAction != null) return
        for (stage in stages) {
            if (stage.id == Stage.START) continue
            if (stage.wasEverReached) continue
            if (stage.progress < lowestReachableProgress) lowestReachableProgress = stage.progress
        }
        val newProgress = progress.coerceAtMost(lowestReachableProgress)
        if (newProgress >= (maxProgress / 0.4f)) {
            Global.getSector().memoryWithoutUpdate["\$MPC_voidsunCanSpawnNow"] = true
        }
        super.setProgress(newProgress)
    }

    private fun startBombardment() {
        val FOB = getFOB() ?: return
        val colony = getFractalColony() ?: return end(MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST)
        val validTargets = getMilitaryTargets()
        //if (validTargets.isEmpty()) validTargets = getGenericTargets()

        val params = GenericRaidParams(Random(random.nextLong()), true)
        params.makeFleetsHostile = false // will be made hostile when they arrive, not before
        params.factionId = FOB.factionId
        params.source = FOB

        params.prepDays = 20f + (MathUtils.getRandomNumberInRange(5f, 9f))
        params.payloadDays = 27f + 7f * random.nextFloat()

        params.raidParams.where = colony.starSystem
        params.raidParams.tryToCaptureObjectives = false
        params.raidParams.bombardment = MarketCMD.BombardType.TACTICAL
        params.raidParams.allowAnyHostileMarket = true
        params.raidParams.type = if (prob(50, random)) FGRaidType.SEQUENTIAL else FGRaidType.CONCURRENT // sequential or concurrent? not sure
        params.noun = "Bombardment"
        params.raidParams.raidsPerColony = 1
        //params.raidParams.allowNonHostileTargets = true
        params.raidParams.allowedTargets.addAll(validTargets)

        params.style = FleetStyle.STANDARD

        val fleetSizeMult: Float = FOB.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f)

        val f: Float = HostileActivityEventIntel.get()?.getMarketPresenceFactor(colony.starSystem)?.coerceAtLeast(1f) ?: 1f

        var totalDifficulty = (fleetSizeMult * 22f * (1f * f))

        while (totalDifficulty > 0) {
            val min = 13
            val max = 15

            //int diff = Math.round(StarSystemGenerator.getNormalRandom(random, min, max));
            val diff = min + random.nextInt(max - min + 1)
            params.fleetSizes.add(diff)
            totalDifficulty -= diff.toFloat()
        }

        val bombardFGI = MPC_IAIICBombardFGI(params)
        bombardFGI.listener = this
        Global.getSector().intelManager.addIntel(bombardFGI)
        currentAction = bombardFGI
    }

    fun startBlockade() {
        val FOB = getFOB() ?: return
        val colony = getFractalColony() ?: return end(MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST)

        val params = GenericRaidParams(Random(random.nextLong()), true)
        params.factionId = FOB.factionId
        params.source = FOB

        params.prepDays = 7f + random.nextFloat() * 14f
        params.payloadDays = 365f

        params.makeFleetsHostile = false

        val bParams = FGBlockadeParams()
        bParams.where = colony.starSystem
        bParams.targetFaction = Factions.PLAYER

        params.style = FleetStyle.STANDARD

        val fleetSizeMult: Float = FOB.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f)

        val f: Float = HostileActivityEventIntel.get().getMarketPresenceFactor(colony.starSystem)

        var totalDifficulty = ((fleetSizeMult * 80f * (1f)).coerceAtLeast(600f)).coerceAtMost(3800f)

        params.fleetSizes.add(200) // Command Fleet

        params.fleetSizes.add(20)
        params.fleetSizes.add(20)

        params.fleetSizes.add(5) // supply fleets #1
        params.fleetSizes.add(5) // supply fleets #2
        params.fleetSizes.add(5) // supply fleets #3


        val r: Random = MathUtils.getRandom()

        while (totalDifficulty > 0) {
            var max = 140f
            var min = 170f
            if (r.nextFloat() > 0.3f) {
                min = totalDifficulty.coerceAtMost(190f).toInt().toFloat()
                max = totalDifficulty.coerceAtMost(190f).toInt().toFloat()
            }
            val diff = StarSystemGenerator.getNormalRandom(r, min, max).roundToInt()
            params.fleetSizes.add(diff)
            totalDifficulty -= diff.toFloat()
        }

        val blockade = MPC_IAIICBlockadeFGI(params, bParams)
        blockade.listener = this
        Global.getSector().intelManager.addIntel(blockade)
        //currentAction = blockade
    }

    fun startAllOutAttack() {
        val FOB = getFOB() ?: return
        val colony = getFractalColony() ?: return end(MPC_IAIICFobEndReason.FRACTAL_COLONY_LOST)
        //if (validTargets.isEmpty()) validTargets = getGenericTargets()

        val spawnFP = 1100f // multiplied against the fob's fleetsize
        val raidIntel = MPC_IAIICAllOutAttack(FOB, colony, spawnFP)

        /*
        val params = GenericRaidParams(Random(random.nextLong()), true)
        params.makeFleetsHostile = false
        params.factionId = FOB.factionId
        params.source = FOB

        params.prepDays = if (Global.getSettings().isDevMode) 9f else 60f + (MathUtils.getRandomNumberInRange(5f, 9f)) // two whole months to prep
        params.payloadDays = 365f // this fight is to the death

        params.raidParams.where = colony.starSystem
        params.raidParams.tryToCaptureObjectives = false
        params.noun = "All-Out Attack"
        params.raidParams.raidsPerColony = 1
        params.raidParams.bombardment = MarketCMD.BombardType.TACTICAL
        //params.raidParams.allowNonHostileTargets = true
        params.raidParams.allowedTargets.add(colony)

        params.style = FleetStyle.STANDARD
        val fleetSizeMult: Float = FOB.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f)

        var totalDifficulty = (fleetSizeMult * 1200f) * 2f

        val initFleetsToSpawn = 30f
        var fleetsToSpawn = initFleetsToSpawn
        val difficultyPerFleet = (totalDifficulty / initFleetsToSpawn)
        while (fleetsToSpawn-- > 0f) {
            var diff = difficultyPerFleet * MathUtils.getRandomNumberInRange(0.9f, 1.1f)

            params.fleetSizes.add(diff.toInt())
            totalDifficulty -= diff
        }

        val attackFGI = MPC_IAIICAllOutAttackFGI(params)
        attackFGI.listener = this
        Global.getSector().intelManager.addIntel(attackFGI)
        currentAction = attackFGI*/
    }

    // LISTENER CRAP

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

        checkBattleForFactors(primaryWinner, battle)
        //checkBattleForRetaliation(primaryWinner, battle)
    }

    private fun checkBattleForFactors(primaryWinner: CampaignFleetAPI, battle: BattleAPI) {
        if (currentAction != null) return

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

        if (fleet.memoryWithoutUpdate.getBoolean("\$MPC_IAIICblockaderFleetFlag")) return

        fleet.addScript(NPCHassler(fleet, source.starSystem))
        fleet.memoryWithoutUpdate["\$nex_ignoreTransponderBlockCheck"] = true

        class LocalScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval, useDays = false) {
            override fun executeImpl() {
                if (!fleet.isExpired && !fleet.isEmpty) {
                    val travel = fleet.assignmentsCopy.firstOrNull { it.assignment == FleetAssignment.GO_TO_LOCATION }
                    if (travel != null) {
                        val castedAssignmentData = (travel as CampaignFleetAI.FleetAssignmentData)
                        castedAssignmentData.maxDurationInDays *= 2f
                    }
                }
            }
        }
        LocalScript(IntervalUtil(0f, 0f)).start()
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

    // FGI STUFF
    override fun reportFGIAborted(intel: FleetGroupIntel?) {
        return
    }

    // COLONY HOSTILE ACTION STUFF

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) {
        if (market == getFOB()) {
            retaliate(RetaliateReason.ATTACKED_FOB, dialog?.textPanel)
        }
    }

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) {
        if (market == getFOB()) {
            retaliate(RetaliateReason.ATTACKED_FOB, dialog?.textPanel)
        }
    }

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == getFOB()) {
            retaliate(RetaliateReason.ATTACKED_FOB, dialog?.textPanel)
        }
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == getFOB()) {
            retaliate(RetaliateReason.ATTACKED_FOB, dialog?.textPanel)
        }
    }

    enum class RetaliateReason {
        ATTACKED_FOB,
        PISSED_OFF_JILL,
        KEPT_SYNCROTRON,
        MILITARY_HOUSE_PROGRESS,
        BETRAYED_LINDUNBERG,
        TURNING_HOUSES_AGAINST_HEGEMONY;
    }
    /** Triggers sabotage after a delay, and with a notification. */
    fun retaliate(reason: RetaliateReason, dialog: TextPanelAPI? = null) {
        if (daysLeftTilNextRetaliate > 0f) return

        if (dialog != null) {
            sendUpdateIfPlayerHasIntel(reason, dialog)
        } else {
            sendUpdateIfPlayerHasIntel(reason, false)
        }
        class MPC_sabotageScript(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
            override fun executeImpl() {
                if (!isEnded) sabotage()
            }
        }
        MPC_sabotageScript(IntervalUtil(0.3f, 0.3f)).start()
        daysLeftTilNextRetaliate = RETALIATE_COOLDOWN_DAYS
    }

    fun tryPeace(dialog: InteractionDialogAPI? = null) {
        if (getPeacePossibility() != PeacePossibility.YES) return
        acceptingPeaceOneFrame = true

        if (dialog != null) {
            sendUpdateIfPlayerHasIntel("Ceasefire established", dialog.textPanel)
        } else {
            sendUpdateIfPlayerHasIntel("Ceasefire established", false, false)
        }

        val delta = RepLevel.SUSPICIOUS.min - Global.getSector().playerFaction.getRelationship(niko_MPC_ids.IAIIC_FAC_ID)
        val impact = CoreReputationPlugin.CustomRepImpact()
        impact.delta = delta
        Global.getSector().adjustPlayerReputation(RepActionEnvelope(CoreReputationPlugin.RepActions.CUSTOM, impact, null, dialog?.textPanel, false, true, "Ceasefire Established"),  niko_MPC_ids.IAIIC_FAC_ID)
        Global.getSector().playerFaction.setRelationship(niko_MPC_ids.IAIIC_FAC_ID, RepLevel.SUSPICIOUS)
    }

    /** Applies a crippling malus to all of the player's worlds' fleetsize. Part of peace concessions. */
    fun disarm(time: Float = DEFAULT_DISARM_TIME) {
        disarmTimeLeft += time
        checkMarketDeficits()
    }

    fun stopDisarm() {
        disarmTimeLeft = 0f
        checkMarketDeficits()
    }

    fun disruptCommand(time: Float = DEFAULT_COMMAND_DISRUPTION_DAYS) {
        var old = disruptedCommandDaysLeft
        disruptedCommandDaysLeft += time
        checkCommandDisruption(disruptedCommandDaysLeft)
    }

    private fun checkCommandDisruption(previousTimeLeft: Float = disruptedCommandDaysLeft) {
        if (disruptedCommandDaysLeft <= 0f) {
            unapplyCommandDisruption()
        } else {
            applyCommandDisruption()
        }
    }

    private fun applyCommandDisruption() {
        getFOB()?.addConditionIfNotPresent(niko_MPC_ids.IAIIC_COMMAND_DISRUPTED_CONDITION)
    }

    private fun unapplyCommandDisruption() {
        getFOB()?.removeCondition(niko_MPC_ids.IAIIC_COMMAND_DISRUPTED_CONDITION)
    }

    abstract class RemoveContributionScript: Script {
        fun getIAIIC(): FactionAPI = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
    }
}

