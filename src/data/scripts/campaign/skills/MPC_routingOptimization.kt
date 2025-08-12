package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.econ.impl.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.LocalResourcesSubmarketPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecution
import data.scripts.campaign.magnetar.AIPlugins.MPC_slavedOmegaCoreOfficerPlugin
import data.scripts.campaign.skills.MPC_routingOptimization.AICoreEffect.Companion.coreEffectMap
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_reflectionUtils
import data.utilities.niko_MPC_settings
import indevo.industries.artillery.industry.ArtilleryStation
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getStationFleet

class MPC_routingOptimization {

    companion object {
        const val GAMMA_INDUSTRY_DEMAND = 1f
        const val BETA_INDUSTRY_UPKEEP_MULT = 0.75f
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
                industry.upkeep.modifyMult(id, BETA_INDUSTRY_UPKEEP_MULT, "Routing Optimization")
            }

            override fun unapply(industry: Industry, id: String) {
                GAMMA.unapply(industry, id)
                industry.upkeep.unmodify(id)
            }
        },
        ALPHA(Commodities.ALPHA_CORE) {
            override fun apply(industry: Industry, id: String) {
                BETA.apply(industry, id)

                val market = industry.market

                if (industry is MilitaryBase) {
                    market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(
                        id, 1f + MilitaryBase.ALPHA_CORE_BONUS, "Routing Optimization (${industry.nameForModifier})"
                    )

                } else if (industry is GroundDefenses) {
                    market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                        id, 1f + GroundDefenses.ALPHA_CORE_BONUS, "Routing Optimization (${industry.nameForModifier})"
                    )
                } else if (industry is PlanetaryShield) {
                    market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(
                        "${id}_PS", 1f + PlanetaryShield.ALPHA_CORE_BONUS, "Routing Optimization (${industry.nameForModifier})"
                    )
                } else if (industry is Spaceport) {
                    market.accessibilityMod.modifyFlat(
                        id,
                        Spaceport.ALPHA_CORE_ACCESSIBILITY,
                        "Routing Optimization (${industry.nameForModifier})"
                    )
                } else if (industry is Waystation) {
                    if (market.isPlayerOwned) {
                        val sub = Misc.getLocalResources(market)
                        if (sub is LocalResourcesSubmarketPlugin) {
                            val bonus = market.size * Global.getSettings().getFloat("stockpileMultExcess")
                            val lr = sub
                            lr.getStockpilingBonus(Commodities.FUEL).modifyFlat(id, bonus)
                            lr.getStockpilingBonus(Commodities.SUPPLIES).modifyFlat(id, bonus)
                            lr.getStockpilingBonus(Commodities.CREW).modifyFlat(id, bonus)
                        }
                    }
                } else if (industry is TradeCenter) {
                    market.incomeMult.modifyPercent(
                        id,
                        TradeCenter.ALPHA_CORE_BONUS,
                        "Routing Optimization (${industry.nameForModifier})"
                    )

                } else if (industry is TechMining) {
                    market.stats.dynamic.getStat(Stats.TECH_MINING_MULT).modifyMult(id, 1f + TechMining.ALPHA_CORE_FINDS_BONUS)
                } else if (industry is OrbitalStation) {
                    MPC_fractalStationOfficerAdder.addIfDoesntExist(market)
                } else if (industry is Cryorevival) {
                    val incoming = market.incoming
                    //val bonus = niko_MPC_reflectionUtils.invoke("getImmigrationBonus", industry, declared = true, classToGetFrom = Cryorevival::class.java) as? Float ?: return
                    incoming.weight.modifyFlat(
                        id, (Cryorevival.ALPHA_CORE_BONUS),
                        "Routing Optimization (${industry.nameForModifier})"
                    )

                }
                else if (niko_MPC_settings.indEvoEnabled && industry is ArtilleryStation) {
                    MPC_artyStationOfficerAdder.addIfDoesntExist(market)
                }

                else {
                    industry.supplyBonusFromOther.modifyFlat(
                        id,
                        ALPHA_INDUSTRY_OUTPUT_INCREMENT,
                        "Routing Optimization (${industry.nameForModifier})"
                    )
                }
            }

            override fun unapply(industry: Industry, id: String) {
                BETA.unapply(industry, id)
                val market = industry.market
                market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
                market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify("${id}_PS")
                market.accessibilityMod.unmodify(id)
                market.incomeMult.unmodify(id)
                market.stats.dynamic.getStat(Stats.TECH_MINING_MULT).unmodify(id)
                market.incoming.weight.unmodify(id)

                MPC_fractalStationOfficerAdder.removeScript(market)
                MPC_artyStationOfficerAdder.removeScript(market)

                industry.supplyBonusFromOther.unmodify(id)

                val sub = Misc.getLocalResources(market)
                if (sub is LocalResourcesSubmarketPlugin) {
                    val lr = sub
                    lr.getStockpilingBonus(Commodities.FUEL).unmodify(id)
                    lr.getStockpilingBonus(Commodities.SUPPLIES).unmodify(id)
                    lr.getStockpilingBonus(Commodities.CREW).unmodify(id)
                }
            }
        };

        abstract fun apply(industry: Industry, id: String)
        abstract fun unapply(industry: Industry, id: String)

        companion object {
            val coreEffectMap = generateAIEffectMap()
            fun generateAIEffectMap(): MutableMap<String, AICoreEffect> {
                val map = HashMap<String, AICoreEffect>()

                for (entry in entries) {
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
            // MPC_fractalCoreReactionScript
        }

        override fun unapply(market: MarketAPI, id: String) {

        }

        override fun getEffectDescription(level: Float): String {
            return "Increases accessibility of all same-faction markets with AI administrators of equal or lower size to the governed market by ${(AI_CORE_ADMIN_ACCESSIBILITY_BONUS * 100f).trimHangingZero()}%"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.GOVERNED_OUTPOST
        }
    }
}