package data.hullmods

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.entities.Ship
import data.utilities.niko_MPC_stringUtils

class MPC_missileCarrier: BaseHullMod() {

    companion object {
        fun getNumMissiles(carrier: FleetMemberAPI): Int {
            var numMissiles = 0
            val spec = carrier.hullSpec.baseHull ?: Global.getSettings().getHullSpec(carrier.hullSpec.baseHullId)
            for (tag in spec.tags) {
                if (!tag.contains("MPC_carriesMissiles")) continue
                val num = tag.filter { it.isDigit() }
                numMissiles = num.toInt()
            }

            if (carrier.hullSpec.baseHullId == "MPC_lockbow" && Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_AOTDLockbowImproved")) numMissiles++

            return numMissiles
        }

        const val DISARMED_DP_DEC = 10f
        const val DISARMED_MAINT_MULT = 0.5f
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) {
            return "cruise missile"
        }
        if (index == 1) {
            return "missile strike"
        }

        return null
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI?,
        hullSize: HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    ) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)

        if (ship == null || tooltip == null) return

        val variant = ship.variant
        if (variant.hasTag("MPC_missileCarrierDisarmed")) {
            tooltip.addPara(
                "However, this ship has been %s, rendering the ability - as well as its %s - %s.",
                10f,
                Misc.getNegativeHighlightColor(),
                "disarmed of its missile autoforge", "primary weapon", "useless"
            )

            tooltip.addPara(
                "However, this does make make the ship easier to maintain, resulting in a %s reduction of %s, and %s less supply usage.",
                10f,
                Misc.getPositiveHighlightColor(),
                "deployment point", "${DISARMED_DP_DEC.toInt()}", niko_MPC_stringUtils.toPercent(1 - DISARMED_MAINT_MULT)
            )
        } else {
            val member = ship.fleetMember
            if (member != null) {
                tooltip.addPara(
                    "This ship can carry %s missiles at a time.",
                    10f,
                    Misc.getHighlightColor(),
                    getNumMissiles(member).toString()
                )
            }
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)

        if (ship == null) return

        val variant = ship.variant
        if (variant.hasTag("MPC_missileCarrierDisarmed")) {
            if (Global.getCombatEngine().isSimulation || ship.originalOwner != -1) {
                val weapon = ship.allWeapons.find { it.spec.weaponId == "MPC_cruiseLauncher" } ?: return
                weapon.disable(true)
            }

            val stats = ship.mutableStats
            if (variant.hasTag("MPC_missileCarrierDisarmed")) {
                stats.suppliesPerMonth.modifyMult(id, DISARMED_MAINT_MULT)
                stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, -DISARMED_DP_DEC)
            }
        }
    }

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        super.advanceInCampaign(member, amount)

        val fleet = member?.fleetData?.fleet ?: return
        if (fleet.isPlayerFleet) { // todo: separate ai from the remnant fleet carrier script
            Global.getSector().characterData.addAbility("MPC_missileStrike")
        }
    }
}