package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.MPC_overgrownNanoforgeExpeditionAssignmentAI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.skills.MPC_routingOptimization
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.makeImportant

class MPC_fractalCoreReactionScript: niko_MPC_baseNikoScript() {

    companion object {
        const val DAYS_TIL_HEGE_FLEET = 30f
        const val BASE_INSPECTOR_FP = 150f

        fun trySpawnHegeFleet(colony: MarketAPI) {
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_hegePrivateInspectorsSpawned")) return
            val hege = Global.getSector().getFaction(Factions.HEGEMONY)
            if (hege.relToPlayer.isHostile) return

            val source = hege.getMarketsCopy().randomOrNull() ?: return

            val params = FleetParamsV3(
                source,
                FleetTypes.PATROL_SMALL,
                BASE_INSPECTOR_FP,
                5f,
                10f,
                10f,
                0f,
                10f,
                0f
            )
            params.officerLevelLimit = 7
            params.officerLevelBonus = 1
            val fleet = FleetFactoryV3.createFleet(params)

            source.containingLocation.addEntity(fleet)
            fleet.containingLocation = source.containingLocation
            val primaryEntityLoc = source.primaryEntity.location
            fleet.setLocation(primaryEntityLoc.x, primaryEntityLoc.y)
            val facingToUse = MathUtils.getRandomNumberInRange(0f, 360f)
            fleet.facing = facingToUse

            fleet.name = "Private Investigators"

            fleet.makeImportant("\$MPC_hegePrivateInspectors")

            fleet.memoryWithoutUpdate["\$MPC_hegePrivateInspectors"] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_LOW_REP_IMPACT] = true

            MPC_privateInvestigatorAssignmentAI(fleet, colony, source).start()

            Global.getSector().memoryWithoutUpdate["\$MPC_hegePrivateInspectorsSpawned"] = true
        }

        fun getFractalColony(): MarketAPI? = MPC_hegemonyFractalCoreCause.getFractalColony()
    }

    val checkInterval = IntervalUtil(3f, 3.2f) // days

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {
            checkReaction()

            checkRouting()
        }
    }

    // this shit doesnt work in the skill lmao. we do it manually
    private fun checkRouting() {
        val market = getFractalColony()

        if (market != null) {
            for (otherMarket in market.faction.getMarketsCopy().filter { it != market && it.admin?.isAICore == true }) {
                if (otherMarket.size > market.size) continue
                otherMarket.accessibilityMod.modifyFlat("MPC_routingOptAccess", MPC_routingOptimization.AI_CORE_ADMIN_ACCESSIBILITY_BONUS, "${market.name} administrator")
            }
        } else {
            for (otherMarket in Global.getSector().economy.marketsCopy) {
                otherMarket.accessibilityMod.unmodify("MPC_routingOptAccess") // this is a nuclear option, a bit slow but really it shouldnt be that bad
            }
        }
    }

    private fun checkReaction() {
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_hegePrivateInspectorsSpawned")) {
            //delete()
            return
        }

        val colony = getFractalColony() ?: return
        val AICoreCond = AICoreAdmin.get(colony) ?: return
        if (AICoreCond.daysActive >= DAYS_TIL_HEGE_FLEET) {
            trySpawnHegeFleet(colony)
        }
    }
}