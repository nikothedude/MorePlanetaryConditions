package data.scripts.campaign.abilities

import com.fs.starfarer.api.Global
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisMissileEntityPlugin
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_missileStrikeAbility: BaseDurationAbility() {

    // high cooldown, very expensive in terms of heavy armaments, and needs a missile carrier

    enum class Missile(val nameColor: Color, val frontEndName: String) {
        EXPLOSIVE(Color(255, 90, 60), "Explosive (Low-Yield)") {
            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    Color(255, 90, 60),
                    source.containingLocation,
                    source.location,
                    100f,
                    0.4f
                )
                explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW

                params.explosionParams = explosionParams
                params.speed = 800f
                params.turnRate = 20f
                params.accelTime = 6f

                return params
            }

            override fun getArmamentCost(): Float {
                return 400f
            }

            override fun getFuelCost(): Float {
                return 500f
            }

            override fun getCooldownTimeDays(): Float {
                return 10f
            }

            override fun getSpeedString(): String = "Fast"
            override fun getSpeedHl(): Color = Misc.getHighlightColor()
            override fun getManeuverString(): String = "Bad"
            override fun getManeuverHl(): Color = Misc.getNegativeHighlightColor()
        },
        INTERDICT(Color(134, 255, 228), "Interdiction") {
            override fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams {
                val source = params.origin ?: return params
                val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                    Color(134, 255, 228),
                    source.containingLocation,
                    source.location,
                    75f,
                    0.4f
                )

                params.explosionParams = explosionParams
                params.speed = 800f
                params.turnRate = 60f
                params.accelTime = 0.5f

                return params
            }

            override fun getArmamentCost(): Float {
                return 75f
            }

            override fun getFuelCost(): Float {
                return 100f
            }

            override fun getCooldownTimeDays(): Float {
                return 2f
            }

            override fun getSpeedString(): String = "Very Fast"
            override fun getSpeedHl(): Color = Misc.getPositiveHighlightColor()
            override fun getManeuverString(): String = "Excellent"
            override fun getManeuverHl(): Color = Misc.getPositiveHighlightColor()

            fun getBaseInterdictTime(): Float = 2f
            override fun addAfterStatsSection(
                tooltip: TooltipMakerAPI,
                params: MPC_aegisMissileEntityPlugin.MissileParams
            ) {
                super.addAfterStatsSection(tooltip, params)

                tooltip.addPara(
                    "Interdicts the hit fleet for %s days on detonation.",
                    5f,
                    Misc.getHighlightColor(),
                    "${getBaseInterdictTime().toInt()}"
                )
            }
        };

        abstract fun adjustMissileParams(params: MPC_aegisMissileEntityPlugin.MissileParams): MPC_aegisMissileEntityPlugin.MissileParams
        abstract fun getArmamentCost(): Float
        abstract fun getFuelCost(): Float
        abstract fun getCooldownTimeDays(): Float
        fun addToTooltip(tooltip: TooltipMakerAPI, params: MPC_aegisMissileEntityPlugin.MissileParams) {
            val params = adjustMissileParams(params)
            val damagePair = MPC_aegisMissileEntityPlugin.getDamageStringAndColor(params.explosionParams.damage)

            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            tooltip.addPara(
                "Damage: %s",
                5f,
                damagePair.second,
                damagePair.first
            )
            tooltip.addPara(
                "Cooldown: %s",
                5f,
                Misc.getHighlightColor(),
                "${getCooldownTimeDays().trimHangingZero()} days"
            )
            tooltip.addPara(
                "Speed: %s",
                5f,
                getSpeedHl(),
                getSpeedString()
            )
            tooltip.addPara(
                "Maneuverability: %s",
                5f,
                getManeuverHl(),
                getManeuverString()
            )
            val fleet = Global.getSector().playerFleet

            val armamentAmount = getArmamentCost()
            val fuelAmount = getFuelCost()

            val cargo = fleet.cargo

            val notEnoughArmaments = cargo.getCommodityQuantity(Commodities.HAND_WEAPONS) < armamentAmount
            val notEnoughFuel = cargo.getCommodityQuantity(Commodities.FUEL) < fuelAmount

            val armamentColor = if (notEnoughArmaments) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
            val fuelColor = if (notEnoughFuel) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()

            tooltip.addPara(
                "Costs %s and %s to fire",
                5f,
                Misc.getHighlightColor(),
                "${armamentAmount.toInt()} heavy armaments", "${fuelAmount.toInt()} fuel"
            ).setHighlightColors(
                armamentColor,
                fuelColor
            )
            tooltip.setBulletedListMode(null)

            addAfterStatsSection(tooltip, params)
        }

        abstract fun getSpeedString(): String
        abstract fun getManeuverString(): String
        abstract fun getSpeedHl(): Color
        abstract fun getManeuverHl(): Color

        open fun addAfterStatsSection(
            tooltip: TooltipMakerAPI,
            params: MPC_aegisMissileEntityPlugin.MissileParams
        ) {
            return
        }
    }

    companion object {
        fun getMissileCarriers(fleet: CampaignFleetAPI): MutableSet<FleetMemberAPI> {
            val carriers = HashSet<FleetMemberAPI>()
            for (member in fleet.fleetData.membersListCopy) {
                if (member.variant.hasHullMod("MPC_missileCarrier") && !member.variant.hasTag("MPC_missileCarrierDisarmed")) {
                    carriers += member
                }
            }
            return carriers
        }

        fun getStandardAbilityParams(fleet: CampaignFleetAPI, currTarget: SectorEntityToken, id: String): MPC_aegisMissileEntityPlugin.MissileParams {
            val explosionParams = ExplosionEntityPlugin.ExplosionParams(
                Color(255, 90, 60),
                fleet.containingLocation,
                fleet.location,
                0f,
                0f
            )
            explosionParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.NONE
            val params = MPC_aegisMissileEntityPlugin.MissileParams(
                currTarget!!, // guaranteed here
                "${id}_${Misc.genUID()}",
                fleet,
                explosionParams,
                faction = fleet.faction.id,
                useTargetFacing = false,
            )
            return params
        }

        fun getPlayerAbility(): MPC_missileStrikeAbility? = Global.getSector().playerFleet?.getAbility("MPC_missileStrike") as? MPC_missileStrikeAbility


        fun playerNextMissile() {
            val ability = getPlayerAbility() ?: return
            ability.nextMissile()
        }
        fun playerPrevMissile() {
            val ability = getPlayerAbility() ?: return
            ability.prevMissile()
        }

        const val MAX_RANGE = 7500f
    }

    var currMissile: Missile = Missile.EXPLOSIVE
    @Transient
    var currTarget: CampaignFleetAPI? = null

    private fun nextMissile() {
        val currOrdinal = currMissile.ordinal
        var nextOrdinal = currOrdinal + 1
        if (nextOrdinal > Missile.entries.size) nextOrdinal = 1

        val target = Missile.entries.find { it.ordinal == nextOrdinal } ?: throw RuntimeException("somehow we didnt get the right missile.")
        currMissile = target
    }

    private fun prevMissile() {
        val currOrdinal = currMissile.ordinal
        var nextOrdinal = currOrdinal - 1
        if (nextOrdinal <= 0) nextOrdinal = Missile.entries.size

        val target = Missile.entries.find { it.ordinal == nextOrdinal } ?: throw RuntimeException("somehow we didnt get the right missile.")
        currMissile = target
    }

    override fun pressButton() {
        if (isUsable && !turnedOn) {
            if (fleet.isPlayerFleet) {

                if (MPC_missileStrikeInputListener.get(false)?.active != true) {

                    val soundId = onSoundUI
                    if (soundId != null) {
                        if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                            Global.getSoundPlayer()
                                .playSound(soundId, 1f, 1f, Global.getSoundPlayer().listenerPos, Vector2f())
                        } else {
                            Global.getSoundPlayer().playUISound(soundId, 1f, 1f)
                        }
                    }

                    class DelayedScript : MPC_delayedExecutionNonLambda(
                        IntervalUtil(0f, 0f),
                        useDays = false,
                        runIfPaused = true
                    ) {
                        override fun executeImpl() {
                            MPC_missileStrikeInputListener.get(true)!!.activate(this@MPC_missileStrikeAbility)
                        }
                    }

                    DelayedScript().start()
                }

            }
        }
    }

    fun forceActivation() {
        activateImpl()
        setCooldownLeft(currMissile.getCooldownTimeDays())
        subtractCommodities()
    }

    private fun subtractCommodities() {
        val armaments = getArmamentCost()
        val fuel = getFuelCost()

        val cargo = fleet.cargo
        cargo.removeCommodity(Commodities.HAND_WEAPONS, armaments)
        cargo.removeFuel(fuel)
    }

    override fun activateImpl() {
        // do the actual missile launching here
        // target is guaranteed here
        val baseParams = getBaseParams()
        val params = currMissile.adjustMissileParams(baseParams)
        MPC_aegisMissileEntityPlugin.createNewFromEntity(fleet, params)
    }

    fun getBaseParams(): MPC_aegisMissileEntityPlugin.MissileParams {
        return getStandardAbilityParams(fleet, currTarget!!, id)
    }

    override fun applyEffect(amount: Float, level: Float) {
        return
    }

    override fun deactivateImpl() {

    }

    override fun cleanupImpl() {

    }

    override fun isUsable(): Boolean {
        val superCall = super.isUsable
        if (!superCall) return false

        if (MPC_missileStrikeInputListener.get(true)!!.active) return false

        if (!canFire()) return false

        return true
    }

    fun canFire(): Boolean {
        val carriers = getMissileCarriers(fleet)
        if (carriers.isEmpty() || carriers.all { it.repairTracker.cr <= 0.1f }) return false

        if (fleet.isPlayerFleet) {
            val cargo = fleet.cargo

            if (cargo.getCommodityQuantity(Commodities.HAND_WEAPONS) < getArmamentCost()) return false
            if (cargo.fuel < getFuelCost()) return false
        }

        return true
    }

    fun getArmamentCost(): Float {
        return currMissile.getArmamentCost() // placeholder
    }

    fun getFuelCost(): Float {
        return currMissile.getFuelCost() // placeholder
    }

    fun canTargetEntity(target: SectorEntityToken): Boolean {
        if (target !is CampaignFleetAPI) return false

        if (target == fleet) return false
        if (target.containingLocation != fleet.containingLocation) return false

        val dist = MathUtils.getDistance(fleet, target)
        if (dist > MAX_RANGE) return false

        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val fleet = getFleet()
        if (fleet == null) return

        Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val bad = Misc.getNegativeHighlightColor()

        if (!Global.CODEX_TOOLTIP_MODE) {
            tooltip.addTitle(spec.name)
        } else {
            tooltip.addSpacer(-10f)
        }

        val pad = 10f

        tooltip.addPara(
            "Utilize the missile carriers within your fleet to deliver a devastating missile strike to a single fleet within %s.",
            pad,
            Misc.getHighlightColor(),
            "${MAX_RANGE.toInt()}su*"
        )

        if (!Global.CODEX_TOOLTIP_MODE) {
            if (getMissileCarriers(fleet).isEmpty()) {
                tooltip.addPara(
                    "You do not have any missile carriers in your fleet.",
                    pad
                ).color = Misc.getNegativeHighlightColor()

                return
            } else if (getMissileCarriers(fleet).all { it.repairTracker.cr <= 0.1f }) {
                tooltip.addPara(
                    "All of your fleet's missile carriers are combat-incapable.",
                    pad
                ).color = Misc.getNegativeHighlightColor()
            }
        }

        if (Global.CODEX_TOOLTIP_MODE) {
            tooltip.addPara(
                "Costs %s of %s and %s to fire, based on the loaded missile.",
                pad,
                Misc.getHighlightColor(),
                "a significant amount", "heavy armaments", "fuel"
            )
        } else {

            tooltip.addPara(
                "Fires from in the direction your fleet is facing, meaning you must %s for an accurate shot.",
                5f,
                Misc.getHighlightColor(),
                "orient your fleet towards your target"
            )

            tooltip.addPara(
                "Currently loaded missile type: %s",
                10f,
                currMissile.nameColor,
                currMissile.frontEndName
            )
            currMissile.addToTooltip(tooltip, getBaseParams())

            val gray = Misc.getGrayColor()
            tooltip.addPara("*2000 units = 1 map grid cell", gray, pad)

        }

        addIncompatibleToTooltip(tooltip, expanded)
    }
}