package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import lunalib.lunaExtensions.getMarketsCopy
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.makeImportant

class MPC_IAIICChurchInitializerScript: niko_MPC_baseNikoScript(), FleetEventListener {

    companion object {
        const val KEY = "\$MPC_IAIICChurchInit"

        fun get(): MPC_IAIICChurchInitializerScript? = Global.getSector().memoryWithoutUpdate[KEY] as MPC_IAIICChurchInitializerScript
    }

    var spawnedFleet: CampaignFleetAPI? = null
        set(value) {
            field?.removeEventListener(this)
            field = value
        }

    var active: Boolean = true

    override fun startImpl() {
        Global.getSector().addScript(this)
        Global.getSector().memoryWithoutUpdate[KEY] = this
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
        spawnedFleet?.removeEventListener(this)

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (MPC_IAIICFobIntel.get() == null) {
            delete()
            return
        }
        if (!active) return

        val market = canSpawn() ?: return
        spawn(market)
    }

    fun canSpawn(): MarketAPI? {
        val playerFleet = Global.getSector().playerFleet
        if (playerFleet.isInHyperspaceTransition) return null

        if (playerFleet.isInHyperspace) {
            val nearbySystems = Misc.getNearbyStarSystems(playerFleet, 10f).filter { it.hasTag(Tags.THEME_CORE) }
            for (sys in nearbySystems) {
                val potentialMarket = locIsGood(sys)
                if (potentialMarket != null) return potentialMarket
            }
        } else {
            return locIsGood(playerFleet.containingLocation)
        }
        return null
    }

    fun locIsGood(location: LocationAPI): MarketAPI? {
        if (!location.hasTag(Tags.THEME_CORE)) return null
        for (market in location.getMarketsInLocation()) {
            if (market.faction.getRelationshipLevel(Factions.LUDDIC_CHURCH) <= RepLevel.FRIENDLY) {
                return market
            }
        }
        return null
    }

    fun spawn(market: MarketAPI) {
        val playerFleet = Global.getSector().playerFleet ?: return

        active = false

        class ReturnScript(val fleet: CampaignFleetAPI): Script {
            override fun run() {
                fleet.clearAssignments()
                fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getMarketsCopy().randomOrNull()?.primaryEntity ?: fleet.market?.primaryEntity,
                    Float.MAX_VALUE
                )
            }

        }

        val params = FleetParamsV3(
            market,
            market.locationInHyperspace,
            Factions.LUDDIC_CHURCH,
            null,
            FleetTypes.PATROL_SMALL,
            6f,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f
        )
        params.commander = MPC_People.getImportantPeople()[MPC_People.CHURCH_ALOOF_MILITANT]
        val fleet = FleetFactoryV3.createFleet(params)
        fleet.makeImportant(niko_MPC_ids.IAIIC_QUEST)
        Misc.makeNonHostileToFaction(fleet, Factions.PLAYER, 30f)
        //fleet.memoryWithoutUpdate[FleetAIFlags.PLACE_TO_LOOK_FOR_TARGET] = Vector2f(playerFleet.location)
        /*if (playerFleet.isInHyperspace) {
            fleet.memoryWithoutUpdate[FleetAIFlags.SEEN_TARGET_JUMPING_FROM] = market.containingLocation.id
        }*/
        fleet.memoryWithoutUpdate.set("\$genericHail", true)
        fleet.memoryWithoutUpdate.set("\$genericHail_openComms", "MPC_IAIICChurchInitialHail")
        fleet.name = "Civilian"
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE] = true
        fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS] = true
        fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS] = true
        //fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PURSUE_PLAYER] = true
        //fleet.isNoFactionInName = true
        fleet.sensorRangeMod.modifyMult("MPC_INFINITERANGE", 100f)

        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION,
            playerFleet,
            30f,
            "approaching your fleet",
        )
        fleet.addAssignment(
            FleetAssignment.INTERCEPT,
            playerFleet,
            30f,
            "approaching your fleet",
            //ReturnScript(fleet)
        )
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getMarketsCopy().randomOrNull()?.primaryEntity ?: fleet.market?.primaryEntity,
            Float.MAX_VALUE
        )
        fleet.addEventListener(this)
        fleet.stats.fleetwideMaxBurnMod.modifyFlat("MPC_FAST", 10f)
        spawnedFleet = fleet

        playerFleet.containingLocation.addEntity(fleet)
        val range = playerFleet.sensorStrength + (fleet.sensorProfile * 1.5f)
        val point = Misc.getPointAtRadius(playerFleet.location, range).scale(1.1f) as Vector2f
        fleet.setLocation(point.x, point.y)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (fleet != spawnedFleet) return
        spawnedFleet = null
        if (reason == null
            || reason == CampaignEventListener.FleetDespawnReason.REACHED_DESTINATION
            || reason == CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY) {

            active = true
            return
        } else {
            delete()
            return
        }

    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        return
    }
}