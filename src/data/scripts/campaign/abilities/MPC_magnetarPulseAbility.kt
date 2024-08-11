package data.scripts.campaign.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Pings
import com.fs.starfarer.api.loading.CampaignPingSpec
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse.Companion.BASE_SHOCKWAVE_DURATION
import data.utilities.niko_MPC_ids
import java.awt.Color

class MPC_magnetarPulseAbility: BaseDurationAbility() {

    companion object {
        const val DETECTABILITY_PERCENT = 175f

        const val MAX_EFFECT = 1f

        //public static final float RANGE = 1000f;
        const val BASE_RADIUS = 200f
        const val BASE_DURATION = BASE_SHOCKWAVE_DURATION * 0.056f
        const val BASE_PULSE_SPEED = niko_MPC_magnetarPulse.BASE_SHOCKWAVE_SPEED * 0.85f
        const val BASE_PULSE_ACCEL = 20f
        const val BASE_SECONDS = 6f
        const val STRENGTH_PER_SECOND = 200f

        const val MAX_BURN_ANCHOR = 10
        /** Controls how much having a high or low burn impacts the power of the pulse. */
        const val BASE_SIZE_MULT = 0.9f

        const val BASE_REP_LOSS = 0.04f // if a fleet is hit by the shockwave

        const val FUEL_USE_MULT = 5f

        val BASE_COLOR = Color(202, 27, 233, 190)

//	public String getSpriteName() {
//		return Global.getSettings().getSpriteName("abilities", Abilities.EMERGENCY_BURN);
//	}


        //	public String getSpriteName() {
        //		return Global.getSettings().getSpriteName("abilities", Abilities.EMERGENCY_BURN);
        //	}

        fun getSizeMult(fleet: CampaignFleetAPI): Float {
            return ((fleet.currBurnLevel.coerceAtMost(40f) / MAX_BURN_ANCHOR) * BASE_SIZE_MULT).coerceAtLeast(0f)
        }

        fun getRange(fleet: CampaignFleetAPI): Float {
            return BASE_RADIUS * getSizeMult(fleet)
        }
    }

    protected var primed: Boolean = false
    protected var elapsed: Float = 0f
    protected var numFired: Int = 0

    override fun activateImpl() {
        Global.getSector().addPing(fleet, Pings.INTERDICT)

        primed = true
        val cost = computeFuelCost()
        fleet.cargo.removeFuel(cost)
    }

    override fun applyEffect(amount: Float, level: Float) {
        val fleet = fleet ?: return
        fleet.stats.detectedRangeMod.modifyPercent(
            modId,
            DETECTABILITY_PERCENT * level,
            "Ionized Pulse"
        )

        //System.out.println("Level: " + level);
        if (level > 0 && level < 1 && amount > 0) {
            showRangePing(amount)
            //			float activateSeconds = getActivationDays() * Global.getSector().getClock().getSecondsPerDay();
//			float speed = fleet.getVelocity().length();
//			float acc = Math.max(speed, 200f)/activateSeconds + fleet.getAcceleration();
//			float ds = acc * amount;
//			if (ds > speed) ds = speed;
//			Vector2f dv = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(fleet.getVelocity()));
//			dv.scale(ds);
//			fleet.setVelocity(fleet.getVelocity().x - dv.x, fleet.getVelocity().y - dv.y);
            //fleet.goSlowOneFrame()
            return
        }
        val range = InterdictionPulseAbility.getRange(fleet)
        val playedHit = !(entity.isInCurrentLocation && entity.isVisibleToPlayerFleet)
        if (level == 1f && primed) {
            /*if (entity.isInCurrentLocation) {
                Global.getSector().memoryWithoutUpdate[MemFlags.GLOBAL_INTERDICTION_PULSE_JUST_USED_IN_CURRENT_LOCATION, true] =
                    0.1f
            }*/
            //fleet.memoryWithoutUpdate[MemFlags.JUST_DID_INTERDICTION_PULSE, true] = 0.1f

            val custom = CampaignPingSpec()
            custom.isUseFactionColor = true
            custom.width = 15f
            custom.range = range * 1.3f
            custom.duration = 0.5f
            custom.alphaMult = 1f
            custom.inFraction = 0.1f
            custom.num = 1
            Global.getSector().addPing(fleet, custom)

            /*for (other in fleet.containingLocation.fleets) {
                if (other === fleet) continue
                if (other.faction === fleet.faction) continue
                if (other.isInHyperspaceTransition) continue
                val dist = Misc.getDistance(fleet.location, other.location)
                if (dist > range) continue
                var interdictSeconds = InterdictionPulseAbility.getInterdictSeconds(fleet, other)
                if (interdictSeconds > 0 && interdictSeconds < 1f) interdictSeconds = 1f
                val vis = other.visibilityLevelToPlayerFleet
                if (vis == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS || vis == VisibilityLevel.COMPOSITION_DETAILS || vis == VisibilityLevel.SENSOR_CONTACT && fleet.isPlayerFleet) {
                    if (interdictSeconds <= 0) {
                        other.addFloatingText("Interdict avoided!", fleet.faction.baseUIColor, 1f, true)
                        continue
                    } else {
                        other.addFloatingText(
                            "Interdict! (" + Math.round(interdictSeconds) + "s)",
                            fleet.faction.baseUIColor,
                            1f,
                            true
                        )
                    }
                }
                val interdictDays = interdictSeconds / Global.getSector().clock.secondsPerDay
                for (ability in other.abilities.values) {
                    if (!ability.spec.hasTag(Abilities.TAG_BURN + "+") &&
                        !ability.spec.hasTag(Abilities.TAG_DISABLED_BY_INTERDICT) &&
                        ability.id != Abilities.INTERDICTION_PULSE
                    ) continue
                    val origCooldown = ability.cooldownLeft
                    var extra = 0f
                    if (ability.isActiveOrInProgress) {
                        extra += ability.spec.deactivationCooldown * ability.progressFraction
                        ability.deactivate()
                    }
                    if (!ability.spec.hasTag(Abilities.TAG_BURN + "+")) continue
                    var cooldown = interdictDays
                    //cooldown = Math.max(cooldown, origCooldown);
                    cooldown += origCooldown
                    cooldown += extra
                    val max = Math.max(ability.spec.deactivationCooldown, 2f)
                    if (cooldown > max) cooldown = max
                    ability.cooldownLeft = cooldown
                }
                if (fleet.isPlayerFleet && other.knowsWhoPlayerIs() && fleet.faction !== other.faction) {
                    Global.getSector().adjustPlayerReputation(
                        RepActionEnvelope(RepActions.INTERDICTED, null, null, false),
                        other.faction.id
                    )
                }
                if (!playedHit) {
                    Global.getSoundPlayer().playSound("world_interdict_hit", 1f, 1f, other.location, other.velocity)
                    //playedHit = true;
                }
            }*/

            val duration = BASE_DURATION * getSizeMult(fleet)
            val radius = BASE_RADIUS * getSizeMult(fleet)
            val speed = BASE_PULSE_SPEED * getSizeMult(fleet)
            //val accel = BASE_PULSE_ACCEL * getSizeMult(fleet)
            val params = niko_MPC_magnetarPulse.MPC_magnetarPulseParams(fleet.containingLocation, fleet.location, radius, 2f, color = BASE_COLOR, shockwaveDuration = duration, shockwaveSpeed = speed)
            params.sourceFleet = fleet
            params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
            params.baseRepLoss = BASE_REP_LOSS
            params.makeParticlesMaxVelocityImmediately = true
            params.respectIgnore = false

            params.explosionDamageMult = 2f

            val explosion = fleet.containingLocation.addCustomEntity(
                Misc.genUID(), "Ionized Pulse",
                "MPC_magnetarPulse", Factions.NEUTRAL, params
            )
            explosion.setLocation(fleet.location.x, fleet.location.y)

            primed = false
            elapsed = 0f
            numFired = 0
        }
    }

    protected fun showRangePing(amount: Float) {
        val fleet = fleet ?: return
        val vis = fleet.visibilityLevelToPlayerFleet
        if (vis == VisibilityLevel.NONE || vis == VisibilityLevel.SENSOR_CONTACT) return
        var fire = false
        if (elapsed <= 0) {
            fire = true
        }
        elapsed += amount
        if (elapsed > 0.5f && numFired < 4) {
            elapsed -= 0.5f
            fire = true
        }
        if (fire) {
            numFired++
            val range = getRange(fleet)
            val custom = CampaignPingSpec()
            custom.isUseFactionColor = true
            custom.width = 7f
            custom.minRange = range - 100f
            custom.range = 200f
            custom.duration = 2f
            custom.alphaMult = 0.25f
            custom.inFraction = 0.2f
            custom.num = 1
            Global.getSector().addPing(fleet, custom)
        }
    }

    override fun deactivateImpl() {
        cleanupImpl()
    }

    override fun cleanupImpl() {
        val fleet = fleet ?: return

        fleet.stats.detectedRangeMod.unmodify(modId)
        //fleet.getStats().getSensorRangeMod().unmodify(getModId());
        //fleet.getStats().getFleetwideMaxBurnMod().unmodify(getModId());
        //fleet.getStats().getAccelerationMult().unmodify(getModId());
        //fleet.getCommanderStats().getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT).unmodify(getModId());

        //fleet.getStats().getSensorRangeMod().unmodify(getModId());
        //fleet.getStats().getFleetwideMaxBurnMod().unmodify(getModId());
        //fleet.getStats().getAccelerationMult().unmodify(getModId());
        //fleet.getCommanderStats().getDynamic().getStat(Stats.NAVIGATION_PENALTY_MULT).unmodify(getModId());
        primed = false
    }

    override fun isUsable(): Boolean {
        return super.isUsable() &&
                fleet != null &&
                fleet.memoryWithoutUpdate[niko_MPC_ids.DRIVE_BUBBLE_DESTROYED] != true &&
                getSizeMult(fleet) > 0f &&
                fleet.cargo.fuel >= computeFuelCost()
        //getNonReadyShips().isEmpty();
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    //	protected List<FleetMemberAPI> getNonReadyShips() {
    //		List<FleetMemberAPI> result = new ArrayList<FleetMemberAPI>();
    //		CampaignFleetAPI fleet = getFleet();
    //		if (fleet == null) return result;
    //
    //		float crCostFleetMult = fleet.getStats().getDynamic().getValue(Stats.EMERGENCY_BURN_CR_MULT);
    //		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
    //			//if (member.isMothballed()) continue;
    //			float crLoss = member.getDeployCost() * CR_COST_MULT * crCostFleetMult;
    //			if (Math.round(member.getRepairTracker().getCR() * 100) < Math.round(crLoss * 100)) {
    //				result.add(member);
    //			}
    //		}
    //		return result;
    //	}
    //	protected float computeSupplyCost() {
    //		CampaignFleetAPI fleet = getFleet();
    //		if (fleet == null) return 0f;
    //
    //		float crCostFleetMult = fleet.getStats().getDynamic().getValue(Stats.EMERGENCY_BURN_CR_MULT);
    //
    //		float cost = 0f;
    //		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
    //			cost += member.getDeploymentPointsCost() * CR_COST_MULT * crCostFleetMult;
    //		}
    //		return cost;
    //	}
    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val fleet = fleet ?: return
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val fuel = Global.getSettings().getColor("progressBarFuelColor")
        val bad = Misc.getNegativeHighlightColor()
        val title = tooltip.addTitle("Ionized Pulse")
        val pad = 10f

        val sizeMult = getSizeMult(fleet)

        tooltip.addPara(
            "Injects an impressive amount of power into the drive field, allowing your fleet to fire off a highly ionized pulse " +
            "that is capable of %s* (excluding your fleet's own) and modestly damaging ships.",
            pad,
            highlight,
            "destroying nearby drive bubbles"
        )
        tooltip.addPara(
            "Base range of %s* units, increased for every burn level above %s, " +
                    "for a total of %s units. While the pulse is charging, the range at which the fleet can be detected will " +
                    "gradually increase by up to %s.", pad, highlight,
            "$BASE_RADIUS", "$MAX_BURN_ANCHOR", "${BASE_RADIUS * sizeMult}", "${DETECTABILITY_PERCENT.toInt()}%"
        )
        if (sizeMult <= 0) {
            tooltip.addPara(
                "Going too slow to activate.",
                bad,
                pad
            )
        }
        tooltip.addPara(
            "A successful drive field destruction is considered a highly hostile act, though not on the same level as " +
                    "open warfare.", pad
        )

        val fuelCost = computeFuelCost()

        tooltip.addPara(
            "Consumes %s fuel.", pad,
            highlight,
            Misc.getRoundedValueMaxOneAfterDecimal(fuelCost)
        )
        if (fuelCost > fleet.cargo.fuel) {
            tooltip.addPara("Not enough fuel.", bad, pad)
        }

        tooltip.addPara("*2000 units = 1 map grid cell", gray, pad)
        tooltip.addPara(
            "*Without a drive field, a fleet is unable to use their travel drives, nor any movement-based abilities",
            pad
        )
        /*tooltip.addPara(
            "*A fleet is considered slow-moving at a burn level of half that of its slowest ship.",
            gray,
            pad
        )*/
        addIncompatibleToTooltip(tooltip, expanded)
    }

    fun computeFuelCost(): Float {
        val fleet = fleet ?: return 0f
        return fleet.logistics.fuelCostPerLightYear * FUEL_USE_MULT
    }


    override fun fleetLeftBattle(battle: BattleAPI?, engagedInHostilities: Boolean) {
        if (engagedInHostilities) {
            deactivate()
        }
    }

    override fun fleetOpenedMarket(market: MarketAPI?) {
        deactivate()
    }
}