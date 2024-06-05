package data.nexerelin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.SATELLITE_NEX_ABILITY_BASE_TOTAL_DAMAGE
import exerelin.campaign.intel.groundbattle.*
import exerelin.campaign.intel.groundbattle.GroundBattleAI.IFBStrengthRecord
import exerelin.campaign.intel.groundbattle.GroundBattleLog.LOG_PADDING
import exerelin.campaign.intel.groundbattle.plugins.AbilityPlugin
import exerelin.campaign.intel.groundbattle.plugins.FireSupportAbilityPlugin
import org.lazywizard.lazylib.MathUtils

class satelliteBarrageNexAbilityPlugin: AbilityPlugin() {

    companion object {
        fun getHandlersForSide(side: GroundBattleSide): MutableSet<niko_MPC_satelliteHandlerCore> {
            val handlers: MutableSet<niko_MPC_satelliteHandlerCore> = HashSet()

            val market = side.intel.market
            val foundHandlers = market.getSatelliteHandlers()
            handlers.addAll(foundHandlers.filter { !it.getFaction().isHostileTo(side.faction) })

            return handlers
        }
    }

    override fun dialogAddIntro(dialog: InteractionDialogAPI?) {
        if (dialog == null) return

        dialog.textPanel.addPara("Prolonged suppressive fire delivered by satellites in orbit, egged on by strategically placed phase flares.")
        val tooltip = dialog.textPanel.beginTooltip()
        generateTooltip(tooltip)
        dialog.textPanel.addTooltip()

        addCooldownDialogText(dialog)
    }

    override fun generateTooltip(tooltip: TooltipMakerAPI?) {
        if (tooltip == null) return

        val opad = 10f

        val duration = getDuration()
        val baseDamage = getTroopDamage(null).toInt()
        val desc1 = "For %s turns, enemy units on the target industry receive %s damage (at current market size) distributed evenly, " +
                "and are forced to reorganize (reducing outgoing damage by %s and preventing movement). \n" +
                "Damage increased by %sx if combat is ongoing on the target, and reduced by target units' defensive modifiers (if any)."

        val desc2 = "Each turn, disrupts the target industry for %s days (unless it is a hardened military structure)."

        tooltip.addPara(desc1, 0f, Misc.getHighlightColor(), "$duration", "$baseDamage", "30%", "${FireSupportAbilityPlugin.CLOSE_SUPPORT_DAMAGE_MULT}")
        tooltip.addPara(desc2, opad, Misc.getHighlightColor(), "${niko_MPC_settings.SATELLITE_NEX_ABILITY_BASE_DISRUPTION_TIME}")
    }

    override fun activate(dialog: InteractionDialogAPI?, user: PersonAPI?) {
        val handlers = getHandlersForSide(side)
        if (handlers.isEmpty()) return

        val mult = handlers.size.toFloat()

        val duration = getDuration()
        val listener = SatelliteBarrageNexAbilityListener(this, target, intel, mult, duration).start()
        listener.applyEffect(true) // immediately apply it just once

        logActivation(user)

        super.activate(dialog, user)
    }

    private fun getDuration(): Int {
        return niko_MPC_settings.SATELLITE_NEX_ABILITY_DURATION
    }

    override fun getDisabledReason(user: PersonAPI?): Pair<String, MutableMap<String, Any>>? {
        val params: MutableMap<String, Any> = HashMap()

        val satelliteHandlers = getHandlersForSide(side)
        if (satelliteHandlers.isEmpty()) {
            val id = "noSatellites"
            val desc = "you shouldnt see this"
            params["desc"] = desc

            return Pair(id, params)
        }
        if (side.data.containsKey(GBConstants.TAG_PREVENT_BOMBARDMENT)) {
            val id = "bombardmentPrevented"
            val desc = "Attack prevented"
            params["desc"] = desc
            return Pair(id, params)
        }

        if (targetIndustries.isEmpty()) {
            val id = "noTargets"
            val desc = "No targets"
            params["desc"] = desc
            return Pair(id, params)
        }

        return super.getDisabledReason(user)
    }

    override fun showIfDisabled(disableReason: Pair<String, MutableMap<String, Any>>?): Boolean {
        return getHandlersForSide(side).isNotEmpty()
    }

    override fun targetsIndustry(): Boolean = true

    override fun getTargetIndustries(): List<IndustryForBattle> { // copied from FireSupportAbilityPlugin
        val targets: MutableList<IndustryForBattle> = ArrayList()
        for (ifb in intel.industries) {
            if (ifb.heldByAttacker != getSide().isAttacker && !ifb.isIndustryTrueDisrupted
                && ifb.plugin.def.hasTag("noBombard")
            ) continue
            if (!ifb.containsEnemyOf(side.isAttacker)) continue
            targets.add(ifb)
        }
        return targets
    }

    override fun getAIUsePriority(ai: GroundBattleAI): Float {
        val industries = ai.industriesWithEnemySorted

        val globalCd = def.cooldownGlobal
        return if (industries.isEmpty()) 0f else if (globalCd == 0) 500f else industries[0].reinforcePriorityCache * 5 // ai will always try to use it cause its free
    }

    // designed to identify the highest ratio of strength and then prevent enemies from escaping
    override fun aiExecute(ai: GroundBattleAI?, user: PersonAPI?): Boolean {
        if (ai == null) return false

        val industries = ai.industriesWithEnemySorted.filter { targetIndustries.contains(it.industry) }

        var highestRatio = 0f
        var bestTarget: IFBStrengthRecord? = null

        for (industry in industries) {
            val ourStrength = industry.ourStr * MathUtils.getRandomNumberInRange(0.7f, 1.3f)
            val opponentStrength = industry.theirStr * MathUtils.getRandomNumberInRange(0.7f, 1.3f)

            if (opponentStrength < 200f) continue

            val ratio = industry.getEffectiveStrengthRatio(ourStrength, opponentStrength)
            if (ratio > highestRatio) {
                highestRatio = ratio
                bestTarget = industry
            }
        }
        if (bestTarget == null) {
            bestTarget = industries.randomOrNull()
        }

        if (bestTarget == null) return false
        target = bestTarget.industry

        return super.aiExecute(ai, user)
    }

    fun getTroopDamage(industry: IndustryForBattle?): Float {
        var damage = SATELLITE_NEX_ABILITY_BASE_TOTAL_DAMAGE
        var marketSize: Int = intel.market.size
        if (marketSize < 3) marketSize = 3

        if (industry != null && industry.isContested) {
            damage *= FireSupportAbilityPlugin.CLOSE_SUPPORT_DAMAGE_MULT
        }

        return damage * marketSize

    }
}

class SatelliteBarrageNexAbilityListener(
    val plugin: satelliteBarrageNexAbilityPlugin,
    val targetIndustry: IndustryForBattle,
    val intel: GroundBattleIntel,
    val effectMult: Float,
    val lifespan: Int
): GroundBattleCampaignListener, MPCGroundBattleReportWriter {
    var roundsSoFar = 0

    override fun reportBattleStarted(battle: GroundBattleIntel?) {
        return
    }

    override fun reportPlayerJoinedBattle(battle: GroundBattleIntel?) {
        return
    }

    override fun reportBattleBeforeTurn(battle: GroundBattleIntel?, turn: Int) {
        return
    }

    override fun reportBattleAfterTurn(battle: GroundBattleIntel?, turn: Int) {
        applyEffect()
        roundsSoFar++
        if (roundsSoFar >= lifespan) delete()
    }

    fun applyEffect(firstAttack: Boolean = false) {
        for (industry in intel.industries) {
            if (industry != targetIndustry) continue
            applyEffectToIndustry(industry, firstAttack)
        }
    }

    fun applyEffectToIndustry(industry: IndustryForBattle, firstAttack: Boolean) {
        for (troop in industry.units) {
            val troopSide = intel.getSide(troop.isAttacker)
            if (troopSide == plugin.side) continue
            disorganizeTroop(troop)
        }
        val damage = getTroopDamage(industry)

        val resolve = GroundBattleRoundResolve(intel)
        resolve.distributeDamage(industry, !plugin.side.isAttacker, damage)

        val canDisrupt = (!industry.plugin.def.hasTag("resistBombard")
                && !industry.plugin.def.hasTag("noBombard"))
        if (canDisrupt) {
            disruptIndustry(industry)
        }

        val log = MPCGroundBattleReport(intel)

        log.params[niko_MPC_ids.NEX_GROUND_REPORT_PLUGIN_ID] = this
        log.params["damageDealt"] = damage
        log.params["disrupted"] = canDisrupt
        log.params["firstattack"] = firstAttack

        intel.addLogEvent(log)
    }

    private fun disruptIndustry(industry: IndustryForBattle) {
        val disruptionTime = getDisruptionTime(industry)
        val realIndustry = industry.industry
        realIndustry.setDisrupted(realIndustry.disruptedDays + disruptionTime, true)
    }

    private fun getDisruptionTime(industry: IndustryForBattle): Int {
        return niko_MPC_settings.SATELLITE_NEX_ABILITY_BASE_DISRUPTION_TIME
    }

    private fun getTroopDamage(industry: IndustryForBattle): Float {
        return plugin.getTroopDamage(industry) * effectMult
    }

    private fun disorganizeTroop(troop: GroundUnit) {
        if (troop.isReorganizing) return
        troop.reorganize(1) // suppressive fire!!
    }

    override fun reportBattleEnded(battle: GroundBattleIntel?) {
        if (intel == battle) delete()
    }

    fun start(): SatelliteBarrageNexAbilityListener {
        Global.getSector().listenerManager.addListener(this)
        return this
    }

    fun delete() {
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun writeLog(tooltip: TooltipMakerAPI, battleReport: MPCGroundBattleReport) {
        val damage = (battleReport.params["damageDealt"] as Float).trimHangingZero()

        val targetString = if (plugin.side.isAttacker) "Defenders" else "Attackers"
        val firstAttack = battleReport.params["firstattack"] as? Boolean
        val attackString = if (firstAttack == true) "begins" else "continues"

        val desc = "%s satellite barrage $attackString on %s! $targetString receive %s damage, and are %s!"

        val label = tooltip.addPara(desc, LOG_PADDING, Misc.getNegativeHighlightColor(), plugin.side.faction.displayName, targetIndustry.name, "$damage", "disorganized")
        label.setHighlightColors(plugin.side.faction.color, Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())
    }
}