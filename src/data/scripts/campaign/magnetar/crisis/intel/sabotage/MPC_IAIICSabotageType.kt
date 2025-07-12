package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel.Companion.MAX_OPERATIVE_RESISTANCE
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICAccessibilitySabotage.Companion.BASE_ACCESSIBILITY_MALUS
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICAccessibilitySabotage.Companion.BASE_TIME_DAYS
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICAccessibilitySabotage.Companion.TIME_VARIATION
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICFleetSizeSabotage.Companion.BASE_SIZE_MALUS
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICFleetSizeSabotage.Companion.MAX_COLONIES
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICFleetSizeSabotage.Companion.MIN_PERCENT_OF_COLONIES
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICSabotageCondition.Companion.addSabotage
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_mathUtils.prob
import data.utilities.niko_MPC_mathUtils.randomlyDistributeNumberAcrossEntries
import data.utilities.niko_MPC_settings
import exerelin.campaign.CovertOpsManager
import exerelin.campaign.intel.agents.AgentIntel
import exerelin.utilities.NexUtils
import org.lazywizard.lazylib.MathUtils
import java.lang.Math.ceil
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.ceil

enum class MPC_IAIICSabotageType() {
    REDUCE_ACCESSIBILITY {
        override fun getInstance(market: MarketAPI, params: MPC_IAIICSabotage.MPC_IAIICSabotageParams): MPC_IAIICAccessibilitySabotage {
            return MPC_IAIICAccessibilitySabotage(market, params)
        }

        override fun getTargetParams(): MutableMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams> {
            val targets = HashMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams>()
            val intel = MPC_IAIICFobIntel.get() ?: return targets

            var validTargets: Collection<MarketAPI> = MPC_IAIICFobIntel.getMilitaryTargets(false).shuffled()
            if (validTargets.size < 3) validTargets = MPC_IAIICFobIntel.getGenericTargets().shuffled()
            if (validTargets.isEmpty()) return targets

            var numTargetsLeft = (ceil(validTargets.size / MPC_IAIICAccessibilitySabotage.MIN_PERCENT_OF_COLONIES)).coerceAtMost(MPC_IAIICAccessibilitySabotage.MAX_COLONIES)
            val pickedTargets = HashSet<MarketAPI>()
            while (numTargetsLeft-- > 0 && validTargets.isNotEmpty()) {
                val picked = validTargets.random()
                validTargets -= picked
                pickedTargets += picked
            }

            val contribution = MPC_IAIICFobIntel.getSabotageMultFromContributingFactions(intel.factionContributions)
            val mult = contribution
            for (entry in pickedTargets) {
                targets[entry] = MPC_IAIICSabotage.MPC_IAIICSabotageParams(
                    BASE_TIME_DAYS + (MathUtils.getRandomNumberInRange(-TIME_VARIATION, TIME_VARIATION)),
                    mult
                )
            }
            return targets
        }

    },
    REDUCE_FLEET_SIZE {
        override fun getInstance(market: MarketAPI, params: MPC_IAIICSabotage.MPC_IAIICSabotageParams): MPC_IAIICFleetSizeSabotage {
            return MPC_IAIICFleetSizeSabotage(market, params)
        }

        override fun getTargetParams(): MutableMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams> {
            val targets = HashMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams>()
            val intel = MPC_IAIICFobIntel.get() ?: return targets

            var validTargets: Collection<MarketAPI> = MPC_IAIICFobIntel.getMilitaryTargets(
                addFractal = false,
                includeHeavyIndustry = false,
                includePatrolHQ = true
            ).shuffled()
            if (validTargets.size < 3) validTargets = MPC_IAIICFobIntel.getGenericTargets().shuffled()
            if (validTargets.isEmpty()) return targets

            var numTargetsLeft = (ceil(validTargets.size / MPC_IAIICFleetSizeSabotage.MIN_PERCENT_OF_COLONIES)).coerceAtMost(MPC_IAIICFleetSizeSabotage.MAX_COLONIES)
            val pickedTargets = HashSet<MarketAPI>()
            while (numTargetsLeft-- > 0 && validTargets.isNotEmpty()) {
                val picked = validTargets.random()
                validTargets -= picked
                pickedTargets += picked
            }

            val contribution = MPC_IAIICFobIntel.getSabotageMultFromContributingFactions(intel.factionContributions)
            val mult = contribution
            for (entry in pickedTargets) {
                targets[entry] = MPC_IAIICSabotage.MPC_IAIICSabotageParams(
                    MPC_IAIICFleetSizeSabotage.BASE_TIME_DAYS + (MathUtils.getRandomNumberInRange(-MPC_IAIICFleetSizeSabotage.TIME_VARIATION, MPC_IAIICFleetSizeSabotage.TIME_VARIATION)),
                    mult
                )
            }
            return targets
        }
    },
    REDUCE_STABILITY{
        override fun getInstance(market: MarketAPI, params: MPC_IAIICSabotage.MPC_IAIICSabotageParams): MPC_IAIICUnrestSabotage {
            return MPC_IAIICUnrestSabotage(market, params)
        }

        override fun getTargetParams(): MutableMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams> {
            val targets = HashMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams>()
            val intel = MPC_IAIICFobIntel.get() ?: return targets

            var validTargets: Collection<MarketAPI> = MPC_IAIICFobIntel.getMilitaryTargets(
                addFractal = false,
                includeHeavyIndustry = true,
                includePatrolHQ = true
            ).shuffled()
            if (validTargets.size < 3) validTargets = MPC_IAIICFobIntel.getGenericTargets().shuffled()
            if (validTargets.isEmpty()) return targets

            var numTargetsLeft = (ceil(validTargets.size / MPC_IAIICUnrestSabotage.MIN_PERCENT_OF_COLONIES)).coerceAtMost(MPC_IAIICUnrestSabotage.MAX_COLONIES)
            val pickedTargets = HashSet<MarketAPI>()
            while (numTargetsLeft-- > 0 && validTargets.isNotEmpty()) {
                val picked = validTargets.random()
                validTargets -= picked
                pickedTargets += picked
            }

            val contribution = MPC_IAIICFobIntel.getSabotageMultFromContributingFactions(intel.factionContributions)
            val mult = contribution
            for (entry in pickedTargets) {
                targets[entry] = MPC_IAIICSabotage.MPC_IAIICSabotageParams(
                    0f,
                    mult
                )
            }
            return targets
        }
    };

    abstract fun getInstance(market: MarketAPI, params: MPC_IAIICSabotage.MPC_IAIICSabotageParams): MPC_IAIICSabotage
    abstract fun getTargetParams(): MutableMap<MarketAPI, MPC_IAIICSabotage.MPC_IAIICSabotageParams>

    enum class MPC_IAIICSabotageResult(val message: String?, val isSuccess: Boolean = false) {
        SUCCESS(null, true),
        THWARTED_BY_MARKET("Thwarted by local security forces"),
        THWARTED_BY_OPERATIVE("Thwarted by operative");
    }
    data class MPC_IAIICSabotageResultData(
        val target: MarketAPI,
        val sabotage: MPC_IAIICSabotage,
        val result: MPC_IAIICSabotageResult
    )

    companion object {
        fun addApplicableSabotage(random: Random = Random()): MutableSet<MPC_IAIICSabotageResultData> {
            val sabotageList = HashSet<MPC_IAIICSabotageResultData>()
            val values = entries
            for (sabotage in values.shuffled(random)) {
                val targets = sabotage.getTargetParams()
                if (targets.isEmpty()) continue
                targets.forEach {
                    val market = it.key
                    val instance = sabotage.getInstance(market, it.value)
                    val result = checkSabotageResistance(market, sabotage)
                    val data = MPC_IAIICSabotageResultData(
                        market,
                        instance,
                        result
                    )
                    if (result.isSuccess) {
                        market.addSabotage(instance)
                    }
                    sabotageList += data
                }
                break
            }
            return sabotageList
        }

        private fun checkSabotageResistance(market: MarketAPI, sabotage: MPC_IAIICSabotageType): MPC_IAIICSabotageResult {
            val marketResistance = getMarketResistance(market)
            if (prob(marketResistance)) {
                return MPC_IAIICSabotageResult.THWARTED_BY_MARKET
            }
            if (niko_MPC_settings.nexLoaded) {
                var operativeResistance = 0f
                for (operative in CovertOpsManager.getAgentsStatic().filter { it.market == market }) {
                    operativeResistance += MPC_IAIICFobIntel.SABOTAGE_SUBVERSION_CHANCE_PER_OPERATIVE_LEVEL * operative.level
                }
                operativeResistance = operativeResistance.coerceAtMost(MAX_OPERATIVE_RESISTANCE)
                if (prob(operativeResistance)) {
                    return MPC_IAIICSabotageResult.THWARTED_BY_OPERATIVE
                }
            }
            return MPC_IAIICSabotageResult.SUCCESS
        }
        fun getMarketResistance(market: MarketAPI) = MPC_IAIICFobIntel.BASE_SABOTAGE_RESISTANCE
    }

    class MPC_IAIICSabotageScript(
        val market: MarketAPI,

    ): niko_MPC_baseNikoScript() {
        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            TODO("Not yet implemented")
        }
    }
}