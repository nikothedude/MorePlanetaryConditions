package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.RepLevel
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
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.makeImportant

class MPC_IAIICChurchInitializerScript: niko_MPC_baseNikoScript(), FleetEventListener {

    var spawnedFleet: CampaignFleetAPI? = null
        set(value) {
            field?.removeEventListener(this)
            field = value
        }

    var active: Boolean = true

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
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
                    fleet.market?.primaryEntity,
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
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE] = true
        fleet.memoryWithoutUpdate.set("\$genericHail", true)
        fleet.memoryWithoutUpdate.set("\$genericHail_openComms", "MPC_IAIICChurchInitialHail")
        fleet.name = "Civilian"
        //fleet.isNoFactionInName = true

        fleet.addAssignment(
            FleetAssignment.INTERCEPT,
            playerFleet,
            Float.MAX_VALUE,
            "approaching your fleet",
            ReturnScript(fleet)
        )
        fleet.addEventListener(this)
        spawnedFleet = fleet

        market.containingLocation.addEntity(fleet)
        fleet.setLocation(market.primaryEntity.location.x, market.primaryEntity.location.y)
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