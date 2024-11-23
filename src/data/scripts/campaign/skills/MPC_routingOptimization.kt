package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import data.scripts.campaign.skills.MPC_routingOptimization.AICoreEffect.Companion.coreEffectMap
import data.utilities.niko_MPC_marketUtils.getLargestMarketSize
import lunalib.lunaExtensions.getMarketsCopy

class MPC_routingOptimization {

    companion object {
        const val GAMMA_INDUSTRY_DEMAND = -1f
        const val BETA_INDUSTRY_UPKEEP_PERCENT = -0.25f
        const val ALPHA_INDUSTRY_OUTPUT_INCREMENT = 1f

        const val AI_CORE_ADMIN_ACCESSIBILITY_BONUS = 0.1f
    }

    enum class AICoreEffect(val coreId: String) {

        GAMMA(Commodities.GAMMA_CORE) {
            override fun apply(industry: Industry, id: String) {
                industry.demandReductionFromOther.modifyFlat(id, GAMMA_INDUSTRY_DEMAND, "Routing Optimization")
            }

            override fun unapply(industry: Industry, id: String) {
                industry.demandReductionFromOther.unmodify(id)
            }
        },
        BETA(Commodities.BETA_CORE) {
            override fun apply(industry: Industry, id: String) {
                GAMMA.apply(industry, id)
                industry.upkeep.modifyPercent(id, BETA_INDUSTRY_UPKEEP_PERCENT, "Routing Optimization")
            }

            override fun unapply(industry: Industry, id: String) {
                GAMMA.unapply(industry, id)
                industry.upkeep.unmodify(id)
            }
        },
        ALPHA(Commodities.ALPHA_CORE) {
            override fun apply(industry: Industry, id: String) {
                BETA.apply(industry, id)
                industry.supplyBonusFromOther.modifyFlat(id, ALPHA_INDUSTRY_OUTPUT_INCREMENT, "Routing Optimization")
            }

            override fun unapply(industry: Industry, id: String) {
                BETA.unapply(industry, id)
                industry.supplyBonusFromOther.unmodify(id)
            }
        };

        abstract fun apply(industry: Industry, id: String)
        abstract fun unapply(industry: Industry, id: String)

        companion object {
            val coreEffectMap = generateAIEffectMap()
            fun generateAIEffectMap(): MutableMap<String, AICoreEffect> {
                val map = HashMap<String, AICoreEffect>()

                for (entry in AICoreEffect.values()) {
                    map[entry.coreId] = entry
                }

                return map
            }
        }
    }

    class Level1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            for (industry in market.industries) {
                val effect = coreEffectMap[industry.aiCoreId] ?: continue
                effect.apply(industry, id)
            }
        }

        override fun unapply(market: MarketAPI, id: String) {
            for (industry in market.industries) {
                for (effect in coreEffectMap.values) {
                    effect.unapply(industry, id) // no real harm in this
                }
            }
        }

        override fun getEffectDescription(level: Float): String {
            return "Enhances the effect of AI cores installed in the market"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level2 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            for (otherMarket in market.faction.getMarketsCopy().filter { it != market && it.admin?.isAICore == true }) {
                if (otherMarket.size > market.size) continue
                otherMarket.accessibilityMod.modifyFlat(id, AI_CORE_ADMIN_ACCESSIBILITY_BONUS, "${market.name} administrator")
            }

        }

        override fun unapply(market: MarketAPI, id: String) {
            for (otherMarket in Global.getSector().economy.marketsCopy) {
                otherMarket.accessibilityMod.unmodify(id) // this is a nuclear option, a bit slow but really it shouldnt be that bad
            }
        }

        override fun getEffectDescription(level: Float): String {
            return "Increases accessibility of all same-faction markets with AI administrators of equal or lower size to the governed market by %${(AI_CORE_ADMIN_ACCESSIBILITY_BONUS * 100f).toInt()}"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.ALL_OUTPOSTS
        }
    }

}