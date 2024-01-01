package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.COMMAND_TAG_SCORE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.GROUND_DEFENSES_TAG_SCORE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.MILITARY_TAG_SCORE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.PATROL_TAG_SCORE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.POPULATION_ANCHOR
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.POPULATION_INCREMENT
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.STABILITY_ANCHOR
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasonsVariables.STABILITY_DIFFERENCE_INCREMENT
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isInhabited
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap

object cullingStrengthReasonsVariables{

    const val POPULATION_ANCHOR = 2
    const val POPULATION_INCREMENT = 35

    const val STABILITY_ANCHOR = 8

    const val STABILITY_DIFFERENCE_INCREMENT = 15f

    const val PATROL_TAG_SCORE = 50f
    const val MILITARY_TAG_SCORE = 80f
    const val COMMAND_TAG_SCORE = 120f

    const val GROUND_DEFENSES_TAG_SCORE = 50f
}

enum class cullingStrengthReasons {

    //TODO: return to this, maybe have a crew/marine/heavy machinery req idfk
    /*
    SHORTAGES {
        override fun getName(): String = "Shortages"

        override fun getDesc(): String = "Insufficient supply, such as crew or heavy machinery, can thwart culling efforts."

        override fun shouldShow(market: MarketAPI): Boolean {
            return false
        }
        override fun getScoreForMarket(market: MarketAPI): Float {
            return 0f
        }
    }, */
    POPULATION {
        override fun getName(): String = "Population"

        override fun getDesc(): String {
            return "For each level of population above $POPULATION_ANCHOR, culling strength will increase by $POPULATION_INCREMENT."
        }

        override fun getScoreForMarket(market: MarketAPI): Float {
            val anchor = POPULATION_ANCHOR
            val stepsAbove = market.size - anchor
            if (stepsAbove <= 0) return 0f

            return (stepsAbove * POPULATION_INCREMENT).toFloat()
        }

        override fun getBaseColor(): Color {
            return Misc.getPositiveHighlightColor()
        }
    },
    STABILITY {
        override fun getDesc(): String {
            return "For each point of stability above or below ${STABILITY_ANCHOR}, culling strength will increase " +
                    "or decrease by ${STABILITY_DIFFERENCE_INCREMENT} respectively."
        }
        override fun getName(): String = "Stability"
        override fun shouldShow(market: MarketAPI): Boolean = true
        override fun getScoreForMarket(market: MarketAPI): Float {
            if (!market.isInhabited()) return 0f
            return ((market.stability.modifiedValue - STABILITY_ANCHOR) * STABILITY_DIFFERENCE_INCREMENT)
        }

        override fun getBaseColor(): Color = Misc.getHighlightColor()
    },
    GROUND_FORCE_PRESENCE {
        val itemInstalledMult: Float = 1.3f
        val tagMap = HashMap<String, Float>()
        val coreMap = HashMap<String, Float>()
        val improvementMult = 1.2f
        init {
            tagMap[Industries.TAG_PATROL] = PATROL_TAG_SCORE
            tagMap[Industries.TAG_MILITARY] = MILITARY_TAG_SCORE
            tagMap[Industries.TAG_COMMAND] = COMMAND_TAG_SCORE

            tagMap[Industries.TAG_GROUNDDEFENSES] = GROUND_DEFENSES_TAG_SCORE

            coreMap[Commodities.ALPHA_CORE] = 1.5f
            coreMap[Commodities.BETA_CORE] = 1.3f
            coreMap[Commodities.GAMMA_CORE] = 1.1f
        }
        override fun getDesc(): String {
            return "For each piece of military infrastructure on the planet, culling strength will increase in " +
                    "relation to it's capability. This can be affected by installed items, shortages, improvements, " +
                    "and more."
        }
        override fun getName(): String = "Ground force presence"
        override fun shouldShow(market: MarketAPI): Boolean = true
        override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()

        override fun getScoreForMarket(market: MarketAPI): Float {
            var totalScore = 0f
            for (industry in market.industries) {

                var industryScore = 0f
                industry.spec.tags.forEach { it -> tagMap[it]?.let { industryScore += it } }

                if (industryScore == 0f) continue

                var totalMult = 1f
                if (industry.isImproved) totalMult += improvementMult - 1
                coreMap[industry.aiCoreId]?.let { totalMult += it - 1 }

                if (industry.specialItem != null) totalMult += itemInstalledMult

                val finalScore = industryScore * totalMult

                totalScore += finalScore
            }
            return totalScore
        }

        override fun getSpecificInfo(): String {
            return "Patrol HQs, Military Bases, and High Commands increase score by " +
                    "${tagMap[Industries.TAG_PATROL]}, ${tagMap[Industries.TAG_MILITARY]}, and ${tagMap[Industries.TAG_COMMAND]} respectively." +
                    "\nGround defenses and heavy batteries also increase score by ${tagMap[Industries.TAG_GROUNDDEFENSES]}." +
                    "\n     When improved: Contribution multiplied by $improvementMult." +
                    "\n     When a AI core is installed, contribution is multiplied by ${coreMap[Commodities.GAMMA_CORE]}, ${coreMap[Commodities.BETA_CORE]}, and ${coreMap[Commodities.ALPHA_CORE]} respectively." +
                    "\n     If a special item is installed, contribution is multiplied by $itemInstalledMult." +
                    "\nModded industries, provided they are tagged correctly, will also contribute to this score."
        }
    },
    ADMINISTRATOR_ABILITY {
        val skillMap: MutableMap<String, Float> = HashMap()
        val scorePerLevel = 5f
        init {
            skillMap[Skills.PLANETARY_OPERATIONS] = 50f
            skillMap[Skills.TACTICAL_DRILLS] = 10f
            skillMap[Skills.HYPERCOGNITION] = 80f
        }

        override fun getDesc(): String {
            return "Various administrative skills may increase the government's ability to manipulate growth."
        }

        override fun getSpecificInfo(): String {
            return "The following skills affect culling strength:" +
                    "\n     Tactical drills - ${skillMap[Skills.TACTICAL_DRILLS]}" +
                    "\n     Hypercognition - ${skillMap[Skills.HYPERCOGNITION]}" +
                    "\nAdministrator level increases strength by $scorePerLevel per level."
        }

        override fun getName(): String {
            return "Administrator Ability"
        }

        override fun shouldShow(market: MarketAPI): Boolean {
            return true
        }

        override fun getScoreForMarket(market: MarketAPI): Float {
            if (!market.isInhabited()) return 0f
            var amount = 0f

            val admin = market.admin ?: return amount
            val stats = admin.stats

            for (entry in skillMap.keys) {
                if (stats.hasSkill(entry)) amount += skillMap[entry]!!
            }

            amount += getScoreForLevel(admin)

            return amount
        }
        fun getScoreForLevel(admin: PersonAPI): Float {
            val level = admin.stats?.level ?: return 0f

            return scorePerLevel * level
        }
        override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()
    };
    /*EXTERNAL_SUPPORT {
        override fun getName(): String = "External Support"

        override fun getDesc(): String = "By injecting credits, resources, and power into the colony, strength may be artificially raised, albiet inefficiently."

        override fun getScoreForMarket(market: MarketAPI): Float {
            var rating = market.getOvergrownNanoforgeIndustryHandler()?.intel?.externalSupportRating ?: return 0f
            rating *= 5f

            return rating
        }
        override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()
    }*/

    abstract fun getName(): String
    abstract fun getDesc(): String
    open fun getSpecificInfo(): String? = null
    open fun shouldShow(market: MarketAPI): Boolean = true
    abstract fun getScoreForMarket(market: MarketAPI): Float
    fun getHighlightColor(score: Float): Color {
        if (score > 0f) return Misc.getPositiveHighlightColor()
        if (score < 0f) return Misc.getNegativeHighlightColor()
        return Misc.getDarkHighlightColor()
    }

    open fun getBaseColor(): Color = Misc.getGrayColor()

    companion object {
        fun getReasons(market: MarketAPI): MutableMap<cullingStrengthReasons, Float> {
            val map: MutableMap<cullingStrengthReasons, Float> = EnumMap(cullingStrengthReasons::class.java)
            for (entry in cullingStrengthReasons.values()) {
                if (!entry.shouldShow(market)) continue

                val score = entry.getScoreForMarket(market)
                map[entry] = score
            }
            return map
        }

        fun getScoreFromReasons(reasons: MutableMap<cullingStrengthReasons, Float>): Float {
            var score = 0f
            reasons.values.forEach { score += it }
            return score.coerceAtLeast(0f)
        }
    }
}